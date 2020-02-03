node {
    def server = Artifactory.server 'art1'
    def rtGradle = Artifactory.newGradleBuild()
    def buildInfo

    stage ('Clone') {
        git url: 'https://github.com/JFrog/project-examples.git'
    }

    stage ('Artifactory configuration') {
        rtGradle.tool = 'gradle' // Tool name from Jenkins configuration
        // rtGradle.useWrapper = true
        rtGradle.deployer repo: 'gradle-virtual', server: server
        rtGradle.resolver repo: 'gradle-virtual', server: server
    }

    stage ('Exec Gradle') {
        buildInfo = rtGradle.run rootDir: "gradle-examples/gradle-example-ci-server/", buildFile: 'build.gradle', tasks: 'clean install artifactoryPublish'
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }
}
