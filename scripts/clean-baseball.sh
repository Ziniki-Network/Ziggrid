#!/bin/bash
if [ $# -lt 3 ] ; then
  echo "Usage: clean-baseball.sh <couch url> <admin user> <admin password> [<ramMB> <#replicas>]" >&2
  exit 1
fi

RAM=1301
REPLICAS=1

if [ $# -ge 4 ] ; then
  RAM=$4
fi

if [ $# -ge 5 ] ; then
  REPLICAS=$5
fi

curl -u ${2}:${3} -X DELETE http://${1}/pools/default/buckets/ziggrid-baseball
curl -u ${2}:${3} -X POST http://${1}/pools/default/buckets -d name=ziggrid-baseball -d authType=sasl -d ramQuotaMB=$RAM -d replicaNumber=$REPLICAS
