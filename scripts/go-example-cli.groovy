node {
    
    env.NODE_HOME=tool name: 'go', type: 'go'
    env.PATH="/usr/local/go/bin:${env.PATH}"
    env.GOPROXY="http://admin:AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8@192.168.230.155:8081/artifactory/api/go/go-virtual"
    
    stage('Prepare') {
        sh 'jfrog rt c art1 --url=http://192.168.230.155:8081/artifactory --user=admin --password=password'        // 此处使用域名不好使，具体原因待查
        sh 'jfrog rt use art1'
    }
    stage('SCM') {
        // cleanWs()
        sh 'ls'
        //git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
    }
    
    stage('Build') {
        dir('project-examples/golang-example/hello') {
          // sh "jfrog rt go build go-virtual --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
          // sh "jfrog rt go build --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
          // sh 'export GOPROXY="http://admin:AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8@192.168.230.155:8081/artifactory/api/go/go-virtual"'
          sh 'go build'
        }
    }
    
    stage('Publish packages') {
        dir('project-examples/golang-example/hello') {
          sh "jfrog rt gp go-virtual v1.0.0 --deps=ALL --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
        }
    }

    stage('Collect environment variables') {
        dir('project-examples/golang-example/hello') {
          sh "jfrog rt bce ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Publish the build info') {
        dir('project-examples/golang-example/hello') {
          sh "jfrog rt bp ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
}
