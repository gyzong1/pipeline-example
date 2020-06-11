pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                    git 'https://gitee.com/gyzong1/project-examples.git'
            }
        }

        stage('build1') {
            steps {
                rtMavenResolver (
                    id: 'art11',
                    serverId: 'art1',
                    releaseRepo: 'maven-virtual',
                    snapshotRepo: 'maven-virtual'
                )  
                
                rtMavenDeployer (
                    id: 'art22',
                    serverId: 'art1',
                    releaseRepo: 'maven-dev-local',
                    snapshotRepo: 'maven-dev-local'
                )

                rtMavenRun (
                    // Tool name from Jenkins configuration.
                    tool: 'maven',
                    pom: 'maven-example/pom.xml',
                    goals: 'clean install',
                    // Maven options.
                    //opts: '-Xms1024m -Xmx4096m',
                    resolverId: 'art11',
                    deployerId: 'art22',
                    // If the build name and build number are not set here, the current job name and number will be used:
                    buildName: 'my-build-name',
                    buildNumber: '17'
                )
            }
        }
    }
}
