node {
    def server = Artifactory.newServer url: 'http://192.168.230.155:8081/artifactory', username: 'admin', password: 'password'
    // or def server = Artifactory.server 'art1'
    def rtMaven = Artifactory.newMavenBuild()
 //   def buildInfo
    def buildInfo = Artifactory.newBuildInfo()

    stage ('Clone') {
        git url: 'https://github.com/gyzong1/pipeline-example.git'
    }

    stage ('Artifactory configuration') {
        rtMaven.tool = 'maven' // Tool name from Jenkins configuration
        rtMaven.deployer releaseRepo: 'maven-dev-local', snapshotRepo: 'maven-dev-local', server: server
        rtMaven.resolver releaseRepo: 'maven-virtual', snapshotRepo: 'maven-virtual', server: server
        buildInfo = Artifactory.newBuildInfo()
    }

    stage ('Exec Maven') {
        rtMaven.run pom: 'project-examples/maven-example/pom.xml', goals: 'clean install', buildInfo: buildInfo
    }
    
    stage('Env capture') {
        echo "收集系统变量"
            buildInfo.env.capture = true
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }

    /*
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
    */
    
}
