# FROM docker_ip/docker-framework:latest

# MAINTAINER guoyunzong gyzong1@163.com

# ADD war/*.war /home/apache-tomcat-8.5.37/webapps/swampup.war

# CMD /bin/bash -c cd /home; /home/apache-tomcat-8.5.37/bin/catalina.sh run

FROM centos:latest

MAINTAINER guoyunzong gyzong1@163.com

# RUN /bin/bash -c cd ~; cd /home ; mkdir -p exec
ADD jdk/jdk-8-linux-x64.tar.gz /home
CMD /bin/bash -c tar zxf /home/jdk-8-linux-x64.tar.gz -C /home
# RUN /bin/tar zxf /home/jdk-8u201-linux-x64.tar.gz -C /home
# RUN /bin/bash -c cd /home/exec; mv /home/exec/jdk1* /home/exec/jdk8
ENV JAVA_HOME=/home/jdk1.8.0_201
RUN sed "/securerandom.source=/{s/file:\/dev\/random/file:\/dev\/urandom/}" /home/jdk1.8.0_201/jre/lib/security/java.security -i

ADD  tomcat/apache-tomcat-8.tar.gz /home
# RUN /bin/tar zxf /home/apache-tomcat-8.5.37.tar.gz -C /home
CMD /bin/bash -c tar zxf /home/apache-tomcat-8.tar.gz -C /home
# RUN /bin/bash -c cd /home/exec; mv /home/exec/apache-tomcat-8.* /home/exec/tomcat
ADD tomcat/server.xml /home/apache-tomcat-8.5.37/conf
ENV CATALINA_HOME=/home/apache-tomcat-8.5.37

# CMD /bin/bash -c cd /home/exec; /home/exec/apache-tomcat-8.5.37/bin/catalina.sh run
CMD /home/apache-tomcat-8.5.37/bin/catalina.sh run
# RUN /home/apache-tomcat-8.5.37/bin/startup.sh

ADD war/*.war /home/apache-tomcat-8.5.37/webapps/swampup.war

CMD /bin/bash -c cd /home; /home/apache-tomcat-8.5.37/bin/catalina.sh run
