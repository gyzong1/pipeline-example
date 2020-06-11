import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def user_apikey

withCredentials([string(credentialsId: 'platform-key', variable: 'secret_text')]) {
    user_apikey = "${secret_text}"
}

node {
	def artiServer
	def buildInfo
	def rtMaven
	def warVersion
	def requirements
    def jira_urls
    def revisionIds
    def sonar_Total

	stage('Prepare') {
		artiServer = Artifactory.server('arti-platform')
	}
	
	stage('SCM') {
		git branch: 'master', url: 'https://github.com/xingao0803/maven-releasemgt-pipeline.git'
	}
	
	stage('Update Version') {
	    warVersion = "${SNAPSHOT_Version_Pre}"
		sh "sed -i 's/-BUILD_NUMBER-/${warVersion}/g' pom.xml **/pom.xml"
	}

	//执行maven构建SNAPSHOT包
	stage('SNAPSHOT Maven Build'){
	    buildInfo = Artifactory.newBuildInfo()
	    buildInfo.name = 'Platform-maven-releasemgt-snapshot'
		buildInfo.env.capture = true
		rtMaven = Artifactory.newMavenBuild()

		rtMaven.resolver server: artiServer, snapshotRepo: 'releasemgt-maven-snapshot-virtual', releaseRepo: 'releasemgt-maven-release-virtual'
		rtMaven.deployer server: artiServer, snapshotRepo: 'releasemgt-maven-snapshot-local', releaseRepo: 'releasemgt-maven-stage-local'
		
		rtMaven.tool = 'maven'
        rtMaven.run pom: './pom.xml', goals: 'clean install', buildInfo: buildInfo

		artiServer.publishBuildInfo buildInfo
		
	}
	
	stage('Add JIRAResult to SNAPSHOT'){
	    def returnList = getRequirements();
	    
	    if (returnList.size() != 0) { 
            requirements = returnList[0];
            echo "requirements : ${requirements}"
            jira_urls = returnList[1];
            revisionIds = getRevisionIds();
            echo "revisionIds : ${revisionIds}"

            commandJira = "curl -H \"X-JFrog-Art-Api: ${user_apikey}\" -X PUT \""+artiServer.url+"/api/storage/releasemgt-maven-snapshot-local/com/jfrogchina/demo/multi3/"+warVersion+"-SNAPSHOT/multi3-"+warVersion+"-SNAPSHOT.war?properties=project.issues="+ requirements +";project.issues.urls="+jira_urls+";project.revisionIds="+ revisionIds +"\" ";
	        process = [ 'bash', '-c', commandJira].execute().text
	    }
	 
   }

	stage('Sonar Test for SNAPSHOT') {
		// Sonar scan
            def scannerHome = tool 'sonarClient';
            withSonarQubeEnv('sonar') {
                sh "${scannerHome}/bin/sonar-runner -Dsonar.projectKey=${JOB_BASE_NAME} -Dsonar.sources=./multi3/src/main -Dsonar.tests=./multi3/src/test"
            }
	}
	//添加sonar扫描结果到SNAPSHOT包上
	stage("Add SonarResult to SNAPSHOT"){
		    //获取sonar扫描结果
		    def getSonarIssuesCmd = "curl  GET -v http://47.93.114.82:9000/api/issues/search?componentRoots=${JOB_BASE_NAME}";
		    echo "getSonarIssuesCmd:"+getSonarIssuesCmd
		    process = [ 'bash', '-c', getSonarIssuesCmd].execute().text

		    //增加sonar扫描结果到artifactory
		    def jsonSlurper = new JsonSlurper()
		    def issueMap = jsonSlurper.parseText(process);
		    echo "issueMap:"+issueMap
		    echo "Total:"+issueMap.total
		    sonar_Total =  issueMap.total
		    
		    commandSonar = "curl -H \"X-JFrog-Art-Api: ${user_apikey}\" -X PUT \""+artiServer.url+"/api/storage/releasemgt-maven-snapshot-local/com/jfrogchina/demo/multi3/"+warVersion+"-SNAPSHOT/multi3-"+warVersion+"-SNAPSHOT.war?properties=" + 
		                    "quality.gate.sonarUrl=http://47.93.114.82:9000/dashboard/index/${JOB_BASE_NAME};quality.gate.sonarIssue="+sonar_Total+"\" ";
		    echo commandSonar
		    process = [ 'bash', '-c', commandSonar].execute().text
	}
	
	//生成Release版本
    stage('Generate Release Version'){
        
        if( sonar_Total < 4 ) {
            warVersion = "${Release_Version_Pre}.${BUILD_NUMBER}"
            def descriptor = Artifactory.mavenDescriptor()
            descriptor.version = warVersion
            descriptor.failOnSnapshot = true
            descriptor.transform()
        }else{
            exit()
        }
    }
    
	//执行maven构建Release包
	stage('Release Maven Build'){
	    buildInfo = Artifactory.newBuildInfo()
	    buildInfo.name = 'Platform-maven-releasemgt-release'
		buildInfo.env.capture = true
		rtMaven = Artifactory.newMavenBuild()

		rtMaven.resolver server: artiServer, releaseRepo: 'releasemgt-maven-release-virtual', snapshotRepo: 'releasemgt-maven-snapshot-virtual'
		rtMaven.deployer server: artiServer, releaseRepo: 'releasemgt-maven-stage-local', snapshotRepo: 'releasemgt-maven-snapshot-local'
		
		rtMaven.tool = 'maven'
        rtMaven.run pom: './pom.xml', goals: 'clean install', buildInfo: buildInfo
        
        def config = """{
                    "version": 1,
                    "issues": {
                            "trackerName": "JIRA",
                            "regexp": "#([\\w\\-_\\d]+)\\s(.+)",
                            "keyGroupIndex": 1,
                            "summaryGroupIndex": 2,
                            "trackerUrl": "http://jira.jfrogchina.com:8081/browse/",
                            "aggregate": "true",
                            "aggregationStatus": "Released"
                    }
                }"""

 
        buildInfo.issues.collect(artiServer, config)
        
		artiServer.publishBuildInfo buildInfo
		
	}
	
	stage('Add Metadata to Release Package'){
	    
	    commandData = "curl -H \"X-JFrog-Art-Api: ${user_apikey}\" -X PUT \""+artiServer.url+"/api/storage/releasemgt-maven-stage-local/com/jfrogchina/demo/multi3/"+warVersion+"/multi3-"+warVersion+".war?properties=" + 
	                  "quality.gate.sonarUrl=http://47.93.114.82:9000/dashboard/index/${JOB_BASE_NAME};quality.gate.sonarIssue="+sonar_Total+
	                  ";project.issues="+ requirements +";project.issues.urls="+jira_urls+";project.revisionIds="+ revisionIds + "\" ";
		echo commandData
		process = [ 'bash', '-c', commandData].execute().text

	}

	stage('xray scan'){
		    def xrayConfig = [
                'buildName'     : buildInfo.name,
                'buildNumber'   : buildInfo.number,
                'failBuild'     : false
            ]

            def xrayResults = artiServer.xrayScan xrayConfig
            echo xrayResults as String

            def jsonSlurper = new JsonSlurper()
    	    def xrayresult = jsonSlurper.parseText(xrayResults.toString())
    	    echo "Xray Result total issues:" + xrayresult.alerts[0].issues.size()
    	    commandText = "curl  -H \"X-JFrog-Art-Api: ${user_apikey}\" -X PUT \""+artiServer.url+"/api/storage/releasemgt-maven-stage-local/com/jfrogchina/demo/multi3/"+warVersion+"/multi3-"+warVersion+".war?properties=Xray=scanned;Xray_issues_number="+xrayresult.alerts[0].issues.size()+"\" ";
	        process = [ 'bash', '-c', commandText].execute().text
	}
   
	stage('Test Approval'){
    	commandText = "curl  -H \"X-JFrog-Art-Api: ${user_apikey}\" -X PUT \""+artiServer.url+"/api/storage/releasemgt-maven-stage-local/com/jfrogchina/demo/multi3/"+warVersion+"/multi3-"+warVersion+".war?properties=test.approve=true\" ";
	    process = [ 'bash', '-c', commandText].execute().text
	    
	} 

	//promotion操作，进行包的升级
	stage('promotion'){
		def promotionConfig = [
			'buildName'   : buildInfo.name,
			'buildNumber' : buildInfo.number,
			'targetRepo'  : 'releasemgt-maven-release-local',
			'comment': 'this is the promotion comment',
			'sourceRepo':'releasemgt-maven-stage-local',
			'status': 'Released',
			'includeDependencies': false,
			'failFast': true,
			'copy': true
		]
		artiServer.promote promotionConfig
		
	}

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

}


