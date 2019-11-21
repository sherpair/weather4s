#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

source "${DIR}/env-w4s" || exit 1
source "${DIR}/env-w4s.secrets" || exit 1

docker inspect ${W4S_DB_CONTAINER} > /dev/null 2>&1 || \
    docker run --rm --name ${W4S_DB_CONTAINER} \
           -d \
           -p ${W4S_DB_PORT}:${W4S_DB_PORT} \
           -v "${W4S_DB_VOLUME_DEST}:${W4S_DB_VOLUME_SRC}" \
           -e POSTGRES_DB=${W4S_DB_NAME} \
           -e POSTGRES_USER=${W4S_DB_USER} \
           -e POSTGRES_PASSWORD=${W4S_DB_SECRET} \
           ${W4S_DB_ID}:${W4S_DB_VERSION}
