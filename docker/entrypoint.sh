#!/bin/sh
if [ -f /certs/public.crt ]; then
  CACERTS="$JAVA_HOME/lib/security/cacerts"
  TRUSTSTORE="/tmp/truststore.jks"
  cp "$CACERTS" "$TRUSTSTORE"
  keytool -importcert -noprompt -keystore "$TRUSTSTORE" \
    -storepass changeit -alias minio-local -file /certs/public.crt
  export JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=${TRUSTSTORE}"
fi

exec java ${JAVA_OPTS} -jar frict.jar
