node {
        
        stage("scm"){
            cleanWs()
            // Clone the code from github:
            git url :'https://github.com/gyzong1/example-poco-timer.git'
        }
        
        stage("config"){
            try {
                sh "conan remote add guide-conan-virtual http://192.168.230.155/artifactory/api/conan/guide-conan-virtual"
                sh "conan user -p AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8 -r guide-conan-virtual admin"
            } catch (e) {
                echo "guide-conan-virtual already exists in remotes"
            }

            try {
                sh "conan remote add guide-conan-dev-local http://192.168.230.155:8081/artifactory/api/conan/guide-conan-dev-local"
                sh "conan user -p AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8 -r guide-conan-dev-local admin"  
            } catch (e) {
                echo "guide-conan-dev-local already exists in remotes"
            }
 
        }  
        
        stage("resolve"){
            sh "conan install . -r test --build missing"
        }   

        stage("upload"){
            sh "cp -r ~/.conan/data/zlib ./"
            sh "ls ./"
            sh "conan upload zlib/1.2.11@conan/stable -r guide-conan-dev-local --all"
        }       
}
