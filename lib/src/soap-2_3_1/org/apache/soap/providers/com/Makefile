#Make file for COMProvider
JNIHeader=org_apache_soap_providers_com_RPCProvider.h
COMProviderPackage=org.apache.soap.providers.com

OUTDIR=

INTDIR=
!ifdef ANTBUILD
!message Ant build home dir is: $(ANTBUILDTARGET)
!message Ant build classpath is $(ANTBUILDCLASSPATH)  
MAKECLASSPATH=$(ANTBUILDCLASSPATH:/=\)
TARGETDIR=$(ANTBUILDTARGET:/=\)
ANTTARGETDIR=$(TARGETDIR)
TARGETS=$(ANTTARGETDIR)\COMProvider.dll
#handle ANTBUIILD debug indicator
!UNDEF DEBUG
!IF "$(ANTBUILDDEBUG)" == "righton" 
DEBUG=1
!ENDIF
!ELSE
TARGETDIR=.
ANTTARGETDIR=BOGUSSTRINGHERE
TARGETS=RPCProvider.class COMProvider.dll
MAKECLASSPATH=$(CLASSPATH)
!ENDIF

!ifdef DEBUG
!MESSAGE DEBUG BUILD!!!  DEBUG BUILD!!!  DEBUG BUILD!!!  DEBUG BUILD!!!  DEBUG BUILD!!!  DEBUG BUILD!!!  DEBUG BUILD!!!  DEBUG BUILD!!!
CPP_OPTIONS=/MDd /Gm /Zi /Od /D /YX   /D _DEBUG /D DEBUG
RSC_OPIONS=/d _DEBUG /d DEBUG
LINK_OPTIONS=/debug /pdbtype:sept /INCREMENTAL:YES
LINK_LIBS= MSVCRTD.LIB

JDEBUG= -g
!else
CPP_OPTIONS=/MD /O2 /D NDEBUG
RSC_OPIONS=/d NDEBUG
LINK_OPTIONS= /RELEASE /INCREMENTAL:NO
LINK_LIBS= MSVCRT.LIB

JDEBUG= 
!endif
JNI_INCLUDE=/I$(JAVA_HOME)/include /I$(JAVA_HOME)/include/win32
CPP_OPTIONS= $(CPP_OPTIONS) /D _UNICODE /D UNICODE

CPP=cl.exe /c
# /D WIN32_LEAN_AND_MEAN /D VC_EXTRALEAN #Leaves out COM stuff
CPP_PROJ=/nologo /W3 /GX /D_WIN32_WINNT=0x400 /DWINVER=0x400\
 $(CPP_OPTIONS)    /D _WINDOWS /D _WINDLL\
 /D _AFXDLL /D _USRDLL /I . /Fo$(INTDIR) /Fd$(INTDIR) /FD 

LINK32=cl.exe

# /NODEFAULTLIB\/IMPLIB:NUL 

LINK32_FLAGS= /link /nologo /subsystem:windows /dll \
 /pdb:$(OUTDIR)COMProvider.pdb /machine:I386 \
  $(LINK_OPTIONS) $(LINK_LIBS) 

LINK_LIBS=  $(LINK_LIBS)\
 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib \
 shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib 

LINK32_OBJS= \
	$(INTDIR)RPCProvider.obj 

all : $(TARGETS) 

COMProvider.dll :  \
		$(LINK32_OBJS) 
 $(LINK32) $(LINK32_OBJS) $(LINK32_FLAGS) /out:COMProvider.dll
!if 0 
 if exist $(TARGETDIR)\COMProvider.dll copy  $(TARGETDIR)\COMProvider.dll $(TARGETDIR)\COMProvider_g.dll
#  !else
 if exist $(TARGETDIR)\COMProvider.exp del $(TARGETDIR)\COMProvider.exp
 if exist $(TARGETDIR)\COMProvider.lib del $(TARGETDIR)\COMProvider.lib
 if exist $(TARGETDIR)\COMProvider.ilk del $(TARGETDIR)\COMProvider.ilk
!endif

$(ANTTARGETDIR)\COMProvider.dll :  COMProvider.dll 
 if exist COMProvider.dll copy  COMProvider.dll $(ANTTARGETDIR)\COMProvider.dll
!ifdef DEBUG 
 if exist COMProvider.dll copy  COMProvider.dll $(ANTTARGETDIR)\COMProvider_g.dll
!endif


$(INTDIR)RPCProvider.obj : RPCProvider.cpp \
		$(JNIHeader) 
	$(CPP) $(CPP_PROJ) $(JNI_INCLUDE) RPCProvider.cpp

$(JNIHeader) : RPCProvider.java
	javah -classpath $(MAKECLASSPATH) \
		-jni $(COMProviderPackage).RPCProvider

RPCProvider.class : RPCProvider.java
  javac $(JDEBUG) $*.java

clean :
    if exist $(ANTTARGETDIR)\COMProvider.dll del $(ANTTARGETDIR)\COMProvider.dll
    if exist $(ANTTARGETDIR)\COMProvider_g.dll del $(ANTTARGETDIR)\COMProvider_g.dll
    if exist $(JNIHeader) erase $(JNIHeader)
    -for %x in (*.ilk) do erase %x
    -for %x in (*.pdb) do erase %x
    -for %x in (*.class) do erase %x
    -for %x in (*.idb) do erase %x
    -for %x in (*.obj) do erase %x
#   -for %x in (*.dll) do erase %x
     

#ENDOF MAKEFILE
