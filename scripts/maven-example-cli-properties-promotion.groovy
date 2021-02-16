node {
    
    // env.NODE_HOME=tool name: 'nodejs', type: 'nodejs'
    // env.PATH="${env.NODE_HOME}/bin:${env.PATH}"
    env.M2_HOME="/usr/local/apache-maven-3.6.0"

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

    stage('Prepare') {
        sh 'jfrog rt c art1 --url=http://124.70.55.35:7082/artifactory --user=guoyunzong --password=Guoyunzong_123 --interactive=false'
        sh 'jfrog rt use art1'
    }
    stage('SCM') {
        // cleanWs()
        sh 'ls'
        //git([url: 'https://github.com/gyzong1/pipeline-example.git', branch: 'master'])
        git([url: 'https://gitee.com/gyzong1/pipeline-example.git', branch: 'master'])
        
    }
    
    stage('Build') {
        dir('project-examples/maven-example') {
          sh "jfrog rt mvnc --global=false --server-id-resolve=art1 --server-id-deploy=art1 --repo-resolve-releases=maven-virtual --repo-resolve-snapshots=maven-virtual --repo-deploy-releases=maven-dev-local --repo-deploy-snapshots=maven-dev-local"
          sh "jfrog rt mvn clean install -f pom.xml --build-name=${env.JOB_NAME} --build-number=${env.BUILD_NUMBER}"
          sh "jfrog rt bce ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Add Properties') {
        dir('project-examples/maven-example') {
          sh 'jfrog rt sp "maven-dev-local/*.jar" "a=1;test=ok"'
        }
    }
    
        
    stage('Publish') {
        dir('project-examples/maven-example') {
          sh "jfrog rt bp ${env.JOB_NAME} ${env.BUILD_NUMBER}"
        }
    }
    
    stage('Build Promotion') {
        dir('project-examples/maven-example') {
          sh "jfrog rt bpr ${env.JOB_NAME} ${env.BUILD_NUMBER} maven-pro-local --copy=true --props=key=value1"
        }
        
     // sync: 拷贝符合质量关卡的包到指定仓库
     stage('sync') {
			write_file_path = "./sync.spec"
			writeFile file: write_file_path, text: file_contents, encoding: "UTF-8"
			// read file and print it out
			fileContents = readFile file: write_file_path, encoding: "UTF-8"
			println fileContents
            
            sh 'jfrog rt cp --spec=sync.spec'
    } 
    }

}
