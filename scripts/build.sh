#!/bin/bash

ROOTDIR="`dirname $0`"

SEP=':'
case `uname -s` in
   CYGWIN*)
     SEP=';'
     ROOTDIR=`echo $ROOTDIR | sed 's%/cygdrive/\(.\)%\1:%'`
     ;;
esac

cp ../ZinUtils/ZinUtils/qbout/ZinUtils.jar qb/libs
cp ../zinclib/dist/lib/zincinline.jar qb/libs
java -cp "$CLASSPATH$SEP$ROOTDIR/Quickbuilder.jar" com.gmmapowell.quickbuild.app.QuickBuild --no-home "$@" qb/ziggrid.qb
