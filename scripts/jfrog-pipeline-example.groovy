node {

    //artifactory   
    def artiServer = Artifactory.server 'art1'
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo = Artifactory.newBuildInfo()
    def descriptor = Artifactory.mavenDescriptor()
    def ARTIFACTORY_URL = 'http://192.168.230.155:8081/artifactory/'
    def ARTIFACTORY_API_KEY = 'AKCp5ccv3oMbQuovKWLzCdRW2RnZW9Qb4agjxVA931J9SsJwwkEuAe1yknQtMBegJvDq8RSr8'
    def RESOLVE_SNAPSHOT_REPO = 'maven-virtual'
    def RESOLVE_RELEASE_REPO = 'maven-virtual'
    def DEPLOY_SNAPSHOT_REPO = 'maven-dev-local'
    def DEPLOY_RELEASE_REPO = 'maven-dev-local'

    def PROMOTION_SOURCE_REPO = 'maven-dev-local'
    def PROMOTION_TARGET_REPO = 'maven-pro-local'

    //maven
    def MAVEN_TOOL = 'maven'
    def MAVEN_GOALS = 'clean install'
    def POM_PATH = 'project-examples/maven-example/pom.xml'

    //git
    // def GIT_URL = 'https://gitee.com/gyzong1/pipeline-example.git'  
    def GIT_URL = 'https://github.com/gyzong1/pipeline-example.git'
    def BRANCH = 'master'
 //   def GIT_CREDENTIALS_ID = 'my-git-hub'

    //sonar
    def SONAR_HOST_URL = 'http://192.168.230.158:9000'
    def SONAR_SERVER = 'sonarqube'
    def SONAR_SCANNER_TOOL = 'sonar-scanner-3.3.0'
    def SONAR_PROJECT_KEY = "${JOB_NAME}"
    def SONAR_SOURCES = 'project-examples/maven-example/multi3/src'
    
// 定义质量关卡    
file_contents = '''
{
  "files": [
    {
      "aql": {
        "items.find": {
         "repo": "maven-dev-local",
         "@test" : {"$eq" : "ok"}
        }
      },

      "target": "maven-stage-local/"
    }
  ]
}
'''    

    //docker
    //def DOCKER_IMAGE_NAME = 'java-mvn-sonar-tomcat' 
    //def DOCKER_IMAGE_TAG = 'latest'


    // docker.image('java-mvn-sonar-tomcat').inside("--name ${DOCKER_CONTAINER_NAME}"){

//        withEnv(["JAVA_HOME=${ tool 'env-docker-maven-jdk-1.8.0_191' }", "MAVEN_HOME=${tool 'env-docker-maven-maven-3.3.9'}", "PATH+MAVEN=${env.MAVEN_HOME}/bin:${env.JAVA_HOME}/bin"]) {

            //环境配置
            stage('Prepare') {

                echo "环境准备"
                rtMaven.resolver server: artiServer, 
                                 releaseRepo: RESOLVE_RELEASE_REPO,
                                 snapshotRepo: RESOLVE_SNAPSHOT_REPO
                rtMaven.deployer server: artiServer,
                                 releaseRepo: DEPLOY_RELEASE_REPO,
                                 snapshotRepo: DEPLOY_SNAPSHOT_REPO
                rtMaven.tool = 'maven'
            }

            stage('SCM') {
                git url: GIT_URL, 
                    branch: BRANCH, 
                    changelog: true
                 //   credentialsId: GIT_CREDENTIALS_ID
            }

            stage('env capture') {
                echo "收集系统变量"
                buildInfo.env.capture = true
            }
            
            /*
            //Sonar 静态代码扫描
            stage('Sonar') {
                // Sonar scan
                def scannerHome = tool SONAR_SCANNER_TOOL;
                withSonarQubeEnv(SONAR_SERVER) {
                    sh "${scannerHome}/bin/sonar-scanner -Dsonar.language=java -Dsonar.projectKey=${JOB_NAME} -Dsonar.sources=${SONAR_SOURCES} -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.java.binaries=*"
                }
            }

            sleep 5
            //添加sonar扫描结果到包上
            stage("Sonar Quality Gate") {

                //timeout(time: 1, unit: 'HOURS') {
                //timeout(time: 5, unit: 'MINUTES') {
                    // Just in case something goes wrong, pipeline will be killed after a timeout
                //    def qg = waitForQualityGate() // Reuse taskId previously collected by withSonarQubeEnv
                //    if (qg.status != 'OK') {
                //        error "Pipeline aborted due to quality gate failure: ${qg.status}"
                //    } else {
                        
                        //获取sonar扫描结果
                        def surl="${SONAR_HOST_URL}/api/measures/component?componentKey=${SONAR_PROJECT_KEY}&metricKeys=alert_status,quality_gate_details,coverage,new_coverage,bugs,new_bugs,reliability_rating,vulnerabilities,new_vulnerabilities,security_rating,sqale_rating,sqale_index,sqale_debt_ratio,new_sqale_debt_ratio,duplicated_lines_density&additionalFields=metrics,periods"
                        def response=httpRequest authentication: "sonar", consoleLogResponseBody: true, contentType: 'APPLICATION_JSON', ignoreSslErrors: true, url: surl


                        def propssonar = readJSON text: response.content
                        if (propssonar.component.measures) {
                            propssonar.component.measures.each{ measure ->
                                def val
                                if (measure.periods){
                                    val = measure.periods[0].value
                                }else {
                                    val = measure.value
                                }
                                rtMaven.deployer.addProperty("sonar.quality.${measure.metric}", val)
                            }
                 //       }

                        //增加sonar扫描结果到artifactory 
                        rtMaven.deployer.addProperty("qulity.gate.sonarUrl", SONAR_HOST_URL + "/dashboard/index/" + SONAR_PROJECT_KEY)

                        

                    }
            //    }
            }
*/
            stage('add jiraResult') {
                def requirements = getRequirementsIds();
                echo "requirements : ${requirements}"
                
                def revisionIds = getRevisionIds();
                echo "revisionIds : ${revisionIds}"
                rtMaven.deployer.addProperty("project.issues", requirements).addProperty("project.revisionIds", revisionIds)
                rtMaven.deployer.addProperty("JiraUrl", "http://jira.example.com/browse/" + requirements)
            }
            
            
            //maven 构建 
            stage('mvn build') {
                rtMaven.deployer.deployArtifacts = false
                rtMaven.run pom: POM_PATH, goals: MAVEN_GOALS, buildInfo: buildInfo
            }
            stage('package deploy to aitifactory'){
                rtMaven.deployer.deployArtifacts buildInfo
                artiServer.publishBuildInfo buildInfo
            }
    
            //进行测试
            stage('basic test') {
                echo "add test step"
            }
    

    
            stage("JUnit test") {
            //解析测试报告 
            // def reportUrl = "/root/.jenkins/workspace/" +buildInfo.name+ "/builds/" +buildInfo.number+ "/performance-reports/JUnit/TEST-artifactory.test.AppTest.xml";
            def reportUrl = "/root/.jenkins/workspace/" +buildInfo.name+ "/project-examples/maven-example/multi3/target/surefire-reports/TEST-artifactory.test.AppTest.xml";
            echo "${reportUrl}"
            sh "cat ${reportUrl}"

            def testSuite = new XmlParser().parse(reportUrl);
            def totalCases = Integer.parseInt( testSuite.attribute("tests"));
            def failures = Integer.parseInt( testSuite.attribute("failures")); 
            def errors = Integer.parseInt( testSuite.attribute("errors")); 
            def skipped = Integer.parseInt( testSuite.attribute("skipped")); 

            echo "=================== testSuite ======================"
            echo "${testSuite}"

            echo "${totalCases}====${failures}====${errors}====${skipped}"

            //计算测试结果
            def passRate = (totalCases - failures - errors - skipped) / totalCases;
            echo "=================== testResult ======================"
            echo "passRate:${passRate}";
            echo "totalCases:${totalCases}";

            //添加元数据
            def pom = readMavenPom file: 'project-examples/maven-example/multi3/pom.xml'
            def latestVersionUrl = "${ARTIFACTORY_URL}api/search/latestVersion?g=${pom.parent.groupId.replace(".","/")}&a=${pom.artifactId}&v=${pom.parent.version}&repos=${PROMOTION_SOURCE_REPO}"
            def latestVersionUrlResponse = httpRequest consoleLogResponseBody: true, 
                                                       customHeaders: [[name: 'X-JFrog-Art-Api',
                                                       value: ARTIFACTORY_API_KEY]], 
                                                       ignoreSslErrors: true, 
                                                       url: latestVersionUrl
            def warLatestVersion = latestVersionUrlResponse.content
            echo "warLatestVersion:${warLatestVersion}"
            httpRequest httpMode: 'PUT', 
                        consoleLogResponseBody: true, 
                        customHeaders: [[name: 'X-JFrog-Art-Api', value: ARTIFACTORY_API_KEY]],
                        url: "${ARTIFACTORY_URL}api/storage/${PROMOTION_SOURCE_REPO}/${pom.parent.groupId.replace(".","/")}/${pom.artifactId}/${pom.parent.version}/${pom.artifactId}-${warLatestVersion}.war?properties=passRate=${passRate};totalCases=${totalCases}"


            }
    
            /*
            stage('xray scan') {
                def xrayConfig = [
                        'buildName'  : buildInfo.name,
                        'buildNumber': buildInfo.number,
                        'failBuild'  : false
                ]
                def xrayResults = artiServer.xrayScan xrayConfig
                echo xrayResults as String
            }
            */
            
 
             // sync: 拷贝符合质量关卡的包到指定仓库
             stage('sync') {
					write_file_path = "./sync.spec"
					writeFile file: write_file_path, text: file_contents, encoding: "UTF-8"
					// read file and print it out
					fileContents = readFile file: write_file_path, encoding: "UTF-8"
					println fileContents
                    
                    sh 'jfrog rt cp --spec=sync.spec'
            }  
    /*
            //promotion操作，进行包的升级
            stage('promotion') {
                def promotionConfig = [
                        'buildName'          : buildInfo.name,
                        'buildNumber'        : buildInfo.number,
                        'targetRepo'         : PROMOTION_TARGET_REPO,
                        'comment'            : 'this is the promotion comment',
                        'sourceRepo'         : PROMOTION_SOURCE_REPO,
                        'status'             : 'Released',
                        'includeDependencies': false,
                        'failFast'           : true,
                        'copy'               : true
                ]
                artiServer.promote promotionConfig
            }
    */
            //进行部署(ansible)
            /*
            stage('deploy') {
                def pom = readMavenPom file: 'project-examples/maven-example/multi3/pom.xml'
                def latestVersionUrl = "${ARTIFACTORY_URL}api/search/latestVersion?g=${pom.parent.groupId.replace(".","/")}&a=${pom.artifactId}&v=${pom.parent.version}&repos=${PROMOTION_TARGET_REPO}"
                def latestVersionUrlResponse = httpRequest consoleLogResponseBody: true, 
                                                           customHeaders: [[name: 'X-JFrog-Art-Api',
                                                           value: ARTIFACTORY_API_KEY]], 
                                                           ignoreSslErrors: true, 
                                                           url: latestVersionUrl
                def warLatestVersion = latestVersionUrlResponse.content
                def deployCmd = "ansible 127.0.0.1 -m get_url -a 'url=${ARTIFACTORY_URL}${PROMOTION_TARGET_REPO}/${pom.parent.groupId.replace(".","/")}/${pom.artifactId}/${pom.parent.version}/${pom.artifactId}-${warLatestVersion}.war  dest=/tmp/apache-tomcat-8.5.37/webapps/demo.war force=true url_username=admin url_password=${ARTIFACTORY_API_KEY}'"
                //sh "ansible 10.1.1.1 -m shell -a '/tomcat/startup.sh' "
                echo deployCmd
                process = [ 'bash', '-c', deployCmd].execute().text
                
                commandText = "curl  -X PUT \"${ARTIFACTORY_URL}api/storage/${PROMOTION_TARGET_REPO}/${pom.parent.groupId.replace(".","/")}/${pom.artifactId}/${pom.parent.version}/${pom.artifactId}-${warLatestVersion}.war?properties=deploy.tool=ansible;deploy.env=127.0.0.1\" -uadmin:${ARTIFACTORY_API_KEY}";
                echo commandText
                [ 'bash', '-c', commandText].execute().text
                
                def stopCmd = "ansible 127.0.0.1 -m shell -a '/tmp/apache-tomcat-8.5.37/bin/shutdown.sh'"
                echo stopCmd
                sleep 5
                [ 'bash', '-c', stopCmd].execute().text
                def startCmd = "ansible 127.0.0.1 -m shell -a '/tmp/apache-tomcat-8.5.37/bin/startup.sh'"
                echo startCmd
                [ 'bash', '-c', startCmd].execute().text
                
            }
            */
    
            //进行部署
            /*
            stage('deploy') {
                def pom = readMavenPom file: 'maven-example/multi3/pom.xml'

                def latestVersionUrl = "${ARTIFACTORY_URL}api/search/latestVersion?g=${pom.parent.groupId.replace(".","/")}&a=${pom.artifactId}&v=${pom.parent.version}&repos=${PROMOTION_TARGET_REPO}"

                def latestVersionUrlResponse = httpRequest consoleLogResponseBody: true, 
                                                           customHeaders: [[name: 'X-JFrog-Art-Api',
                                                           value: ARTIFACTORY_API_KEY]], 
                                                           ignoreSslErrors: true, 
                                                           url: latestVersionUrl

                def warLatestVersion = latestVersionUrlResponse.content

                def getWarUrl = "${ARTIFACTORY_URL}${PROMOTION_TARGET_REPO}/${pom.parent.groupId.replace(".","/")}/${pom.artifactId}/${pom.parent.version}/${pom.artifactId}-${warLatestVersion}.war"
                
                httpRequest outputFile: '/tmp/apache-tomcat-8.5.37/webapps/demo.war', 
                            customHeaders: [[name: 'X-JFrog-Art-Api', value: ARTIFACTORY_API_KEY]], 
                            url: getWarUrl


                // def stopCmd = "/tmp/apache-tomcat-8.5.35/bin/shutdown.sh"
                // sh stopCmd
                //启动服务
                def stopCmd = "/tmp/apache-tomcat-8.5.37/bin/shutdown.sh"
                def startCmd = "/tmp/apache-tomcat-8.5.37/bin/startup.sh &"
                sh "ssh -p22 root@192.168.230.135 ${stopCmd}"
                sleep 5
                sh "ssh -p22 root@192.168.230.135 ${startCmd}"
                //添加元数据
                httpRequest httpMode: 'PUT', 
                            consoleLogResponseBody: true, 
                            customHeaders: [[name: 'X-JFrog-Art-Api', value: ARTIFACTORY_API_KEY]],
                            url: "${ARTIFACTORY_URL}api/storage/${PROMOTION_TARGET_REPO}/${pom.parent.groupId.replace(".","/")}/${pom.artifactId}/${pom.parent.version}/${pom.artifactId}-${warLatestVersion}.war?properties=deploy.tool=ansible;deploy.env=127.0.0.1"
                
            }
            */
            /*
            stage('regression testing') {
                sleep 3
                httpRequest url: "http://localhost:8888", 
                            consoleLogResponseBody: true
            }
            */
       // }
    // }

}

