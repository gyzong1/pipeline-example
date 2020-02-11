node {
    def server = Artifactory.server 'art1'
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo = Artifactory.newBuildInfo()

    stage ('Clone') {
        git url: 'https://github.com/gyzong1/pipeline-example.git'
    }

    stage('env capture') {
        echo "收集系统变量"
        buildInfo.env.capture = true
    }

    // 添加元数据方式一（如jira issue ID）
    stage('Add jiraResult') {
        def requirements = getRequirementsIds();
        echo "requirements : ${requirements}" 
        def revisionIds = getRevisionIds();
        echo "revisionIds : ${revisionIds}"
        rtMaven.deployer.addProperty("project.issues", requirements).addProperty("project.revisionIds", revisionIds)
        rtMaven.deployer.addProperty("JiraUrl", "http://jira.example.com/browse/" + requirements)
    }

    stage ('Artifactory configuration') {
        rtMaven.tool = 'maven' // Tool name from Jenkins configuration
        rtMaven.deployer releaseRepo: 'guide-maven-dev-local', snapshotRepo: 'guide-maven-dev-local', server: server
        rtMaven.resolver releaseRepo: 'guide-maven-virtual', snapshotRepo: 'guide-maven-virtual', server: server
    }

    stage ('Exec Maven') {
        rtMaven.run pom: 'project-examples/maven-example/pom.xml', goals: 'clean install', buildInfo: buildInfo
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }

/*
    // 添加元数据方式二（如jira issue ID）
    stage('Add jiraResult') {
        def requirements = getRequirementsIds();
        echo "requirements : ${requirements}" 
        def revisionIds = getRevisionIds();
        echo "revisionIds : ${revisionIds}"
        sh 'curl -uadmin:password -X PUT "http://192.168.230.155:8081/artifactory/api/storage/guide-maven-dev-local/org/jfrog/test?properties=project.issues=' + requirements+ ';project.revisionIds=' + revisionIds + ';JiraUrl=http://jira.example.com/browse/' + requirements +'"'
    }
*/

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
            def matcher = (logEntry.getMsg() =~ patten);
            def count = matcher.getCount();
            for (int i = 0; i < count; i++) {
                reqIds += matcher[i].replace('#', '') + ","
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
