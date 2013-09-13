#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "scripts/baseball-generator.sh <couch url> [delay] [ipaddr]" >&2
  exit 1
fi

COUCH_URL=$1
IPADDR="localhost"
DELAY=500
if [ $# -ge 2 ] ; then
  DELAY=$2
fi
if [ $# -ge 3 ] ; then
  IPADDR=$3
fi

sed "s%@@COUCH_URL@@%http://$COUCH_URL/%" SampleData/baseball.xml.template > SampleData/baseball.xml

cd SampleData
java -cp "../dist/lib/Ziggrid.jar" -Dorg.ziniki.claim.endpoint="$IPADDR" -Djava.util.logging.config.class=org.ziggrid.http.LoggingConfiguration -Dnet.spy.log.LoggerImpl=net.spy.memcached.compat.log.SunLogger org.ziggrid.generator.main.ZigGenerator org.ziggrid.generator.out.json.LoadCouchbase file:baseball.xml --delay $DELAY --server 10052
