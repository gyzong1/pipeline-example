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
        rtMaven.tool = "maven"
        // Specific dependency resolve repo
        rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: artiServer
        // Specific target repo
        rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: artiServer
        try {
            sh 'helm delete jfrog-helm-demo --purge'
            sh 'sleep 5'
        } catch(Exception e) {
            println('remove resources in kubernetes failed, please check the log.')
        }
    }
    stage('SCM') {
        // Checkout source code 上传helm-demo-master文件到自己的git仓库，拉取代码
        //git([url: 'git@gitlab.com:fuhui/helm-demo.git', branch: 'master'])
	
    }
    stage('Build') {
        // Maven build here
        rtMaven.run pom: 'pom.xml', goals: 'clean test install', buildInfo: buildInfo
        artiServer.publishBuildInfo buildInfo
    }
    stage('Image') {
        // Build docker image
        tagName = 'ha.example.com/docker-virtual/jfrog-cloud-demo:latest'
        docker.build(tagName)
        sh 'docker login ha.example.com -u admin -p AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8'
        sh 'docker push ha.example.com/docker-virtual/jfrog-cloud-demo:latest'
        // sh 'docker rmi docker-local.artifactory.cloud.demo/jfrog-cloud-demo:latest'
        // sh 'docker logout docker-local.artifactory.cloud.demo'
    }
    stage('Test') {
        // Smoke test
        docker.image(tagName).withRun('-p 8181:8080') { c->
            sleep 5
            //sh 'curl "http://127.0.0.1:8181"'
        }
    }
    stage('Package Helm Chart'){
        // Package code to Helm Chart
        sh 'helm package jfrog-cloud-chart --debug'
    }
    stage('Upload Chart'){
        // Upload Chart to artifactory helm repository
	// sh 'helm repo add helm-virtual http://ha.example.com/artifactory/helm-virtual --username admin --password AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8'
        sh 'curl -u admin:password -X PUT "http://ha.example.com/helm-virtual/jfrog-cloud-chart-0.1.0.tgz" -T jfrog-cloud-chart-0.1.0.tgz'
    }
    stage('Deploy Helm Chart'){
        // Deploy to kubernetes via helm client
        sh 'helm repo update'
        sh 'helm install helm-virtual/jfrog-cloud-chart --name jfrog-helm-demo'
    }
}
