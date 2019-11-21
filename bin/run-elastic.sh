#!/bin/bash

# Port 1358 is used by the DejaVu GUI client

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

source "${DIR}/env-w4s"

# Needed to work with a user-local volume
mkdir -p "${W4S_ES_VOLUME_DEST}"
chmod g+rwx "${W4S_ES_VOLUME_DEST}"
chgrp `id -g` "${W4S_ES_VOLUME_DEST}"

docker inspect ${W4S_ES_CONTAINER} > /dev/null 2>&1 || \
    docker run --rm --name ${W4S_ES_CONTAINER} \
           -d \
           --user `id -u`:`id -g` \
           -p 9200:9200 -p 9300:9300 \
           -e bootstrap.memory_lock=true \
           -e "discovery.type=single-node" \
           -e "cluster.name=${W4S_ES_CONTAINER}" \
           -e "http.port=${W4S_ES_PORT}" \
           -e "http.cors.enabled=true" \
           -e "http.cors.allow-origin=http://localhost:1358,http://127.0.0.1:1358" \
           -e "http.cors.allow-headers=X-Requested-With,X-Auth-Token,Content-Type,Content-Length,Authorization" \
           -e "http.cors.allow-credentials=true" \
           -v "${W4S_ES_VOLUME_DEST}:${W4S_ES_VOLUME_SRC}" \
           ${W4S_ES_ID}:${W4S_ES_VERSION}
