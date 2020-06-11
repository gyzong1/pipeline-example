    stage('Deploy') {
        def downloadSpec = """ {
            "files": [
                {
                    "aql": {
                        "items.find": {
                            "repo": "releasemgt-maven-release-local",
                            "name": {"\$match": "multi3-${warVersion}.war"},
                            "@quality.gate.sonarIssue":{"\$lt":"4"},
                            "@Xray":{"\$eq":"scanned"},
                            "@test.approve":{"\$eq":"true"}
                        }
                    },
                    "flat": "true"
                } 
            ]
        } """
      
        artiServer.download(downloadSpec)
        sleep 3
        sh "ls -l multi3-*.war"
        
        def path = sh returnStdout: true , script: "pwd"
        path = path.trim()

        //Deploying
		def deployCmd = "ansible 127.0.0.1 -c local -m copy -a 'src=${path}/multi3-${warVersion}.war  dest=/home/tomcat/apache-tomcat-8.5.40/webapps/demo1.war'"
		echo deployCmd
		process = sh returnStdout: true , script: deployCmd
		echo process
		
		//Add Deployment Metadata
    	commandText = "curl  -H \"X-JFrog-Art-Api: ${user_apikey}\" -X PUT \""+artiServer.url+"/api/storage/releasemgt-maven-release-local/com/jfrogchina/demo/multi3/"+warVersion+"/multi3-"+warVersion+".war?properties=" + 
    	              "deploy.tool=ansible;deploy.env=127.0.0.1\" ";
	    process = [ 'bash', '-c', commandText].execute().text
    
        //Restarting Tomcat
		def stopCmd = "ansible 127.0.0.1 -c local -m shell -a '/home/tomcat/apache-tomcat-8.5.40/bin/shutdown.sh'"
		echo stopCmd
		[ 'bash', '-c', stopCmd].execute().text
		
		def startCmd = "ansible 127.0.0.1 -c local -m shell -a '/home/tomcat/apache-tomcat-8.5.40/bin/startup.sh'"
		echo startCmd
		[ 'bash', '-c', startCmd].execute().text
        
        //ClearUp
        sh "rm multi3*.war"

        echo "Deploy Completed!"
    }
