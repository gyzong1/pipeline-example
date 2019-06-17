node {
    
    //env.NODE_HOME=tool name: 'go', type: 'go'
    //env.PATH="/usr/local/go/bin:${env.PATH}"
    
    stage('Prepare') {
        sh 'jfrog rt c art1 --url=http://192.168.230.155:8081/artifactory --user=admin --password=password'        // 此处使用域名不好使，具体原因待查
        sh 'jfrog rt use art1'
    }
    stage('SCM') {
        // cleanWs()
        sh 'ls'
        git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
    }
    
    stage('Build') {
        dir('project-examples/golang-example/python-example') {
          sh "pip install -r requirements.txt"
        }
    }
    
    stage('Publish packages') {
        dir('project-examples/golang-example/python-example') {
          sh "python setup.py sdist upload -r local"
        }
    }
    
    stage('Collect environment variables') {
        dir('project-examples/golang-example/python-example') {
          sh "jfrog rt bce ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Publish the build info') {
        dir('project-examples/golang-example/python-example') {
          sh "jfrog rt bp ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
}
