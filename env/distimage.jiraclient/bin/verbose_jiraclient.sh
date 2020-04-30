#!/bin/bash

# -----------------Configurable Section------------------------------
# Java options here, you can add more options if needed.
# See http://kb.almworks.com/wiki/Deskzilla_Command_Line_Options

JAVA_OPTIONS="-Xmx600m -Djiraclient.debug=true -Djira.dump=all -Dbugzilla.dump=all -Ddebug.httpclient=true $ALMWORKS_DEBUG_OPTIONS"

# -----------------End of Configurable Secion------------------------
SHELL=/bin/bash
export JAVA_OPTIONS
$SHELL "`dirname \"$0\"`"/launch.sh $*
exit $?
