node {
    env.ANDROID_HOME="/usr/local/android-sdk-linux"
    env.PATH="$ANDROID_HOME/tools:$PATH"
    def server = Artifactory.server 'art1'
    def rtGradle = Artifactory.newGradleBuild()
    def buildInfo

    stage ('Clone') {
        git url: 'https://github.com/jfrog/project-examples.git'
        //git url: 'https://gitee.com/gyzong1/project-examples.git'
    }

    stage ('Artifactory configuration') {
        rtGradle.tool = 'gradle' // Tool name from Jenkins configuration
        // rtGradle.useWrapper = true  
        rtGradle.usesPlugin = true
        rtGradle.deployer repo: 'guide-gradle-dev-local', server: server
        rtGradle.resolver repo: 'guide-gradle-virtual', server: server
    }

    stage ('Exec Gradle') {
        buildInfo = rtGradle.run rootDir: "gradle-examples/gradle-android-example/", buildFile: 'build.gradle', tasks: 'clean artifactoryPublish'
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }
}
