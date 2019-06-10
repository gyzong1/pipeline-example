manager.listener.logger.println("-----------------------Collecting Junit pass rate-------------------------");
def buildName = manager.envVars['JOB_NAME'];
def buildId = manager.envVars['BUILD_ID'];


manager.listener.logger.println(buildName);

//获取版本号
manager.listener.logger.println(manager.envVars );

manager.envVars['RELEASE_VERSION'] = manager.envVars['POM_VERSION'];
def releaseVersion = manager.envVars['RELEASE_VERSION'] ;
def warVersion = releaseVersion;
manager.listener.logger.println(manager.envVars['RELEASE_VERSION'] );


if(releaseVersion.contains("SNAPSHOT")){
     def curlstr="curl -uadmin:password \'http://192.168.230.155:8081/artifactory/"
     def warverstr= curlstr +  "api/search/latestVersion?g=org.jfrog.test&a=multi3&repos=maven-dev-local&v=${releaseVersion}\'";
     //sh "${warverstr} >  warVersion.txt 2>/dev/null"
     //["bash","-c", "touch warVersion.txt"].execute()
     warVersion = ["bash","-c",warverstr].execute().text
     //warVersion=readFile('warVersion.txt')
    manager.listener.logger.println("warverstr:" + warverstr );
    manager.listener.logger.println("warVersion:" + warVersion );
} 

/*
//解析测试报告
// def reportUrl = "/root/.jenkins/workspace/" +buildName+ "/builds/" +buildId+ "/performance-reports/JUnit/TEST-artifactory.test.AppTest.xml";
def reportUrl = "/root/.jenkins/workspace/" +buildName+ "/maven-example/multi1/target/surefire-reports/TEST-artifactory.test.AppTest.xml";

def testSuite = new XmlParser().parse(reportUrl);

def totalCases = Integer.parseInt( testSuite.attribute("tests"));
def failures = Integer.parseInt( testSuite.attribute("failures")); 
def errors = Integer.parseInt( testSuite.attribute("errors")); 
def skipped = Integer.parseInt( testSuite.attribute("skipped")); 

//计算测试结果
def passRate = (totalCases - failures - errors - skipped) / totalCases;

manager.envVars['PASS_RATE'] = String.valueOf(passRate);

manager.listener.logger.println("PASS_RATE is : " + manager.envVars['PASS_RATE'] );
*/


manager.listener.logger.println("-----------------------获取 需求ID------------------------");
def jiraIds = "";
final changeSet = manager.build.getChangeSet()
final changeSetIterator = changeSet.iterator()
while (changeSetIterator.hasNext()) {
  final gitChangeSet = changeSetIterator.next()
  def a = gitChangeSet.getMsg()
  def patten = ~/[\w\-_\d]+/;
  def matcher = (gitChangeSet.getMsg() =~ patten);
  if(matcher.getCount() > 0){
     jiraIds += matcher[0] + ","
  }
}
manager.listener.logger.println("Jira Ids : " + jiraIds);
manager.listener.logger.println("-----------------------获取 需求ID结束------------------------");

manager.listener.logger.println("-----------------------添加需求ID作为元数据--------------------");
 def jiraCommandText = "curl  -X PUT \"http://192.168.230.155:8081/artifactory/api/storage/maven-dev-local/org/jfrog/test/multi3/"+releaseVersion+"/multi3-"+warVersion+".war?properties=requirements.ids="+jiraIds+"\" -uadmin:password";
 manager.listener.logger.println("jiraCommandText: " + jiraCommandText);
 process = [ 'bash', '-c', jiraCommandText].execute().text

manager.listener.logger.println("-----------------------获取 Sonar 测试结果--------------------");
//获取 Sonar 测试结果
// def getSonarIssuesCmd = "curl  GET -v http://demo.jfrogchina.com:9000/api/issues/search?componentRoots=org.jfrog.test:multi3";
def getSonarIssuesCmd = "curl  GET -v http://192.168.230.158:9000/api/issues/search?componentRoots=my:maven-project-test";
process = [ 'bash', '-c', getSonarIssuesCmd].execute().text

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def jsonSlurper = new JsonSlurper()
def issueMap = jsonSlurper.parseText(process);
manager.listener.logger.println(issueMap);


//增加Sonar 代码扫描结果作为元数据
//def url1 = "http://192.168.230.132:9000/dashboard?id=my%3Aproject"
// commandText = "curl  -X PUT \"http://demo.jfrogchina.com/artifactory/api/storage/maven-stage-local/org/jfrog/test/multi3/"+releaseVersion+"/multi3-"+releaseVersion+".war?properties=qulity.gate.sonarUrl=http://47.93.114.82:9000/dashboard/index/jfrog:multi3;qulity.gate.sonarIssue="+issueMap.total+"\" -uadmin:AKCp2WXCWmSmLjLc5VKVYuSeumtarKV7TioZfboRAEwC1tqKAUvbniFJqp7xLfCyvJ7GxWuJZ";
commandText = "curl  -X PUT \"http://192.168.230.155:8081/artifactory/api/storage/maven-dev-local/org/jfrog/test/multi3/"+releaseVersion+"/multi3-"+warVersion+".war?properties=qulity.gate.sonarUrl=http://192.168.230.156:9000/dashboard;qulity.gate.sonarIssue="+issueMap.total+"\" -uadmin:password";
process = [ 'bash', '-c', commandText].execute().text
manager.listener.logger.println(commandText);



