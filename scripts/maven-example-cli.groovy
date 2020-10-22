node {
    
    // env.NODE_HOME=tool name: 'nodejs', type: 'nodejs'
    // env.PATH="${env.NODE_HOME}/bin:${env.PATH}"
    
    stage('Prepare') {
        sh 'jfrog rt c art --url=http://192.168.230.155:8081/artifactory --user=admin --password=password'
        sh 'jfrog rt use art'
    }
    stage('SCM') {
        // cleanWs()
        sh 'ls'
        git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
    }
    
    stage('Build') {
        dir('project-examples/maven-example') {
          sh "jfrog rt mvn clean install -f pom.xml --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
          sh "jfrog rt bce ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Publish') {
        dir('project-examples/maven-example') {
          sh "jfrog rt bp ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
}
