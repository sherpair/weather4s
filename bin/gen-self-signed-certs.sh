#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$( cd -P "$( dirname "$SOURCE" )" >/dev/null 2>&1 && pwd )"

source "${DIR}/env-w4s.secrets" || exit 1

pushd $(mktemp -d -t https.XXXXXXXX)

openssl req -x509 -newkey rsa:4096 -sha256 -days 3560 -nodes -subj "/C=UK/O=Weather4s/CN=weather4s.io" \
        -out weather4s.crt -keyout weather4s.pem -extensions san \
        -config <(echo -e '[req]\ndistinguished_name=req\n[san]\nsubjectAltName=DNS:*.weather4s.io') && \
  openssl pkcs12 -export -name weather4s -passout pass:${W4S_KEY_STORE_SECRET} \
          -in weather4s.crt -inkey weather4s.pem -out weather4s.p12 && \
    keytool -import -alias weather4s -storepass ${W4S_KEY_STORE_SECRET} -noprompt -trustcacerts \
            -file weather4s.crt -keystore weather4s.jks && \
      mkdir -p "$DIR/../shared/src/main/resources/ssl" && \
        cp -f weather4s.jks weather4s.p12 "$DIR/../shared/src/main/resources/ssl/"
popd
