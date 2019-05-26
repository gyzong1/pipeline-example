node {

    env.NODE_HOME=tool name: 'nodejs', type: 'nodejs'
    env.PATH="${env.NODE_HOME}/bin:${env.PATH}"

    def server = Artifactory.server 'art1'
    def rtNpm = Artifactory.newNpmBuild()
    def buildInfo

    stage ('Clone') {
        git url: 'https://github.com/JFrog/project-examples.git'
    }

    stage ('Artifactory configuration') {
        rtNpm.deployer repo: 'npm-local', server: server
        rtNpm.resolver repo: 'npm-remote', server: server
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
    
    sh "curl -uadmin:admin -X POST 'http://192.168.230.135:8081/artifactory/api/copy/npm-local/npm-example/-/npm-example-0.0.3.tgz?to=/npm-local/npm-example/-/npm-example-0.0.3.${env.BUILD_NUMBER}.tgz'"

}
