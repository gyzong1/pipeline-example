node {
    def server = Artifactory.server 'art'
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo

    stage ('Clone') {
        // git url: 'https://github.com/JFrog/project-examples.git'
        git url: 'https://gitee.com/gyzong1/project-examples.git'
    }
    
    stage ('Artifactory configuration') {
        rtMaven.tool = 'maven' // Tool name from Jenkins configuration
        rtMaven.deployer releaseRepo: 'maven-dev-local', snapshotRepo: 'maven-dev-local', server: server
        rtMaven.resolver releaseRepo: 'maven-virtual', snapshotRepo: 'maven-virtual', server: server
        buildInfo = Artifactory.newBuildInfo()
    }

    stage ('Exec Maven') {
        rtMaven.run pom: 'maven-example/pom.xml', goals: 'clean install', buildInfo: buildInfo
    }

    stage ('Upload File') {
        def uploadSpec = """{
         "files": [
            {
                "pattern": "/root/upload.zip",
                "target": "maven-dev-local/zip/"
            }
         ]
        }"""
        server.upload spec: uploadSpec, buildInfo: buildInfo
    }
    
    stage ('Download File') {
        def downloadSpec = """{
         "files": [
            {
                "pattern": "gradle-ali-remote-cache/org/codehaus/plexus/plexus-utils/1.5.1/plexus-utils-1.5.1.jar",
                "target": "/root/"
            }
         ]
        }"""
        server.download spec: downloadSpec, buildInfo: buildInfo
    }

    stage('Env capture') {
        echo "收集系统变量"
        buildInfo.env.capture = true
    }
    
    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }
    
    stage ('Xray scan') {
        // curl -u admin:password -X POST http://192.168.230.138:8000/api/v1/scanBuild
        def scanConfig = [
               // 'artifactoryId'  : art1,
                'buildName'      : buildInfo.name,
                'buildNumber'    : buildInfo.number,
                'failBuild'      : false
            ]
        def scanResult = server.xrayScan scanConfig
        echo scanResult as String
    }
    
}
