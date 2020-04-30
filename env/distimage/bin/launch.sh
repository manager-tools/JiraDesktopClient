# Application launcher - do not call manually.
##############################################

I4JSCRIPT="true"
PROG_OPTIONS=""
for ARG in $* ; do
  if [ "${ARG:0:2}" = "-J" ] ; then JAVA_OPTIONS="$JAVA_OPTIONS ${ARG:2}"
  else PROG_OPTIONS="$PROG_OPTIONS $ARG" ; fi
done

ME=$0
while [ -L "$ME" ]; do
  _dir="`dirname \"$ME\"`"
  _link="`readlink \"$ME\"`"
  if [ "x$?" != "x0" ]; then break; fi
  if [ "x${_link}" = "x" ]; then break; fi
  if [ "x${_link:0:1}" != "x/" ]; then
    ME="${_dir}/${_link}"
  else
    ME="${_link}"
  fi
done
MYDIR="`dirname \"$ME\"`"

PROGRAM_JAR=jiraclient.jar
PROGRAM_NAME="Client for Jira"
LAUNCHER=launch.sh
LAUNCH="$MYDIR/$LAUNCHER"
ROOT_LAUNCHER="$MYDIR/../jiraclient"
SHELL=/bin/bash

if [ "x$I4JSCRIPT" != "xfalse" ]; then
  if [ -x "$ROOT_LAUNCHER" ]; then
    INSTALL4J_ADD_VM_PARAMS=$JAVA_OPTIONS
    export INSTALL4J_ADD_VM_PARAMS
    "$ROOT_LAUNCHER" $PROG_OPTIONS
    exit $?
  fi
fi

if [ ! -f "$LAUNCH" ] ; then
echo ==========================================================================
echo ERROR: Cannot start $PROGRAM_NAME
echo Cannot find $LAUNCHER in $MYDIR
echo ==========================================================================
exit 1
fi

X_ALMWORKS_LAUNCH_PERMIT=true

if [ "$X_ALMWORKS_LAUNCH_PERMIT" != "true" ]; then
echo ==========================================================================
echo ERROR: `basename "$0"` should not be called manually. 
echo Please start application with other .sh files.
echo ==========================================================================
exit 1
fi

if [ "x$PROGRAM_NAME" = "x" -o "x$PROGRAM_JAR" = "x" ]; then
echo ==========================================================================
echo ERROR: Bad call to `basename "$0"`
echo [$PROGRAM_JAR] [$PROGRAM_NAME]
echo ==========================================================================
exit 1
fi

APPBIN="`dirname \"$0\"`"
APPHOME="$APPBIN/.."
JAVA_EXE=java
JAVA=$JAVA_EXE

if [ ! -f "$APPHOME/$PROGRAM_JAR" ]; then
echo ==========================================================================
echo ERROR: Cannot start $PROGRAM_NAME
echo Cannot find $PROGRAM_JAR in $APPHOME
echo ==========================================================================
exit 1
fi

if [ "x$JAVA_HOME" != "x" ]; then
JAVA="$JAVA_HOME/bin/$JAVA_EXE"
if [ ! -f "$JAVA" ]; then JAVA="$JAVA_HOME/jre/bin/$JAVA_EXE"; fi
if [ ! -f "$JAVA" ]; then JAVA="$APPHOME/jre/bin/$JAVA_EXE"; fi
if [ ! -f "$JAVA" ]; then JAVA=$JAVA_EXE; fi
fi

"$JAVA" "$OS_JAVA_OPT" $JAVA_OPTIONS -jar "$APPHOME/$PROGRAM_JAR" $PROG_OPTIONS
