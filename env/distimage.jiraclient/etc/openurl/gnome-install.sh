#!/bin/bash

GCONFTOOL="gconftool-2"
APPSCRIPT="openurl.sh"
SCHEME1="almworks-jc-http"
SCHEME2="almworks-jc-https"

if [ ! `which $GCONFTOOL` ] ; then
echo ==========================================================================
echo ERROR: Cannot find $GCONFTOOL
echo ==========================================================================
exit 1
fi

MYDIR="$( cd "$( dirname "$0" )" && pwd )"
LAUNCHER="$MYDIR/$APPSCRIPT"

if [ ! -x "$LAUNCHER" ] ; then
echo ==========================================================================
echo ERROR: No $APPSCRIPT in $MYDIR
echo ==========================================================================
exit 1
fi

OPENCMD="\"$LAUNCHER\" \"%s\""

$GCONFTOOL -s /desktop/gnome/url-handlers/${SCHEME1}/command "$OPENCMD" --type String
$GCONFTOOL -s /desktop/gnome/url-handlers/${SCHEME1}/enabled --type Boolean true
$GCONFTOOL -s /desktop/gnome/url-handlers/${SCHEME2}/command "$OPENCMD" --type String
$GCONFTOOL -s /desktop/gnome/url-handlers/${SCHEME2}/enabled --type Boolean true
