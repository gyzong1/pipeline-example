node {
    def server = Artifactory.server 'art1'
    def rtGradle = Artifactory.newGradleBuild()
    def buildInfo = Artifactory.newBuildInfo()

    stage ('Clone') {
        git url: 'https://github.com/JFrog/project-examples.git'
    }

    stage ('Artifactory configuration') {
        rtGradle.tool = 'gradle' // Tool name from Jenkins configuration
        rtGradle.deployer repo: 'guide-gradle-dev-local', server: server
        rtGradle.resolver repo: 'guide-gradle-virtual', server: server
    }

    withEnv (['DONT_COLLECT=FOO']) {
        stage ('Config Build Info') {
            buildInfo.env.capture = true
            buildInfo.env.filter.addInclude ("*")
            buildInfo.env.filter.addExclude ("DONT_COLLECT*")
        }

        stage ('Extra gradle configurations') {
            rtGradle.deployer.artifactDeploymentPatterns.addExclude ("*.war")
            rtGradle.usesPlugin = true // Artifactory plugin already defined in build script
        }

        stage ('Exec Gradle') {
            rtGradle.run rootDir: "gradle-examples/gradle-example/", buildFile: 'build.gradle', tasks: 'clean artifactoryPublish', buildInfo: buildInfo
        }

        stage ('Publish build info') {
            server.publishBuildInfo buildInfo
        }
    }
}
