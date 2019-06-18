node {
        
        stage("scm"){
            // Clone the code from github:
            git url :'https://github.com/memsharded/example-poco-timer.git'
            //git url :'https://github.com/memsharded/hello-use'
            //git url : 'https://github.com/lasote/conan-goserver-example'
        }
        stage("build&push"){
            sh "conan install . --build missing"
            sh "conan upload * --all -r conan-local --confirm"
        }     
}
