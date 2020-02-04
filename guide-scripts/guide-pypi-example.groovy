node {
    
    def DEPLOYREPO="guide-pypi-dev-local"
    
    stage('Prepare') {
        sh 'jfrog rt c art1 --url=http://192.168.230.155:8081/artifactory --user=admin --password=password'
        sh 'jfrog rt use art1'
    }
    stage('SCM') {
        // cleanWs()
        sh 'ls'
        git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
    }
    
    stage('Build') {
        dir('project-examples/python-example') {
          sh "jfrog rt pipi -r requirements.txt --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
          // sh "jfrog rt pipi --force-reinstall -r requirements.txt --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
        }
    }
    
    stage('Package the project') {
        dir('project-examples/python-example') {
          sh "python setup.py sdist bdist_wheel"
        }
    }
    
    stage('Publish packages') {
        dir('project-examples/python-example') {
          sh "jfrog rt u dist/ ${DEPLOYREPO} --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
        }
    }
    
    stage('Collect environment variables') {
        dir('project-examples/python-example') {
          sh "jfrog rt bce ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Publish the build info') {
        dir('project-examples/python-example') {
          sh "jfrog rt bp ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
}
