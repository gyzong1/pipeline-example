node {
        
        stage("scm"){
            // Clone the code from github:
            git url :'https://github.com/memsharded/example-poco-timer.git'
            //git url :'https://github.com/memsharded/hello-use'
            //git url : 'https://github.com/lasote/conan-goserver-example'
        }
        
        stage("config"){
            sh "conan remote add conan http://192.168.230.155/artifactory/api/conan/conan-local"
            sh "conan user -p AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8 -r conan admin"
        }  
        
        stage("build&push"){
            sh "conan install . --build missing"
            sh "conan upload * --all -r conan-local --confirm"
        }     
}
