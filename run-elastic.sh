#!/bin/bash

# Port 1358 is used by the DejaVu GUI client

ES_VERSION="7.3.1"
GEO_CONTAINER="es731geo"

VOLUME="${HOME}/docker/volumes/${GEO_CONTAINER}"

mkdir -p "${VOLUME}"
chmod g+rwx "${VOLUME}"
chgrp 1000 "${VOLUME}"

docker inspect ${GEO_CONTAINER} > /dev/null 2>&1 || \
    docker run --rm --name ${GEO_CONTAINER} \
           -d \
           -p 9200:9200 -p 9300:9300 \
           -e "discovery.type=single-node" \
           -e "cluster.name=${GEO_CONTAINER}" \
           -e "http.port=9200" \
           -e "http.cors.enabled=true" \
           -e "http.cors.allow-origin=http://localhost:1358,http://127.0.0.1:1358" \
           -e "http.cors.allow-headers=X-Requested-With,X-Auth-Token,Content-Type,Content-Length,Authorization" \
           -e "http.cors.allow-credentials=true" \
           -v "${VOLUME}:/usr/share/elasticsearch/data" \
           docker.elastic.co/elasticsearch/elasticsearch:${ES_VERSION}
