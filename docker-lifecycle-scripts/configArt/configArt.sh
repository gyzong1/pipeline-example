curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.RemoteRepositoryConfiguration+json" -X PUT "$DOCKER_ART_URL/api/repositories/swampup" -T swampup.json
curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.RemoteRepositoryConfiguration+json" -X PUT "$DOCKER_ART_URL/api/repositories/apache-archive" -T apache-archive.json
curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.RemoteRepositoryConfiguration+json" -X POST "$DOCKER_ART_URL/api/repositories/jcenter" -T jcenter.json
curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.RemoteRepositoryConfiguration+json" -X POST "$DOCKER_ART_URL/api/repositories/docker-remote" -T docker-remote.json
curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.LocalRepositoryConfiguration+json" -X PUT "$DOCKER_ART_URL/api/repositories/tomcat-local" -T tomcat-local.json
curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.LocalRepositoryConfiguration+json" -X PUT "$DOCKER_ART_URL/api/repositories/libs-dev-local" -T libs-dev-local.json
curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.VirtualRepositoryConfiguration+json" -X PUT "$DOCKER_ART_URL/api/repositories/libs-dev" -T libs-dev.json
curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.VirtualRepositoryConfiguration+json" -X PUT "$DOCKER_ART_URL/api/repositories/docker-prod" -T docker-prod.json
curl -uadmin:password -H "content-type: application/vnd.org.jfrog.artifactory.repositories.VirtualRepositoryConfiguration+json" -X PUT "$DOCKER_ART_URL/api/repositories/docker-dev" -T docker-dev.json
curl -uadmin:password -X DELETE "$DOCKER_ART_URL/api/repositories/docker-virtual"

./warmup.sh
