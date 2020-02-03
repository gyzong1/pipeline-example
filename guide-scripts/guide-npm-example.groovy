node {
    def server = Artifactory.server 'art1'
    def rtNpm = Artifactory.newNpmBuild()
    def buildInfo

    stage ('Clone') {
        git url: 'https://github.com/JFrog/project-examples.git'
    }

    stage ('Artifactory configuration') {
        rtNpm.deployer repo: 'guide-npm-dev-local', server: server
        rtNpm.resolver repo: 'guide-npm-virtual', server: server
        rtNpm.tool = 'nodejs' // Tool name from Jenkins configuration
        buildInfo = Artifactory.newBuildInfo()
    }

    stage ('Install npm') {
        rtNpm.install buildInfo: buildInfo, path: 'npm-example'
    }

    stage ('Publish npm') {
        rtNpm.publish buildInfo: buildInfo, path: 'npm-example'
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }
}
