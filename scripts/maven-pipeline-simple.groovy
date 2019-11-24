node {

  def artServer = Artifactory.server 'art1'
  def rtMaven = Artifactory.newMavenBuild()
  def buildInfo = Artifactory.newBuildInfo()
 /* 
  stage 'Clone'
        git url: 'https://github.com/gyzong1/spring-boot-samples.git', branch: 'master'

  stage 'Build Maven'
    dir ('./spring-boot-basewebapp/'){
      
      rtMaven.resolver server: artServer, releaseRepo: 'maven-virtual', snapshotRepo: 'maven-virtual'
      rtMaven.deployer server: artServer, releaseRepo: 'maven-dev-local', snapshotRepo: 'maven-dev-local'
      rtMaven.tool = 'maven'
      rtMaven.deployer.deployArtifacts = true
     // rtMaven.deployer.artifactDeploymentPatterns.addInclude("frog*")
      rtMaven.run pom: 'pom.xml', goals: 'clean package', buildInfo: buildInfo   

      artServer.publishBuildInfo buildInfo
    }
   */
  
  stage ('Clone') {
        git url: 'https://github.com/JFrog/project-examples.git'
    }

    stage ('Artifactory configuration') {
        rtMaven.tool = 'maven' // Tool name from Jenkins configuration
        rtMaven.resolver releaseRepo: 'maven-virtual', snapshotRepo: 'maven-virtual', server: artServer
        rtMaven.deployer releaseRepo: 'maven-test-local', snapshotRepo: 'maven-test-local', server: artServer
        rtMaven.deployer.artifactDeploymentPatterns.addInclude("multi3*")
        rtMaven.deployer.deployArtifacts = true
    }

    stage ('Exec Maven') {
        rtMaven.run pom: 'maven-example/pom.xml', goals: 'clean install', buildInfo: buildInfo
    }

    stage ('Publish build info') {
        artServer.publishBuildInfo buildInfo
    }

}

