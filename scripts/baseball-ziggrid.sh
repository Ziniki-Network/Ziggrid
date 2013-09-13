#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "scripts/baseball-ziggrid.sh <couch url> [ipaddr]" >&2
  exit 1
fi

COUCH_URL="http://$1/"
IPADDR=localhost
if [ $# -ge 2 ] ; then
  IPADDR="$2"
fi

java -cp "dist/lib/Ziggrid.jar" -Dorg.ziniki.claim.endpoint="$IPADDR" -Djava.util.logging.config.class=org.ziggrid.http.LoggingConfiguration -Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.SunLogger org.ziggrid.driver.Ziggrid "$COUCH_URL" SampleData/buckets --connector org.ziggrid.driver.RawTapMessageSource --connector org.ziggrid.driver.WebServer BaseballDemo/ 10051
