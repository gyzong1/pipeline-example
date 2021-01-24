node {
    def mvnHome
    def artiServer
    def rtMaven
    def buildInfo
    def tagName
    stage('Prepare') {
        // Prepare for object initilization
        artiServer = Artifactory.server('art1')
        buildInfo = Artifactory.newBuildInfo()
        buildInfo.env.capture = true
        rtMaven = Artifactory.newMavenBuild()
        rtMaven.tool = "maven3"
        // Specific dependency resolve repo
        rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: artiServer
        // Specific target repo
        rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: artiServer
        try {
            // helm2
            //sh 'helm delete jfrog-helm-demo --purge'
            // helm3
            sh 'helm delete jfrog-helm-demo'
            sh 'sleep 5'
        } catch(Exception e) {
            println('remove resources in kubernetes failed, please check the log.')
        }
    }
    stage('SCM') {
        // Checkout source code
        git url: 'https://github.com/gyzong1/helm-demo.git'
	
    }
    stage('Build') {
        // Maven build here
        rtMaven.run pom: 'pom.xml', goals: 'clean test install', buildInfo: buildInfo
        artiServer.publishBuildInfo buildInfo
    }
    stage('Image') {
        // Build docker image
        tagName = '39.99.224.184:8082/docker-webinar-virtual/jfrog-cloud-demo:latest'
        docker.build(tagName)
        sh 'docker login 39.99.224.184:8082 -u admin -p AKCp8hyiwBrktjxqu8yUzPe1CTA6agQnVCT6rdMocDJhMK8Z831raDuWGPuhaxftfkWTjpzTf'
        sh 'docker push 39.99.224.184:8082/docker-webinar-virtual/jfrog-cloud-demo:latest'
        sh 'docker rmi 39.99.224.184:8082/docker-webinar-virtual/jfrog-cloud-demo:latest'
        // sh 'docker logout docker-local.artifactory.cloud.demo'
    }
    stage('Test') {
        // Smoke test
        docker.image(tagName).withRun('-p 8181:8080') { c->
            sleep 5
            sh 'curl "http://127.0.0.1:8181"'
        }
    }
    stage('Package Helm Chart'){
        // Package code to Helm Chart
        sh 'helm package jfrog-cloud-chart --debug'
    }
    stage('Upload Chart'){
        // Upload Chart to artifactory helm repository
	    // sh 'helm repo add helm-virtual http://ha.example.com/artifactory/helm-virtual --username admin --password AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8'
        sh 'curl -H "X-JFrog-Art-Api:AKCp8hyiwBrktjxqu8yUzPe1CTA6agQnVCT6rdMocDJhMK8Z831raDuWGPuhaxftfkWTjpzTf" -T jfrog-cloud-chart-0.1.0.tgz "http://39.99.224.184:8081/artifactory/helm-webinar-virtual/jfrog-cloud-chart-0.1.0.tgz"'
    }
    stage('Deploy Helm Chart'){
        // Deploy to kubernetes via helm client
        sh 'helm repo update'
        //sh 'helm install helm-webinar-virtual/jfrog-cloud-chart --name jfrog-helm-demo'
        sh 'helm upgrade --install jfrog-helm-demo helm-webinar-virtual/jfrog-cloud-chart'
    }
}
