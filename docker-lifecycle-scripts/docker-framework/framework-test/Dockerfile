FROM docker_ip/docker-framework:latest

MAINTAINER Mark guoyunzong gyzong1@163.com

# RUN ls /home/apache-tomcat-8.5.37

COPY war/*.war /home/apache-tomcat-8.5.37/webapps/swampup.war

CMD /bin/bash -c cd /home; /home/apache-tomcat-8.5.37/bin/catalina.sh run
