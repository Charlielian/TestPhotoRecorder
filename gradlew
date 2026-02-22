#!/usr/bin/env sh

if [ -z "$JAVA_HOME" ] ; then
  if [ -n "$JAVA_HOME" ] ; then
    export JAVA_HOME
  fi
fi

if [ -z "$JAVA_HOME" ] ; then
  echo "ERROR: JAVA_HOME is not set"
  exit 1
fi

exec "$JAVA_HOME/bin/java" -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"