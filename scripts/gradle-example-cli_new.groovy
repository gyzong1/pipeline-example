jfrog config add art1 --overwrite=true --artifactory-url=http://124.70.55.35:9082/artifactory --user=admin --password=password --interactive=false
jfrog config show
jfrog rt gradlec --server-id-resolve=art1 --server-id-deploy=art1 --repo-resolve=gradle-virtual --repo-deploy=gradle-dev-local --uses-plugin=false
jfrog rt gradle clean artifactoryPublish -b path/to/build.gradle
