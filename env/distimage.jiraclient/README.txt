Client for Jira Readme
Document version: 3.8.5 (26 April 2020)
http://almworks.com/jiraclient
https://bitbucket.org/almworks/jiraclient/src/cloud/


Contents
========
* System Requirements
* Installation
* Upgrading
* Uninstall
* Backup and Restore
* Trademarks and Copyright


System Requirements
===================

1. Server:
   Atlassian Jira version: 5.0-7.9
   Atlassian Service Desk version: 3.0-3.12
   Atlassian Jira Cloud
   Atlassian Service Desk Cloud

  I18N: Jira servers with localized interfaces are not yet supported
  (Client for Jira may work anyway, please try and let us know if there are
  problems);

2. Additional server authentication: Basic HTTP, Digest, NTLM version 1 or 2; 

3. Operating System: Microsoft Windows, Linux, Mac OS X, or any other supported
   by Java;

4. System Memory: 256MB required, 512MB recommended;

5. Hard Drive Space: 200MB required, 300MB recommended;

6. Screen Resolution: 1024x768 or better;

7. Displays: multiple displays are supported, but Client for Jira should be
   restarted when a display is connected or disconnected;

8. Network connection: Atlassian Jira produces a lot of traffic when
   answering Client for Jira requests, so performance over slow network
   connection may be degraded. (Hint: turn on "Use Gzip Compression" 
   setting in Jira configuration to reduce traffic.)

9. Java: In case you downloaded a distribution without Java bundled in, you
   will need Java SE 8 or later. We support only Java from Oracle; other
   Java implementations, including OpenJDK, are not supported.


Installation - Windows
======================

To install the application, run downloaded executable file and follow 
instructions. If having problems installing on Windows 7/8/10, use
"Run as Administrator" to start the installer.


Installation - Linux
====================

To install the application, unpack downloaded archive. Run 
  bin/jiraclient.sh 
to start Client for Jira.

Client for Jira will create ".JIRAClient" subdirectory in your home
directory. Make sure the home is writable.


Installation - Mac OS X
=======================

Supported versions:

- Mac OS X 10.5 (Leopard): Mac OS X 10.5.2 and above with
Java for Mac OS X 10.5 Update 1 and above (64-bit Intel-based Macs);

- OS X 10.8 (Mountain Lion);

- OS X 10.9 (Mavericks);

- OS X 10.10 (Yosemite);

- OS X 10.11 (El Capitan);

- macOS 10.12 (Sierra);

- macOS 10.13.4+ (High Sierra).

To install the application, copy it from the dmg image to the
Applications folder. Client for Jira will create ".JIRAClient"
subdirectory in your home directory. Make sure it is writable.


Upgrading
=========

1. Stop Client for Jira if it is running;

2. Back up your workspace; 
   See http://wiki.almworks.com/display/jc16/How+to+Backup+Your+Workspace

3. Either unpack the new version on top of the old version, or run the
   new installer.


Uninstall
=========

To uninstall Client for Jira on Windows, run uninstaller from
Control Panel - Add/Remove Programs.

To uninstall Client for Jira on other operating systems, just delete Client for
Jira home directory.

NOTE: Client for Jira workspace (queries, local database, etc.) usually
resides in a separate directory. Uninstallation should not affect it.


Backup and Restore
==================

See also: http://wiki.almworks.com/display/jc16/How+to+Backup+Your+Workspace

All user information is contained within workspace directory. By default,
it is named ".JIRAClient" and it resides in the user's home directory.
(Something like /home/username/.JIRAClient on Unix or
 C:\Users\username\.JIRAClient on Windows.)

To back it up, just copy all of its contents to a directory of your choice.

To restore from backed up workspace, delete or move current workspace and
copy back the workspace that you have saved earlier. 

NOTE: You must shut down Client for Jira to perform backup and restore.


Trademarks and Copyright
========================

Atlassian and Jira are trademarks of Atlassian Software Systems Pty Ltd.

Java is a registered trademark of Oracle and/or its affiliates.

Microsoft, Windows are trademarks of Microsoft Corporation.

Apple, Mac, Mac OS, and OS X are trademarks of Apple Inc.

All other trademarks mentioned in ALM Works products, documentation and 
other files are the property of their respective owners.

See license/ sub-directories for licenses of various open-source products
delivered with this product.

Deskzilla and Client for Jira are copyright (C) ALM Works Ltd 2004-2020

All files included in this software distribution package are subject to 
copyright by ALM Works Ltd and its licensors.

