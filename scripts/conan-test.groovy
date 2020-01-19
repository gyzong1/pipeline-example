node {
        
        stage("scm"){
            cleanWs()
            // Clone the code from github:
            git url :'https://github.com/gyzong1/example-poco-timer.git'
            //git url :'https://github.com/memsharded/hello-use'
            //git url : 'https://github.com/lasote/conan-goserver-example'
        }
        
        stage("config"){
            //sh "conan remote add conan http://192.168.230.155/artifactory/api/conan/conan-local"
            //sh "conan remote add test http://192.168.230.155:8081/artifactory/api/conan/conan-virtual"
            sh "conan user -p AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8 -r test admin"
        }  
        
        stage("build&push"){
            sh "conan install . -r test --build missing"
            // sh "conan upload * -r test --all"
        }     
}
