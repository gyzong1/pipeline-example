node {
    def server = Artifactory.server 'art1'
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo

    stage ('Clone') {
        git url: 'https://github.com/JFrog/project-examples.git'
    }

    stage ('Artifactory configuration') {
        rtMaven.tool = 'maven' // Tool name from Jenkins configuration
        rtMaven.deployer releaseRepo: 'guide-maven-dev-local', snapshotRepo: 'guide-maven-dev-local', server: server
        rtMaven.resolver releaseRepo: 'guide-maven-virtual', snapshotRepo: 'guide-maven-virtual', server: server
        buildInfo = Artifactory.newBuildInfo()
    }

    stage ('Exec Maven') {
        rtMaven.run pom: 'maven-example/pom.xml', goals: 'clean install', buildInfo: buildInfo
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }
}
