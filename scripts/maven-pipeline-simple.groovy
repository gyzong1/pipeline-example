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
        // 或 env.MAVEN_HOME = '/tools/apache-maven-3.3.9'
        // 指定jdk版本
        // env.JAVA_HOME = 'full/path/to/JDK'
        rtMaven.resolver releaseRepo: 'maven-virtual', snapshotRepo: 'maven-virtual', server: artServer
        rtMaven.deployer releaseRepo: 'maven-test-local', snapshotRepo: 'maven-test-local', server: artServer
      
        // 只包含某部分包，或排除某部分
        // rtMaven.deployer.artifactDeploymentPatterns.addInclude("*multi3*")
        // rtMaven.deployer.artifactDeploymentPatterns.addExclude("*.zip")
        // rtMaven.deployer.artifactDeploymentPatterns.addInclude("frog*").addExclude("*.zip")
      
        // 添加属性
        rtMaven.deployer.addProperty("status", "in-qa").addProperty("compatibility", "1", "2", "3")
      
        // 禁用部署工件
        // rtMaven.deployer.deployArtifacts = false
        // 稍后发布工件
        // rtMaven.deployer.deployArtifacts buildInfo   
    }

    stage ('Env capture') {
        // 搜集环境变量
        buildInfo.env.capture = true
        // 包含或排除
        // buildInfo.env.filter.addInclude("*a*")
        // buildInfo.env.filter.addExclude("DONT_COLLECT*")
    }
    stage ('Exec Maven') {
        rtMaven.run pom: 'maven-example/pom.xml', goals: 'clean install', buildInfo: buildInfo
    }
  
    stage ('Publish build info') {
        artServer.publishBuildInfo buildInfo
    }

}

