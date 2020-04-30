#!/bin/bash

# -----------------Configurable Section------------------------------
# Java options here, you can add more options if needed.
# See http://kb.almworks.com/wiki/Deskzilla_Command_Line_Options
# You may add JVM options by prepending them with "-J": jiraclient.sh -J-Dopt=value
# This script expects Oracle Java8 on the PATH or JAVA_HOME

JAVA_OPTIONS="-Xmx600m"

# -----------------End of Configurable Secion------------------------
OS_JAVA_OPT="-Xdock:name=Client for Jira"
SHELL=/bin/bash
export JAVA_OPTIONS OS_JAVA_OPT
$SHELL "`dirname \"$0\"`"/launch.sh $*
exit $?
