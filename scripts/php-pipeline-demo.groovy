node {

    stage('SCM') {
        // cleanWs()
        sh 'ls'
        git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
    }
    
    stage('Build') {
        dir('project-examples/php-example') {
          sh "php /usr/local/bin/composer init"
        }
    }
    
    stage('Package') {
        dir('project-examples/php-example') {
            sh "cd .. && tar -zcvf php-demo-${env.BUILD_NUMBER}.tar.gz ${env.JOB_NAME}"
        }
    }
    
    stage('Upload') {
        dir('project-examples/php-example') {
            sh "cd .. && curl -uadmin:password -X PUT http://192.168.230.155:8081/artifactory/composer-dev-local/${env.BUILD_NUMBER}/php-demo-${env.BUILD_NUMBER}.tar.gz -T php-demo-${env.BUILD_NUMBER}.tar.gz"
        }
    }
    
}
