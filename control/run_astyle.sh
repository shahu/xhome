#!/bin/bash
BASEDIR=`dirname $0`
BASEDIR=`(cd "$BASEDIR"; pwd)`
astyle --options=$BASEDIR/astylerc "$BASEDIR/controller/*.java"