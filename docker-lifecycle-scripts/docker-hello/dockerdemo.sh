#!/bin/bash
echo ">>> Downloading dependencies for artifactory"
curl -uadmin:password -O https://xraydemo.jfrog.io/artifactory/jcenter-cache/commons-io/commons-io/2.4/commons-io-2.4.jar
curl -uadmin:password -O https://xraydemo.jfrog.io/artifactory/jcenter-cache/uk/org/retep/xmpp/xmpp/commons/9.11/commons-9.11.jar
echo ">>> Building docker image"
docker login -u admin -p password -e stanleyf@jfrog.com http://xraydemo-docker-prod.jfrog.io/
docker build -t xraydemo-docker-prod.jfrog.io/xray/demo:$1 .
docker push xraydemo-docker-prod.jfrog.io/xray/demo:$1
echo ">>> xray/demo docker image deployed to  https://xraydemo.jfrog.io/artifactory/webapp/#/artifacts/browse/tree/General/docker-prod/xray/demo/$1"
  
