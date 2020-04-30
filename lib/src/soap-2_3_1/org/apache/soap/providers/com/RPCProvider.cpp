/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "SOAP" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 2001, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
#pragma warning(disable:4786)  /*Very annoying warning that symbol exceeds debug info size*/
#include <cstdlib>
#include <sstream>

#include <string.h>
#include <iostream>
#include <deque>
#include <string>
#include <cassert>
#include <new>
#include <new.h>
#include <algorithm>

#include <jni.h>
#include <process.h>
#include <windows.h>
#include "RPCProvider.h"

using namespace std;



HRESULT   variant2object (JNIEnv *jenv, const VARIANT &var,  jobject &ret, bool *localRefCreated= NULL);

wchar_t ModuleName[MAX_PATH *2 ]={0};
HANDLE thisModule;
BOOL WINAPI DllMain(HINSTANCE hinstDll, DWORD fdwReason, LPVOID fImpLoad)
{
  if(fdwReason == DLL_PROCESS_ATTACH)
  { 
    thisModule= hinstDll;
    GetModuleFileName( hinstDll,  ModuleName, sizeof ModuleName);
  }

  return true;
}

void   dumpVariant( VARIANT &v)
{
  switch( v.vt)
  {
    case VT_I4 :
      fwprintf(stderr, L"Integer: %d\n", v.lVal);
    break;
    case VT_BSTR :
      fwprintf(stderr, L"BStr: %s\n", v.bstrVal);
    break;
    case VT_BOOL:
      fwprintf(stderr, L"Boolean: %d\n", v.boolVal);
    break;
  }
}

/*These can someday go into a msg resource file for NLS translation */
int msgCLSID2stringfailed= (int)  L"Failed to convert string to clsid hr= 0x%1!lx!";
int msgNoParms= (int) L"The number of parameters I got is: %1!u!";
int msgComInitfailed= (int) L"COM Initialize failed hr= 0x%1!lx!";
int msgCLSIDnMethod= (int)  L"CLSID is:%1, method to execute is:%2";
int msgPROGIDnMethod=(int) L"Progid is:%1, method to execute is:%2";
int msgPROGID2CLSID=(int) L"Failed to convert progid to clsid hr= 0x%1!lx!"; 
int msgFailedCoCreate=(int) L"Failed to create instance and interface IID_Dispatch hr=0x%1!lx!"; 
int msgFailedGetdipid=(int) L"Failed to get dispid for method %1";
int msgFailedDIInvoke=(int) L"Invoke of method %1 failed.  hr=0x%2!lx!";
int msgSuccess=(int) L"Success! Return back to java.";
int msgCoTaskMemAllocFail=(int) L"Insufficient resources CoTaskMemAlloc failed.";
int msgCalledDLL= (int)  L"In DLL:%1!s!";
int msgInvokeFailedParms= (int) L"COMProvider: method:%1!s! received error %2!lu!. Error source: %3!ls!\t. Description:%4!s!";
int msgInvokeFailedParms2= (int)  L"Bad argument:%1!lu! method %2!ls!.  hr=0x%3!lx!";
int msgInvokeFailed= (int) L"Invoke of method %1!s! failed.  hr=0x%2!lx!";
int msgUnknowCPPException= (int) L"An unknown CPP exception has occurred";
int msgFailedConvertReturn= (int) L"Failed to convert return parameter, variant type:0x%1!lx!, Object:%2!s!, Method:%3!s!";
int msgFailedMemory= (int) L"Failed to allocate memory!";

const short LOG_INFORMATION = 0x1000;
const short LOG_SUCCESS= 0x100;
const short LOG_WARNING = 0x10;
const short LOG_ERROR = 1;
short eventLevel= LOG_ERROR;

struct Undoit
{
 void (__stdcall*coinit )(void);
 IDispatch *idisp;
 VARIANT *vp;
 
 ~Undoit()
 {
  if (NULL != idisp ) { idisp->Release(); idisp= NULL;}
  if (NULL != vp) { CoTaskMemFree(vp); vp= NULL;}
  if( NULL != coinit) { coinit(); coinit= NULL ; }
 }
};

class COMProvider 
{
  jobject jPP;  //the java pluggable provider
  JNIEnv *env;
  static const char* javaClass;
  public:
  COMProvider(JNIEnv *e, jobject jthis):jPP(jthis),env(e){};

  WORD getMethodDispatchType( const wchar_t *mname)
  {
    WORD ret = DISPATCH_METHOD;
    if(0 == wcsncmp(L"get_", mname, 4)) ret= DISPATCH_PROPERTYGET;
    else if(0 == wcsncmp(L"set_", mname, 4)) ret= DISPATCH_PROPERTYPUT;
    return ret;
  }

