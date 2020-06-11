pipeline {
    agent any

    stages {
        stage('SCM') {
            steps {
                git 'https://gitee.com/gyzong1/project-examples.git'
            }
        }

        stage('build') {
            steps {
                rtMavenResolver (
                    id: 'resolve-arti',
                    serverId: 'art1',
                    releaseRepo: 'maven-virtual',
                    snapshotRepo: 'maven-virtual'
                )  
                
                rtMavenDeployer (
                    id: 'deploy-arti',
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
                    resolverId: 'resolve-arti',
                    deployerId: 'deploy-arti',
                    // If the build name and build number are not set here, the current job name and number will be used:
                    buildName: 'my-build-name',
                    buildNumber: '17'
                )
            }
        }
    }
}