@NonCPS
def getRequirements(){
    def reqIds = "";
    def urls = "";
    def jira_url = "http://jira.jfrogchina.com:8081/browse/";
    
    final changeSets = currentBuild.changeSets
    echo 'changeset count:'+ changeSets.size().toString()
    if ( changeSets.size() == 0 ) {
        return reqIds;
    }
    final changeSetIterator = changeSets.iterator()
    while (changeSetIterator.hasNext()) {
        final changeSet = changeSetIterator.next();
        def logEntryIterator = changeSet.iterator();
        while (logEntryIterator.hasNext()) {
            final logEntry = logEntryIterator.next()
            def patten = ~/#[\w\-_\d]+/;
            def matcher = (logEntry.getMsg() =~ patten);
            def count = matcher.getCount();
            for (int i = 0; i < count; i++){
                reqIds += matcher[i].replace('#', '') + ","
                urls += jira_url + matcher[i].replace('#', '') + ","
            }
        }
    }
    
    def returnList = [ reqIds[0..-2], urls[0..-2] ]
    return returnList;
}

@NonCPS
 def getRevisionIds(){
    def reqIds = "";
    final changeSets = currentBuild.changeSets
    if ( changeSets.size() == 0 ) {
        return reqIds;
    }
    final changeSetIterator = changeSets.iterator()
    while (changeSetIterator.hasNext()) {
        final changeSet = changeSetIterator.next();
        def logEntryIterator = changeSet.iterator();
        while (logEntryIterator.hasNext()) {
            final logEntry = logEntryIterator.next()
            reqIds += logEntry.getRevision() + ","
        }
    }
    return reqIds[0..-2]
}
