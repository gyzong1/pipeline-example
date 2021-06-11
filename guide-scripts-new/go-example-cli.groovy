node {
    
    //env.NODE_HOME=tool name: 'nodejs', type: 'nodejs'
    //env.PATH="${env.NODE_HOME}/bin:${env.PATH}"
    
    stage('Prepare') {
        sh 'jfrog config add art1 --overwrite=true --artifactory-url=http://124.70.55.35:9082/artifactory --user=admin --password=password --interactive=false'        
        sh 'jfrog config show'  
        sh 'jfrog config use art1'
        
    }
    stage('SCM') {
        // cleanWs()
        sh 'ls'
        git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
    }
    
    stage('Build') {
        dir('project-examples/golang-example') {
          sh 'jfrog rt go-config --repo-deploy=go-dev-local --repo-resolve=go-virtual --server-id-deploy=art1 --server-id-resolve=art1'
          sh "jfrog rt go build --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
        }
    }
    
    stage('Publish Packages') {
        dir('project-examples/golang-example') {
          sh "jfrog rt gp go-dev-local v1.0.0 --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
        }
    }

    stage('Publish BuildInfo') {
        dir('project-examples/golang-example') {
          sh "jfrog rt bce ${env.JOB_NAME} ${env.BUILD_NUMBER}"
          sh "jfrog rt bp ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Build Scan') {
        dir('project-examples/golang-example') {
          sh "jfrog rt bs --fail=false ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
}
