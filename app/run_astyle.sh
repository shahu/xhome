#!/bin/bash
BASEDIR=`dirname $0`
BASEDIR=`(cd "$BASEDIR"; pwd)`
astyle --options=$BASEDIR/astylerc "$BASEDIR/Android-Universal-Image-Loader/*.java" "$BASEDIR/XhomeCamera/*.java"