//@NonCPS
def getRequirementsIds() {
    def reqIds = "";
    final changeSets = currentBuild.changeSets
    echo 'changeset count:' + changeSets.size().toString()
    final changeSetIterator = changeSets.iterator()
    while (changeSetIterator.hasNext()) {
        final changeSet = changeSetIterator.next();
        def logEntryIterator = changeSet.iterator();
        while (logEntryIterator.hasNext()) {
            final logEntry = logEntryIterator.next()
            def patten = ~/#[\w\-_\d]+/;
	    // def patten = ~/.*/; //匹配所有
            def matcher = (logEntry.getMsg() =~ patten);
            def count = matcher.getCount();
            for (int i = 0; i < count; i++) {
                reqIds += matcher[i].replace('#', '') + ","
		// reqIds += matcher[i]  //匹配所有
            }
        }
    }
    return reqIds;
}
//@NonCPS
def getRevisionIds() {
    def reqIds = "";
    final changeSets = currentBuild.changeSets
    final changeSetIterator = changeSets.iterator()
    while (changeSetIterator.hasNext()) {
        final changeSet = changeSetIterator.next();
        def logEntryIterator = changeSet.iterator();
        while (logEntryIterator.hasNext()) {
            final logEntry = logEntryIterator.next()
            reqIds += logEntry.getRevision() + ","
        }
    }
    return reqIds
}