  jobject invoke (jstring jthreadingModel, jstring jprogid, jstring jmethodName, jbyteArray parms_in)
  {
   eventlog(LOG_SUCCESS, msgCalledDLL, ModuleName );
   wchar_t progid[256];
   Undoit undoit;
   ZeroIt(undoit);
   jsize noParmsBytes= NULL == parms_in ? 0 : env->GetArrayLength( parms_in); 
   unsigned noParms= noParmsBytes/ sizeof VARIANT;
   eventlog(LOG_SUCCESS, msgNoParms, noParms );
   char *x= NULL;
   if(noParms)
   {
     x= (char*)CoTaskMemAlloc( noParmsBytes ); 
     if( NULL == x)
     {
       throwSoapException( msgCoTaskMemAllocFail ); 
       return NULL;
     }
     memset(x, 0,  noParmsBytes);
     jboolean iscopy;
     jbyte *jpeer= env->GetByteArrayElements(parms_in, &iscopy);
     memcpy(x, jpeer, noParmsBytes);
     if(iscopy == JNI_TRUE) env->ReleaseByteArrayElements(parms_in,jpeer,0);
   }
   // VARIANT *vp= (VARIANT *)x;
   undoit.vp= (VARIANT *)x;
   /*
   for(int i=0; i< noParms; ++i)
   {
     dumpVariant(vp[i]);
   }
   */
   DWORD coInit= COINIT_MULTITHREADED; //The default.
   bool oleInit= false;
   jboolean isCopy;
   wchar_t  *cstr= (wchar_t *)env->GetStringChars(jthreadingModel, &isCopy);
   if(0== wcscmp(cstr, L"MULTITHREADED"))
   { //just 
    coInit= COINIT_MULTITHREADED; //Just recognize it.
   }
   else if(0== wcscmp(cstr, L"APARTMENTTHREADED"))
   {
    coInit= COINIT_APARTMENTTHREADED;
   }
   else if(0== wcscmp(cstr, L"SINGLEAPARTMENTTHREADED"))
   {
    oleInit= true;
   }

   if(isCopy == JNI_TRUE) env->ReleaseStringChars(jthreadingModel, cstr);
   HRESULT hr;
   if(oleInit)
   {
     hr= OleInitialize(NULL);
     if (ASSERT_FAILED(hr))
     {
       eventlog(LOG_WARNING, msgComInitfailed, hr);
       throwSoapException( msgComInitfailed, hr ); 
       return NULL;
     }
     undoit.coinit= OleUninitialize; //Ensure we uninitialize.
   }
   else
   {
     hr= CoInitializeEx(NULL, coInit);
     if (ASSERT_FAILED(hr))
     {
       eventlog(LOG_WARNING, msgComInitfailed, hr);
       throwSoapException( msgComInitfailed, hr ); 
       return NULL;
     }
     undoit.coinit= CoUninitialize; //Ensure we uninitialize.
   }

   cstr= (wchar_t *)env->GetStringChars(jmethodName, &isCopy);
   wchar_t *methodName= _wcsdup(cstr);
   if(isCopy == JNI_TRUE) env->ReleaseStringChars(jmethodName, cstr);
   if(!methodName)
   {
       eventlog(LOG_ERROR, msgFailedMemory);
       throwSoapException( msgFailedMemory); 
       return NULL;
   }

   CLSID clsid;
   cstr= (wchar_t*) env->GetStringChars(jprogid, &isCopy);
   if( L'{' == cstr[0]) //clsid is specified as a string
   {
     hr= CLSIDFromString(cstr, &clsid );
     if (ASSERT_FAILED(hr))
     {
       throwSoapException( msgCLSID2stringfailed, hr ); 
       if(isCopy == JNI_TRUE) env->ReleaseStringChars(jprogid, cstr);
       if(methodName) free(methodName);
       return NULL;
     }
     eventlog(LOG_INFORMATION, msgCLSIDnMethod , cstr, methodName); 
   }
   else
   {
     eventlog(LOG_INFORMATION, msgPROGIDnMethod , cstr, methodName); 
     hr= CLSIDFromProgID( cstr, &clsid); 
     if (ASSERT_FAILED(hr))
     {
       eventlog(LOG_WARNING, msgPROGID2CLSID, hr );
       if(isCopy == JNI_TRUE) env->ReleaseStringChars(jprogid, cstr);
       throwSoapException( msgPROGID2CLSID, hr ); 
       if(methodName) free(methodName);
       return NULL;
     }
   }
   ZeroIt(progid);
   wcsncpy(progid, cstr, (sizeof(progid)/(sizeof progid[0]))-1);
   if(isCopy == JNI_TRUE) env->ReleaseStringChars(jprogid, cstr);

   //Start doing COM stuff

   hr= CoCreateInstance( clsid, NULL, CLSCTX_ALL, IID_IDispatch, (void **)&undoit.idisp  );
     
   if (ASSERT_FAILED(hr))
   {
     eventlog(LOG_WARNING, msgFailedCoCreate, hr );
     throwSoapException( msgFailedCoCreate, hr); 
     if(methodName) free(methodName);
     return NULL;
   }


   DISPID dispid;
   ZeroIt(dispid);
   bool isSet= false;
   unsigned nameOffset= 0;
   if(0 == wcsncmp(L"get_", methodName, 4))  nameOffset= 4;
   else if(0 == wcsncmp(L"set_", methodName, 4)) { nameOffset= 4; isSet= true;}
   wchar_t * mname= methodName+ nameOffset;

   hr= undoit.idisp->GetIDsOfNames(IID_NULL, &mname, 1, GetUserDefaultLCID(), &dispid);

   if (ASSERT_FAILED(hr))
   {
     eventlog(LOG_WARNING, msgFailedGetdipid, methodName ); 
     throwSoapException(msgFailedGetdipid, methodName );
     if(methodName) free(methodName);
     return NULL;
   }

   DISPPARAMS dispparams;
   ZeroIt( dispparams);
   dispparams.rgvarg= undoit.vp;
   dispparams.cArgs= noParms;
   DISPID didpp= DISPID_PROPERTYPUT;
   if(isSet)
   {
     dispparams.cNamedArgs=  1 ;
     dispparams.rgdispidNamedArgs= &didpp;
   }
   VARIANT result;
   ZeroIt(result);
   EXCEPINFO expInfo;
   ZeroIt( expInfo);
   unsigned  parg;
   ZeroIt( parg);

   hr= undoit.idisp->Invoke(dispid, IID_NULL, GetUserDefaultLCID(), getMethodDispatchType(methodName), &dispparams, &result, &expInfo, &parg);

   if(hr == DISP_E_EXCEPTION )
   {
     unsigned long err= (unsigned long) expInfo.wCode;
     if (err == 0l) err= (unsigned long) expInfo.scode;
     wchar_t *source= expInfo.bstrSource ? expInfo.bstrSource : L"";
     wchar_t *desc= expInfo.bstrDescription ? expInfo.bstrDescription : L"";
     throwSoapException(msgInvokeFailedParms,methodName, err, source, desc);
     if(methodName) free(methodName);
     return NULL;
   }

   if (ASSERT_FAILED(hr))
   {
     if(hr == DISP_E_TYPEMISMATCH ||  hr == DISP_E_PARAMNOTFOUND)
     {
        throwSoapException(msgInvokeFailedParms2, noParms- parg , methodName, hr );
        if(methodName) free(methodName);
        return NULL;
     }
     eventlog(LOG_WARNING, msgFailedDIInvoke, methodName,hr ); 
     throwSoapException(msgInvokeFailed ,  methodName, hr);
     if(methodName) free(methodName);
     return NULL;
   }
   jobject jresult= NULL;

   hr= variant2object (env, result, jresult);
   if (ASSERT_FAILED(hr))
   {
     eventlog(LOG_ERROR, msgFailedConvertReturn,  V_VT (&result), progid, methodName); 
     throwSoapException(msgFailedConvertReturn, V_VT (&result), progid, methodName);
     if(methodName) free(methodName);
     return NULL;
   }
   
   if(methodName) free(methodName);
   eventlog(LOG_SUCCESS, msgSuccess ); 

   return jresult;
  }
  //Throw a Java SOAP Exception.
  BOOL throwSoapException( const wchar_t *msg)
  {
    if(NULL== msg) msg= L"Exception thrown by COMProvider";
    jstring jmsg= env->NewStringUTF(ws2mbs(msg)); 
    jclass jcASE= env->FindClass(javaClass);
    jmethodID mid= env->GetStaticMethodID(jcASE,"getSOAPException","(Ljava/lang/String;)Lorg/apache/soap/SOAPException;");
    jthrowable jt= (jthrowable) env->CallStaticObjectMethod(jcASE,mid, jmsg);
    env->Throw(jt);  
    return true;
  }

