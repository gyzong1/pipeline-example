node {
    
    // env.NODE_HOME=tool name: 'nodejs', type: 'nodejs'
    // env.PATH="${env.NODE_HOME}/bin:${env.PATH}"
    env.M2_HOME="/usr/local/apache-maven-3.6.0"
    
    stage('Prepare') {
        // sh 'jfrog rt c art1 --url=http://124.70.55.35:8082/artifactory --user=admin --password=password --interactive=false'
        // sh 'jfrog config add art1 --overwrite=true --artifactory-url=http://124.70.55.35:9082/artifactory --user=admin --password=password --interactive=false'
        sh 'jfrog config use art1'
    }
    stage('SCM') {
        // cleanWs()
        sh 'ls'
        //git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
        git([url: 'https://gitee.com/gyzong1/pipeline-example.git', branch: 'master'])
        
    }
    
    stage('Build') {
        dir('project-examples/maven-example') {
          sh "jfrog rt mvnc --global=false --server-id-resolve=art1 --server-id-deploy=art1 --repo-resolve-releases=maven-virtual --repo-resolve-snapshots=maven-virtual --repo-deploy-releases=maven-dev-local --repo-deploy-snapshots=maven-dev-local"
          sh "jfrog rt mvn clean install -f pom.xml --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
          sh "jfrog rt bce ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Add Jira Issue') {
        dir('project-examples/maven-example') {
          sh "jfrog rt bag ${env.JOB_NAME} ${env.BUILD_NUMBER} --config jira-cli.conf"
        }
    }
    
    stage('Publish') {
        dir('project-examples/maven-example') {
          sh "jfrog rt bp ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Scan') {
        // sh "jfrog rt bs ${env.JOB_NAME} ${env.BUILD_NUMBER} --fail=false"
    }
    

}
