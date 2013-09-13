#!/bin/bash

ROOTDIR="`dirname $0`"

SEP=':'
case `uname -s` in
   CYGWIN*)
     SEP=';'
     ROOTDIR=`echo $ROOTDIR | sed 's%/cygdrive/\(.\)%\1:%'`
     ;;
esac

java -cp "$CLASSPATH$SEP$ROOTDIR/Quickbuilder.jar" com.gmmapowell.quickbuild.app.QuickBuild "$@" qb/ziggrid.qb
