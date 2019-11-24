node {

  def artServer = Artifactory.server 'art1'
  //artServer.credentialsId='935750e8-8657-49c1-a012-00f276ce1173'
  def rtMaven = Artifactory.newMavenBuild()
  def buildInfo = Artifactory.newBuildInfo()
  
  stage 'Clone'
       // git url: 'https://github.com/lyqwaterway/spring-boot-samples.git', branch: 'master'
        git url: 'https://github.com/gyzong1/spring-boot-samples.git', branch: 'master'

  stage 'Build Maven'
    dir ('./spring-boot-basewebapp/'){
      
      rtMaven.resolver server: artServer, releaseRepo: 'gyz-test4-virtual', snapshotRepo: 'gyz-test4-virtual'
      rtMaven.deployer server: artServer, releaseRepo: 'gyz-test4-virtual', snapshotRepo: 'gyz-test4-virtual'
      rtMaven.tool = 'maven'
      rtMaven.deployer.deployArtifacts = true
     // rtMaven.deployer.artifactDeploymentPatterns.addInclude("frog*")
      rtMaven.run pom: 'pom.xml', goals: 'clean package', buildInfo: buildInfo
      
      

      artServer.publishBuildInfo buildInfo
    }

}