  BOOL throwSoapException( int id, ...)
  {

    va_list a;
    va_start( a, id);
    wchar_t *msgbuf= NULL;
    FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |FORMAT_MESSAGE_FROM_STRING ,
      (wchar_t *)id, 0, 3, (wchar_t *) &msgbuf, 10000, &a);
                    

    if(NULL== msgbuf || *msgbuf == L'\0') msgbuf= L"Exception thrown by COMProvider";
    jstring jmsg= env->NewStringUTF(ws2mbs(msgbuf)); 
    LocalFree( msgbuf );
    jclass jcASE= env->FindClass(javaClass);
    jmethodID mid= env->GetStaticMethodID(jcASE,"getSOAPException","(Ljava/lang/String;)Lorg/apache/soap/SOAPException;");
    jthrowable jt= (jthrowable) env->CallStaticObjectMethod(jcASE,mid, jmsg);
    env->Throw(jt);  
    env->DeleteLocalRef(jmsg);

   return true;
  }

  BOOL eventlog(short eventtype,  int id, ...)
  {

   if( (eventtype & eventLevel) == 0 ) return false;
   va_list a;
   va_start( a, id);
   wchar_t *msgbuf;
   FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |FORMAT_MESSAGE_FROM_STRING,
      (wchar_t *)id, 0, 3, (wchar_t *) &msgbuf, 10000, &a);
                   
   // fwprintf( stderr,  L"%s\n", msgbuf); 
   jclass  spClass= env->GetObjectClass(jPP);
   jstring jmsg= env->NewStringUTF(ws2mbs(msgbuf)); 
   jmethodID mid= env->GetMethodID(spClass, "logit", "(ILjava/lang/String;)V");
   env->CallVoidMethod(jPP, mid, (jint)eventtype, jmsg);
   env->DeleteLocalRef(jmsg);
   LocalFree( msgbuf );

   return true;
  }
};//class COMProvider  
 const char* COMProvider::javaClass= "org/apache/soap/providers/com/RPCProvider";

