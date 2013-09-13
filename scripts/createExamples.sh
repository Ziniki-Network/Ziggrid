#!/bin/bash -x

if [ ! -d dist/examples ] ; then
  mkdir -p dist/examples
fi

cd SampleData/buckets
zip -r ../../dist/examples/baseball.zip ziggrid-baseball
exit $?
