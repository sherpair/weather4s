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
source "${DIR}/env-w4s-ssl" || exit 1

# Needed to work with a user-local volume
mkdir -p "${W4S_ES_VOLUME_DEST}"
chmod g+rwx "${W4S_ES_VOLUME_DEST}"
chgrp `id -g` "${W4S_ES_VOLUME_DEST}"

docker-compose -f "${DIR}/docker-compose.yml" up
