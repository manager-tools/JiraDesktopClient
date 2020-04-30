#!/bin/bash

KDEBASES=( ".kde" ".kde4" )
SERVICESDIR="share/kde4/services"

APPSCRIPT="openurl.sh"
SCHEMES=( "almworks-jc-http" "almworks-jc-https" )

MYDIR="$( cd "$( dirname "$0" )" && pwd )"
LAUNCHER="$MYDIR/$APPSCRIPT"

if [ ! -x "$LAUNCHER" ] ; then
  echo ==========================================================================
  echo ERROR: No $APPSCRIPT in $MYDIR
  echo ==========================================================================
  exit 1
fi

HOMEDIR="$( cd && pwd )"
KDEDIR=""
for KDEBASE in "${KDEBASES[@]}" ; do
  TRYING="$HOMEDIR/$KDEBASE"
  if [ -d "$TRYING" ] ; then
    KDEDIR=$TRYING
    break
  fi
done

if [ ! $KDEDIR ] ; then
  echo ==========================================================================
  echo ERROR: Cannot find KDE4 directory in $HOMEDIR
  echo ==========================================================================
  exit 1
fi

DIRNAME="$KDEDIR/$SERVICESDIR"
if [ ! -d "$DIRNAME" ] ; then
  echo ==========================================================================
  echo ERROR: $DIRNAME does not exist or is not a directory
  echo ==========================================================================
  exit 1
fi

for SCHEME in "${SCHEMES[@]}" ; do
  FILENAME="$DIRNAME/$SCHEME.protocol"
  echo [Protocol] > "$FILENAME"
  echo exec=\"$LAUNCHER\" \"%u\" >> "$FILENAME"
  echo protocol=$SCHEME >> "$FILENAME"
  echo input=none >> "$FILENAME"
  echo output=none >> "$FILENAME"
  echo helper=true >> "$FILENAME"
  echo reading=false >> "$FILENAME"
  echo writing=false >> "$FILENAME"
  echo deleting=false >> "$FILENAME"
  echo icon=text-html >> "$FILENAME"
  echo Description=$SCHEME >> "$FILENAME"
done
