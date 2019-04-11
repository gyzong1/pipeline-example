cd docker-framework/jdk
jfrog rt dl jswampup-remote/java/jdk-8u91-linux-x64.tar.gz
mv jdk-8u* jdk-8-linux-x64.tar.gz
cd ../tomcat
export TOMCAT_VERSION=$(curl -uadmin:password "$DOCKER_ART_URL/api/search/latestVersion?g=org.apache&a=apache-tomcat&repos=tomcat-local")
jfrog rt dl "tomcat-local/org/apache/apache-tomcat/apache-tomcat-$TOMCAT_VERSION.tar.gz"
mv "apache-tomcat-$TOMCAT_VERSION.tar.gz" apache-tomcat-8.tar.gz
