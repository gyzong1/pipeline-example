node {
    
    //env.NODE_HOME=tool name: 'nodejs', type: 'nodejs'
    //env.PATH="${env.NODE_HOME}/bin:${env.PATH}"
    
    stage('Prepare') {
        sh 'jfrog config add art1 --overwrite=true --artifactory-url=http://124.70.55.35:9082/artifactory --user=admin --password= --interactive=false'        
        sh 'jfrog config show'  
        sh 'jfrog config use art1'
        
    }
    stage('SCM') {
        // cleanWs()
        sh 'ls'
        git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
    }
    
    stage('Build') {
        dir('project-examples/python-example') {
          
          sh 'jfrog rt pipc --server-id-resolve=art1 --repo-resolve=pypi-virtual'
          sh "jfrog rt pip-install --trusted-host 124.70.55.35 . --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
          sh "jfrog rt bce ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Publish') {
        dir('project-examples/python-example') {
          sh "jfrog rt bp ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }

    stage('Publish') {
        dir('project-examples/python-example') {
          sh "jfrog rt bs --fail=false ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
}