extern "C" JNIEXPORT jobject JNICALL Java_org_apache_soap_providers_com_RPCProvider_invoke
  (JNIEnv *env, jobject jo, jstring threadingModel, jstring jprogid, jstring jmethodName, jbyteArray parms_in)
{
 CRTDBGBRK
 COMProvider cp(env, jo);
#ifdef NDEBUG  //This also will to debug on demand so if we are debugging don't do the catch.
 try
 {
#endif 
 env->ExceptionClear(); //Ignore all previous errors.
 return cp.invoke(threadingModel, jprogid, jmethodName,  parms_in);
#ifdef NDEBUG
 }
 catch( ... )
 { //Main point here is not to trap the JVM!
   env->ExceptionClear(); //Ignore all previous errors.
   cp.throwSoapException( msgUnknowCPPException);
   return NULL;
 }
#endif 
}
extern "C" JNIEXPORT void JNICALL Java_org_apache_soap_providers_com_RPCProvider_initlog(JNIEnv *env, jclass jc, jshort jlvl)
{
 eventLevel= jlvl;
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_org_apache_soap_providers_com_RPCProvider_nativeConvertToBString(JNIEnv *env, jclass jc, jstring s)
{
 env->ExceptionClear(); //Let a calling thread to invoke a call record the error.
 jboolean isCopy;
 const wchar_t  * cs= env->GetStringChars(s, &isCopy);
 BSTR bstrS=SysAllocString(cs);
 if(isCopy == JNI_TRUE) env->ReleaseStringChars(s, cs);
 jbyteArray jByteArray= env->NewByteArray(sizeof bstrS);
 jbyte *jr= env->GetByteArrayElements(jByteArray, &isCopy);
 void *x =bstrS;
 memcpy(jr, &x, sizeof x);
 if(isCopy == JNI_TRUE) env->ReleaseByteArrayElements(jByteArray,jr,0);
 return jByteArray;
}

/* make objects from primitives */

jobject bsf_makeBoolean (JNIEnv *jenv, int val) {
  jclass classobj = jenv->FindClass ( "java/lang/Boolean");
  jmethodID constructor = 
    jenv->GetMethodID (classobj, "<init>", "(Z)V");
  return jenv->NewObject (classobj, constructor, (jboolean) val);
}

jobject bsf_makeByte (JNIEnv *jenv, int val) {
  jclass classobj = jenv->FindClass ( "java/lang/Byte");
  jmethodID constructor = 
    jenv->GetMethodID ( classobj, "<init>", "(B)V");
  return jenv->NewObject ( classobj, constructor, (jbyte) val);
}

jobject bsf_makeShort (JNIEnv *jenv, int val) {
  jclass classobj = jenv->FindClass ( "java/lang/Short");
  jmethodID constructor = 
    jenv->GetMethodID ( classobj, "<init>", "(S)V");
  return jenv->NewObject ( classobj, constructor, (jshort) val);
}

jobject bsf_makeInteger (JNIEnv *jenv, int val) {
  jclass classobj = jenv->FindClass ( "java/lang/Integer");
  jmethodID constructor = 
    jenv->GetMethodID ( classobj, "<init>", "(I)V");
  return jenv->NewObject ( classobj, constructor, (jint) val);
}

jobject bsf_makeLong (JNIEnv *jenv, long val) {
  jclass classobj = jenv->FindClass ( "java/lang/Long");
  jmethodID constructor = 
    jenv->GetMethodID ( classobj, "<init>", "(J)V");
  return jenv->NewObject ( classobj, constructor, (jlong) val);
}

jobject bsf_makeFloat (JNIEnv *jenv, float val) {
  jclass classobj = jenv->FindClass ( "java/lang/Float");
  jmethodID constructor = 
    jenv->GetMethodID ( classobj, "<init>", "(F)V");
  return jenv->NewObject ( classobj, constructor, (jfloat) val);
}

jobject bsf_makeDouble (JNIEnv *jenv, double val) {
  jclass classobj = jenv->FindClass ( "java/lang/Double");
  jmethodID constructor = 
    jenv->GetMethodID ( classobj, "<init>", "(D)V");
  return jenv->NewObject ( classobj, constructor, (jdouble) val);
}

HRESULT variant2object (JNIEnv *jenv, const VARIANT &var, jobject &jresult,  bool *localRefCreated)
{
 HRESULT result = S_OK;
 char *buf= NULL;
 if(localRefCreated) *localRefCreated= true; //this is mostly the case.
 switch (V_VT (&var)) {
 case VT_ERROR:  
 case VT_EMPTY:
 case VT_NULL:
  if(localRefCreated) *localRefCreated= false; 
  jresult = NULL;
  break;
 case VT_I1:
  jresult = bsf_makeByte (jenv, (int) V_I1 (&var));
  break;
 case VT_I2:
  jresult = bsf_makeShort (jenv, (int) V_I2 (&var));
  break;
 case VT_I4:
  jresult = bsf_makeInteger (jenv, (int) V_I4 (&var));
  break;
 case VT_I8:
  /* where's V_I8??? */
  jresult = bsf_makeLong (jenv, (long) V_I4 (&var));
  break;
 case VT_R4:
  jresult = bsf_makeFloat (jenv, (float) V_R4 (&var));
  break;
 case VT_R8:
  jresult = bsf_makeDouble (jenv, (double) V_R8 (&var));
  break;
 case VT_BSTR:
  /* if its a string with the right stuff, retract the object */
  buf= ws2mbs(var.bstrVal);
  jresult = jenv->NewStringUTF (buf);
  break;
 case VT_BOOL:
  jresult = bsf_makeBoolean (jenv, (int) V_BOOL (&var));
  break;
 case VT_VARIANT:
  result = variant2object (jenv, *(V_VARIANTREF (&var)), jresult, localRefCreated);
  break;
 case VT_UI1:
  jresult = bsf_makeShort (jenv, (int) V_UI1 (&var));
  break;
 case VT_UI2:
  jresult = bsf_makeInteger (jenv, (int) V_UI2 (&var));
  break;
 case VT_UI4:
  jresult = bsf_makeLong (jenv, (long) V_UI4 (&var));
  break;
 case VT_UI8:
  /* where's V_UI8??? */
  jresult = bsf_makeLong (jenv, (long) V_UI4 (&var));
  break;
 case VT_INT:
  jresult = bsf_makeInteger (jenv, V_INT (&var));
  break;
 case VT_UINT:
  jresult = bsf_makeLong (jenv, (long) V_UINT (&var));
  break;
 default:
  if (V_ISBYREF (&var))
  {
   if((var.vt & (VT_BYREF|VT_ARRAY)) == (VT_BYREF|VT_ARRAY))
   { //It is an array by reference.
    result= DISP_E_BADVARTYPE; //rrfoo can support needs work.
  /*
    //var.vt&= (~VT_BYREF);
    VARIANT arraybyref;
    ZeroIt(arraybyref);
    arraybyref.vt = VT_ARRAY;
    arraybyref.parray= *var.pparray;
    result= variant2object(jenv, arraybyref, jresult localRefCreated);
    */
   }
   else
   { //This is a reference ... dereference.
    result = variant2object (jenv, *((VARIANT *)var.byref), jresult, localRefCreated);
   }
  }
  else
  {
    result= DISP_E_BADVARTYPE;
   break;
  }
 } //endswitch to variant.

 if(!jresult && localRefCreated) *localRefCreated= false;
 return result;
}
