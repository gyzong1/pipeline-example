// Artifactory server instance, defined in Jenkins --> Manage:
def artifactory_server = 'art1'

// Conan repository in Artifactory server
def artifactory_repo = 'conan-virtual-local'

// Binary dev repository in Artifactory server
def dev_repo = 'conan-dev-local'

// Binary release repository in Artifactory server
def release_repo = 'conan-pro-local'

// User/Pass for Artifactory server
def user_name = 'admin'
def user_apikey = 'AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8'

// Source Code project in github
def repo_url = 'https://github.com/xingao0803/demo-poco-timer.git'
def repo_branch = 'master'

node {
    def server
    def client
    def serverName
    def buildInfo
    def settings
    def test = false
    def deploy = false

    stage("Get project"){
        // Clone source code from github:
        git branch: repo_branch, url: repo_url
    }
    
    stage("Configure Artifactory/Conan"){
        // Obtain an Artifactory server instance: 
        server = Artifactory.server artifactory_server

        // Create a conan client instance:
        client = Artifactory.newConanClient()
 
        // Add a new repository to the conan client.
        // The 'remote.add' method returns a 'serverName' string, which is used later in the script:
        serverName = client.remote.add server: server, repo: artifactory_repo

        // Create a local build-info instance:
        buildInfo = Artifactory.newBuildInfo()
        
        // Login the new conan remote server
        def command = "user -r ${serverName} -p ${user_apikey} ${user_name}"
        client.run(command: command.toString())
    }

    stage("Get dependencies from Artifactory repository"){
        sh "mkdir -p build"
        dir ('build') {
            // Download dependencies, build if only source code there
            def command = "install .. -r ${serverName}"
            client.run(command: command.toString(), buildInfo: buildInfo)
        }
    }

    stage("Get settings"){
        dir ('build') {
            settings = "build.name=${buildInfo.name};build.number=${buildInfo.number}"
            def add_props = 0
            def file = readFile "conaninfo.txt"
            def lines = file.split('\n')
            for ( line in lines) {
                if ( add_props == 0 ) {
                    if ( line.contains("[full_settings]") ) {
                        add_props = 1
                    }
                } 
                else {
                    if ( add_props == 1 ) {
                        print line
                        if ( line.contains("=") ) {
                            settings = settings.concat(";").concat(line.trim())
                        }
                        else {
                            add_props = 0    
                        }
                    }
                }
            }
        }
    }

    stage("Build binary app"){
        dir ('build') {
            // Build with g++
            sh "g++ ../*.cpp @conanbuildinfo.args -o timer"
        }
    }
    
    stage("Upload binary app"){
        dir ('build') {
            // Upload binary app with settings to artifactory
            tartgetRepo = "${dev_repo}/poco-timer/${buildInfo.number}/timer-${buildInfo.number}"
            def uploadSpec = """{
                "files": [
                    {
                        "pattern": "./timer",
                        "target": "${tartgetRepo}",
                        "props": "${settings}"
                    }
                ]
            }""" 

            def buildInfo1 = server.upload(uploadSpec)
            buildInfo.append buildInfo1
            server.publishBuildInfo buildInfo
        }
    }

    stage("Test"){
        dir ('build') {
            sh "./timer"
            echo "Test Completed!"
            test = true
        }
    }

    stage('Bind Test Metadata') {
        //Add test results
        if (test) {
            sleep 3
            sh "curl -X PUT http://192.168.230.155:8081/artifactory/api/storage/${dev_repo}/poco-timer/${buildInfo.number}/timer-${buildInfo.number}?properties=test=done -u${user_name}:${user_apikey}"
        } else {
            echo 'Test NOT finished yet, stop here.'
            sh "curl -X PUT http://192.168.230.155:8081/artifactory/api/storage/${dev_repo}/poco-timer/${buildInfo.number}/timer-${buildInfo.number}?properties=test=failed -u${user_name}:${user_apikey}"    
            error("Pipeline failed because of test failed")
        }
    }
    
    stage('Promote') {
        def promotionConfig = [
            // Mandatory parameters
            'buildName'          : buildInfo.name,
            'buildNumber'        : buildInfo.number,
            'targetRepo'         : release_repo,
 
            // Optional parameters
            'comment'            : 'this is the promotion comment',
            'sourceRepo'         : dev_repo,
            'status'             : 'Released',
            'includeDependencies': false,
            'copy'               : true,
            // 'failFast' is true by default.
            // Set it to false, if you don't want the promotion to abort upon receiving the first error.
            'failFast'           : true
        ]
 
        // Promote build
        server.promote promotionConfig
    }

    stage('Deploy') {
        sh "mkdir -p deploy"
        dir ('deploy') {
            //Download App
            def downloadSpec = """ {
                "files": [
                    {
                        "aql": {
                            "items.find": {
                                "repo": "${release_repo}",
                                "name": {"\$match": "timer-${buildInfo.number}"},
                                "@test":{"\$eq":"done"},
                                "@arch":{"\$eq":"x86_64"},
                                "@os":{"\$eq":"Linux"},
                                "@compiler":{"\$eq":"gcc"}
                            }
                        },
                        "flat": "true"
                    } 
                ]
            } """
        
            server.download(downloadSpec)
            sleep 3
            //sh "chmod +x timer-${buildInfo.number}"
            //sh "./timer-${buildInfo.number}"
            echo "Deploy Completed!"
            deploy = true
        }
    }

    stage('Bind Deploy Metadata') {
        //Add test results
        if (deploy) {
            sleep 3
            sh "curl -X PUT http://192.168.230.155:8081/artifactory/api/storage/${release_repo}/poco-timer/${buildInfo.number}/timer-${buildInfo.number}?properties=deploy=done -u${user_name}:${user_apikey}"
        } else {
            echo 'Deploy NOT finished yet, stop here.'
            sh "curl -X PUT http://192.168.230.155:8081/artifactory/api/storage/${release_repo}/poco-timer/${buildInfo.number}/timer-${buildInfo.number}?properties=deploy=failed -u${user_name}:${user_apikey}"
            error("Pipeline failed because of deploy failed")
        }
    }

    stage("Cleanup"){
        sh "rm -rf build"
        sh "rm -rf build@tmp"
        sh "rm -rf deploy"
        sh "rm -rf deploy@tmp"
    }

}
