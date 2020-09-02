node {
    def server = Artifactory.server 'art1'
    def rtMaven = Artifactory.newMavenBuild()
    def buildInfo
    def PROMOTION_SOURCE_REPO = 'guide-maven-dev-local'
    def PROMOTION_TARGET_REPO = 'guide-maven-pro-local'

    stage ('Clone') {
        git url: 'https://gitee.com/gyzong1/project-examples.git'
    }

    stage ('Artifactory configuration') {
        rtMaven.tool = 'maven' // Tool name from Jenkins configuration
        rtMaven.deployer releaseRepo: 'guide-maven-dev-local', snapshotRepo: 'guide-maven-dev-local', server: server
        rtMaven.resolver releaseRepo: 'guide-maven-virtual', snapshotRepo: 'guide-maven-virtual', server: server
        buildInfo = Artifactory.newBuildInfo()
    }

    stage('Env capture') {
        echo "收集系统变量"
        buildInfo.env.capture = true
    }

    stage ('Exec Maven') {
        rtMaven.run pom: 'maven-example/pom.xml', goals: 'clean install', buildInfo: buildInfo
    }

    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }
    
    /*def id = 1
    stage('Example') {
        if (id > 2) {
            echo 'id is ok'
        } else {
            echo 'id is not ok'
        }
    }
    */
    
    stage('Approve'){
        input message: 'Comfirm deployment of production environment ?', ok: 'Yes'
        echo 'updata jirastatus'
    }
    
    //promotion操作，进行包的升级
    stage('Promotion') {
        def promotionConfig = [
                'buildName'          : buildInfo.name,
                'buildNumber'        : buildInfo.number,
                'targetRepo'         : PROMOTION_TARGET_REPO,
                'comment'            : 'this is the promotion comment',
                'sourceRepo'         : PROMOTION_SOURCE_REPO,
                'status'             : 'Released',
                'includeDependencies': false,
                'failFast'           : true,
                'copy'               : true,
        ]
        server.promote promotionConfig
    }
}
