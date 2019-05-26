node {
// -------------------------------------------------------------------------------------------------------
// Parameters

  // docker
  def DOCKER_URL = '192.168.230.155:8081'

  // sonarqube
  def SONAR_HOST_URL = 'http://192.168.230.155:9000'
  def SONAR_SERVER = 'sonar'
  def SONAR_SCANNER_TOOL = 'sonarscanner'
  def SONAR_PROJECT_KEY = "${JOB_NAME}"
  def SONAR_SOURCES = 'maven-example/multi3/src'
 
  // artifactory
  def ART_URL = 'http://192.168.230.155:8081/artifactory/'
  def CREDENTIALSID = 'art1'
  def PASSWORDVARIABLE = 'PASSWORD'
  def USERNAMEVARIABLE = 'USERNAME'
  def SOURCEREPO = 'docker-dev-local'
  def TARGETREPO = 'docker-pro-local'
  def RESOLVE_SNAPSHOT_REPO = 'maven-virtual'
  def RESOLVE_RELEASE_REPO = 'maven-virtual'
  def DEPLOY_SNAPSHOT_REPO = 'maven-dev-local'
  def DEPLOY_RELEASE_REPO = 'maven-pro-local'
  def artServer = Artifactory.server('art1')
  def rtMaven = Artifactory.newMavenBuild()
  def buildInfo = Artifactory.newBuildInfo()

  // git
  def GIT_URL = 'https://github.com/gyzong1/pipeline-example.git'

  // maven
  def MAVEN_TOOL = 'maven'
  def MAVEN_GOALS = 'clean install'
  def POM_PATH = 'maven-example/pom.xml'

  // -------------------------------------------------------------------------------------------------------

  stage ('Checkout Code') {
    git GIT_URL
  }
 
dir('project-examples') {
  stage ('Build Maven') { 
    rtMaven.resolver server: artServer, releaseRepo: RESOLVE_RELEASE_REPO, snapshotRepo: RESOLVE_SNAPSHOT_REPO
    rtMaven.deployer server: artServer, releaseRepo: DEPLOY_RELEASE_REPO, snapshotRepo: DEPLOY_SNAPSHOT_REPO
    rtMaven.tool = MAVEN_TOOL
    rtMaven.run pom: POM_PATH, goals: MAVEN_GOALS, buildInfo: buildInfo
  }
}

dir('Docker-lifecycle-scripts') {

  stage('Resolve') {
    dir('docker-framework') {
      withCredentials([usernamePassword(credentialsId: CREDENTIALSID, passwordVariable: PASSWORDVARIABLE, usernameVariable: USERNAMEVARIABLE)]) {
        def uname=env.USERNAME
        def pw=env.PASSWORD
        artServer.username=uname
        artServer.password=pw
        def curlstr="curl -u"+uname+':'+pw+" "+"\'"+ART_URL
        def artDocker= Artifactory.docker server: artServer
        def tomcatverstr=curlstr+ "api/search/latestVersion?g=org.apache&a=apache-tomcat&repos=tomcat-local'"
        println(tomcatverstr)
        sh tomcatverstr+' > tomcat/version.txt'
        env.TOMCAT_VERSION=readFile('tomcat/version.txt')
        sh 'echo $TOMCAT_VERSION'
        
        def downloadSpec = """{
         "files": [
          {
           "pattern": "tomcat-local/java/jdk-8u201-linux-x64.tar.gz",
           "target": "jdk/jdk-8-linux-x64.tar.gz",
           "flat":"true"
          },
          {
           "pattern": "tomcat-local/org/apache/apache-tomcat/apache-tomcat-"""+env.TOMCAT_VERSION+""".tar.gz",
           "target": "tomcat/apache-tomcat-8.tar.gz",
           "flat":"true"
          }
          ]
        }"""

        artServer.download(downloadSpec, buildInfo)
        sh 'pwd'
        sh 'ls -al jdk'
        sh 'ls -al tomcat'
        sh 'echo download complete'
      }
    }
  }

  stage('docker build') {
    dir('docker-framework') {
      withCredentials([usernamePassword(credentialsId: CREDENTIALSID, passwordVariable: PASSWORDVARIABLE, usernameVariable: USERNAMEVARIABLE)]) {
        def uname=env.USERNAME
        def pw=env.PASSWORD
        artServer.username=uname
        artServer.password=pw
        def curlstr="curl -u"+uname+':'+pw+" "+"\'"+ART_URL
        def artDocker= Artifactory.docker server: artServer

        buildInfo.env.collect()
        println('starting build '+env.BUILD_NUMBER)
        def tagName=DOCKER_URL+'/docker-virtual/docker-framework:'+env.BUILD_NUMBER
        sh 'pwd'
        sh 'ls -al'
        sh 'cat Dockerfile'
        buildInfo.env.capture = true
        docker.build(tagName)
        buildInfo.env.vars['status2'] = 'pre-test'
        artDocker.push(tagName, 'docker-virtual', buildInfo)

        //artDocker.deployer.addProperty("status", "in-qa").addProperty("compatibility", "1", "2", "3")
        artServer.publishBuildInfo(buildInfo)
        println('Retagging Image')
        sh 'sed -E "s/@/$BUILD_NUMBER/" retag-dev > retag_out_dev.json'
        sh 'cat retag_out_dev.json'
        def retagstr=curlstr+"api/docker/docker-dev-local/v2/promote' -X POST -H 'Content-Type: application/json' -T retag_out_dev.json"
        sh retagstr
      }
    }
  }

  stage('testing') {
    dir('docker-framework/framework-test') {
      withCredentials([usernamePassword(credentialsId: CREDENTIALSID, passwordVariable: PASSWORDVARIABLE, usernameVariable: USERNAMEVARIABLE)]) {
        def uname=env.USERNAME
        def pw=env.PASSWORD
        artServer.username=uname
        artServer.password=pw
        def curlstr="curl -u"+uname+':'+pw+" "+"\'"+ART_URL
        def artDocker= Artifactory.docker server: artServer

        println('Get the latest version of the tomcat war from maven-snapshots-local repo.  We only want war files that have been released')
        def warverstr=curlstr+ "api/search/latestVersion?g=org.jfrog.test&a=multi3&repos=maven-snapshots-local&v=3.7-SNAPSHOT'"
        sh warverstr +' > war/version.txt'
        env.WARVER=readFile('war/version.txt')
        def downloadSpecWar = """{
          "files": [
           {
            "pattern": "maven-snapshots-local/org/jfrog/test/multi3/3.7-SNAPSHOT/multi3-"""+env.WARVER+""".war",
            "target": "war/webservice.war",
            "flat": "true"
           }
           ]
          }""" //"//DownloadSpec
        println(downloadSpecWar)
        artServer.download(downloadSpecWar)
        sh "sed -i 's#docker_ip#${DOCKER_URL}/docker-virtual#' Dockerfile"
        echo "===================================="
        sh "cat Dockerfile"
        def tagNameTest=DOCKER_URL+'/docker-virtual/docker-framework-test:'+env.BUILD_NUMBER
        docker.build(tagNameTest)
        docker.image(tagNameTest).withRun('-p 8181:8181') {c ->
          sleep 5
          sh 'curl "http://localhost:8181/swampup/"'
        }
      }
    }
  }

  
  stage('promote') {
    dir('docker-framework/framework-test') {
      withCredentials([usernamePassword(credentialsId: CREDENTIALSID, passwordVariable: PASSWORDVARIABLE, usernameVariable: USERNAMEVARIABLE)]) {
        def uname=env.USERNAME
        def pw=env.PASSWORD
        artServer.username=uname
        artServer.password=pw
        def curlstr="curl -u"+uname+':'+pw+" "+"\'"+ART_URL

        def artDocker= Artifactory.docker server: artServer   
        def promotionConfig = [
          'buildName'          : env.JOB_NAME,
          'buildNumber'        : env.BUILD_NUMBER,
          'targetRepo'         : TARGETREPO,
          'comment'            : 'Framework works with latest version of application to pass test',
          'sourceRepo'         : SOURCEREPO,
          'status'             : 'Released',
          'includeDependencies': false,
          'failFast'           : false,
          'copy'               : true
        ]
        // Promote build
        artServer.promote promotionConfig

         dir('..') {
          sh 'ls -l'
            sh 'sed -E "s/@/$BUILD_NUMBER/" retag-release.json > retag_out_release.json'
            sh 'cat retag_out_release.json'
            def retagstr=curlstr+"api/docker/docker-pro-local/v2/promote' -X POST -H 'Content-Type: application/json' -T retag_out_release.json"
            sh retagstr
         }
      }
    }
  }
}

}
