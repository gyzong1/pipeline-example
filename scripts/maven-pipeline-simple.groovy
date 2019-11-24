node {

  def artServer = Artifactory.server 'art1'
  def rtMaven = Artifactory.newMavenBuild()
  def buildInfo = Artifactory.newBuildInfo()
  
  stage 'Clone'
        git url: 'https://github.com/gyzong1/spring-boot-samples.git', branch: 'master'

  stage 'Build Maven'
    dir ('./spring-boot-basewebapp/'){
      
      rtMaven.resolver server: artServer, releaseRepo: 'maven-virtual', snapshotRepo: 'maven-virtual'
      rtMaven.deployer server: artServer, releaseRepo: 'maven-dev-local', snapshotRepo: 'maven-dev-local'
      rtMaven.tool = 'maven'
      rtMaven.deployer.deployArtifacts = true
     // rtMaven.deployer.artifactDeploymentPatterns.addInclude("frog*")
      rtMaven.deployer.artifactDeploymentPatterns.addInclude("*.zip")
      rtMaven.run pom: 'pom.xml', goals: 'clean package', buildInfo: buildInfo
      
      

      artServer.publishBuildInfo buildInfo
    }

}

