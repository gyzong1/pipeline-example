node {
    def server = Artifactory.server 'art1'
    def rtDocker = Artifactory.docker server: server
    def buildInfo = Artifactory.newBuildInfo()
    def ARTIFACTORY_DOCKER_REGISTRY='192.168.230.155/guide-docker-dev-local'

    stage ('Clone') {
        git url: 'https://github.com/JFrog/project-examples.git'
    }

    stage ('Add properties') {
        // Attach custom properties to the published artifacts:
        rtDocker.addProperty("project-name", "docker1").addProperty("status", "stable")
    }

    stage ('Docker login') {
        sh 'docker login -u admin -p password 192.168.230.155:8081'
    }

    stage ('Build docker image') {
        docker.build(ARTIFACTORY_DOCKER_REGISTRY + '/hello-world:latest', 'jenkins-examples/pipeline-examples/resources')
    }

    stage ('Push image to Artifactory') {
        buildInfo = rtDocker.push ARTIFACTORY_DOCKER_REGISTRY + '/hello-world:latest', 'guide-docker-dev-local'
    }

    stage ('Pull image from Artifactory') {
        sh 'docker pull 192.168.230.155:8081/guide-docker-virtual/busybox'
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }
}
