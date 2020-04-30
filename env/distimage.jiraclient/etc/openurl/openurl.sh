#/bin/bash

APPSCRIPT=jiraclient.sh

MYDIR="$( cd "$( dirname "$0" )" && pwd )"
LAUNCHER="$MYDIR/../../bin/$APPSCRIPT"

if [ ! -x "$LAUNCHER" ] ; then
echo ==========================================================================
echo ERROR: No $APPSCRIPT in ../../bin/
echo ==========================================================================
exit 1
fi

"$LAUNCHER" -J-Dno.splash=true --open=$1

