/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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
 * originally based on software copyright (c) 2000, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.soap.server;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.soap.Constants;
import org.apache.soap.encoding.*;
import org.apache.soap.rpc.*;
import org.apache.soap.server.http.*;
import org.apache.soap.util.xml.*;

/**
 * This class represents the deployment information about a SOAP service.
 *
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class DeploymentDescriptor implements Serializable {
  // types of services
  public static final int SERVICE_TYPE_RPC = 0;
  public static final int SERVICE_TYPE_MESSAGE = 1;

  // scopes for the lifecycles of the provider class
  public static final int SCOPE_REQUEST = 0;
  public static final int SCOPE_SESSION = 1;
  public static final int SCOPE_APPLICATION = 2;

  // types of providers
  public static final byte PROVIDER_JAVA = (byte) 0;
  public static final byte PROVIDER_SCRIPT_FILE = (byte) 1;
  public static final byte PROVIDER_SCRIPT_STRING = (byte) 2;
  public static final byte PROVIDER_USER_DEFINED = (byte) 3;

  protected String id;
  protected int serviceType = SERVICE_TYPE_RPC;
  protected int scope;
  protected byte providerType = -1;
  protected String providerClass;

  protected String serviceClass;     // For user-defined providers
  protected Hashtable  props ;

  protected boolean isStatic;
  protected String scriptFilenameOrString;
  protected String scriptLanguage;
  protected String[] methods;
  protected TypeMapping[] mappings;
  transient SOAPMappingRegistry cachedSMR;
  protected String[] faultListener;
  private transient SOAPFaultRouter fr;

  // Should I throw a mustUnderstand fault if I get
  // any mustUnderstand headers?  If you deploy a
  // service that doesn't process headers at all,
  // this option should technically be set to be
  // a good SOAP citizen.
  protected boolean checkMustUnderstands = false;

  private String defaultSMRClass = null;
  /**
   * Constructor.
   *
   * @param id the name of the service. Should be of valid URI syntax.
   */
  public DeploymentDescriptor () {
  }

  /**
   * ID of this service.
   */

  public void setID (String id) {
    this.id = id;
  }

  public String getID () {
    return id;
  }

  public boolean getCheckMustUnderstands()
  {
      return checkMustUnderstands;
  }
  
  public void setCheckMustUnderstands(boolean doIt)
  {
      checkMustUnderstands = doIt;
  }
  
  /**
   * Type of the service: message or procedural service. Defaults to being
   * a procedural service. The difference is that if its procedural then
   * the methods are invoked by decoding the children of the first body
   * element as being parameters of the method call. If its message-oriented,
   * then no decoding is done and the method is invoked giving the envelope
   * as an argument.
   */
  public void setServiceType (int serviceType) {
    this.serviceType = serviceType;
  }

  public int getServiceType () {
    return serviceType;
  }

  /**
   * Lifecyle of the object providing the service.
   */
  public void setScope (int scope) {
    this.scope = scope;
  }

  public int getScope () {
    return scope;
  }

  public void setDefaultSMRClass(String _defaultSMRClass) {
    defaultSMRClass = _defaultSMRClass;
  }

  public String getDefaultSMRClass() {
    return defaultSMRClass;
  }

  /**
   * Methods provided by the service.
   */
  public void setMethods (String[] methods) {
    this.methods = methods;
  }

  public String[] getMethods () {
    return methods;
  }

  /**
   * Type of provider.
   */
  public void setProviderType (byte providerType) {
    this.providerType = providerType;
  }

  public byte getProviderType () {
    return providerType;
  }

  /**
   * Classname used to load provider/service
   */
  public void setServiceClass (String serviceClass) {
    this.serviceClass = serviceClass;
  }

  public String getServiceClass () {
    return serviceClass;
  }

  public Hashtable getProps() {
    return props ;
  }

  public void setProps(Hashtable props) {
    this.props = props ;
  }

  /**
   * For Java providers, the class providing the service.
   */
  public void setProviderClass (String providerClass) {
    this.providerClass = providerClass;
  }

  public String getProviderClass () {
    return providerClass;
  }

  /**
   * For Java providers, is it static or not.
   */
  public void setIsStatic (boolean isStatic) {
    this.isStatic = isStatic;
  }

  public boolean getIsStatic () {
    return isStatic;
  }

  public void setScriptLanguage (String scriptLanguage) {
    this.scriptLanguage = scriptLanguage;
  }

  public String getScriptLanguage () {
    return scriptLanguage;
  }

  public void setScriptFilenameOrString (String scriptFilenameOrString) {
    this.scriptFilenameOrString = scriptFilenameOrString;
  }

  public String getScriptFilenameOrString () {
    return scriptFilenameOrString;
  }

  /**
   * Type mappings between XML types of certain encodings and
   * Java types.
   *
   * @param map the structure containing the mapping info
   */
  public void setMappings (TypeMapping[] mappings) {
    this.mappings = mappings;
  }

  /**
   * Return the registered mappings.
   */
  public TypeMapping[] getMappings () {
    return mappings;
  }

  /**
   * Cache the SOAP serialization registry for this descriptor; used only
   * by me.
   */
  private void setCachedSMR (SOAPMappingRegistry cachedSMR) {
    this.cachedSMR = cachedSMR;
  }

  private SOAPMappingRegistry getCachedSMR () {
    return cachedSMR;
  }


  public String[] getFaultListener() {return faultListener;}

  public void setFaultListener(String[] _faultListener) {faultListener = _faultListener;}


  public SOAPFaultRouter buildFaultRouter(SOAPContext ctxt) {
        if (fr != null) return fr;

        fr = new SOAPFaultRouter();

        if (faultListener == null) return fr;


        SOAPFaultListener[] lis = new SOAPFaultListener[faultListener.length];
        try {
          for (int i = 0; i < faultListener.length; i++) {
            Class c = ctxt.loadClass( faultListener[i] );
            lis[i] = (SOAPFaultListener)c.newInstance();
          }
        }
        catch (Exception e) {}

        fr.setFaultListener(lis);
        return fr;
  }
  /**
   * Write out the deployment descriptor according to the
   * the deployment descriptor DTD.
   */
  public void toXML(Writer pr) {
    PrintWriter pw = new PrintWriter (pr);

    pw.println ("<isd:service xmlns:isd=\"" +
                Constants.NS_URI_XML_SOAP_DEPLOYMENT + "\" id=\"" + id + "\"" +
                (serviceType != SERVICE_TYPE_RPC ? " type=\"message\"" : "") +
                " checkMustUnderstands=\"" + (checkMustUnderstands ? "true" :
                                                                    "false") +
                                             "\"" +
                ">");

    byte pt = providerType;
    String[] scopes = {"Request", "Session", "Application"};
    String   providerString = null ;

    if ( pt == DeploymentDescriptor.PROVIDER_JAVA )
      providerString = "java" ;
    else  if ( pt == DeploymentDescriptor.PROVIDER_USER_DEFINED )
      providerString = serviceClass ;
    else
      providerString = "script" ;

    pw.print ("  <isd:provider type=\"" + providerString +
              "\" scope=\"" + scopes[scope] + "\" methods=\"");
    for (int i = 0; i < methods.length; i++) {
      pw.print (methods[i]);
      if (i < methods.length-1) {
        pw.print (" ");
      }
    }
    pw.println ("\">");
    if (pt == DeploymentDescriptor.PROVIDER_JAVA) {
      pw.println ("    <isd:java class=\"" + providerClass +
                  "\" static=\"" + (isStatic ? "true" : "false") + "\"/>");
    } else if (pt == DeploymentDescriptor.PROVIDER_SCRIPT_FILE ||
               pt == DeploymentDescriptor.PROVIDER_SCRIPT_STRING ) {
      pw.print ("    <isd:script language=\"" + scriptLanguage + "\"");
      if (pt == DeploymentDescriptor.PROVIDER_SCRIPT_FILE) {
        pw.println (" source=\"" + scriptFilenameOrString + "\"/>");
      } else {
        pw.println (">");
        pw.println ("      <![CDATA[");
        pw.println (scriptFilenameOrString);
        pw.println ("      ]]>");
        pw.println ("    </isd:script>");
      }
    }
    else if ( pt == DeploymentDescriptor.PROVIDER_USER_DEFINED &&
              providerClass != null ) {
      pw.println ("    <isd:java class=\"" + providerClass +
                  "\" static=\"" + (isStatic ? "true" : "false") + "\"/>");
    }

    if ( props != null )
        for ( Enumeration e = props.keys() ; e.hasMoreElements(); ) {
            String  key   = (String) e.nextElement() ;
            String  value = (String) props.get(key);
            pw.println("    <isd:option key=\"" + key + "\" value=\"" +
                       value + "\" />" );
        }

    pw.println ("  </isd:provider>");

        if (faultListener != null) {
                for (int i = 0; i < faultListener.length; i++) {
                        pw.println("  <isd:faultListener>" +
                                   faultListener[i] +
                                   "</isd:faultListener>");
                }
        }

    if (mappings != null) {
      pw.print("  <isd:mappings");
      if (defaultSMRClass != null) {
        pw.println(" defaultRegistryClass=\"" + defaultSMRClass + "\">");
      } else {
        pw.println(">");
      }

      for (int i = 0; i < mappings.length; i++) {
        TypeMapping tm = mappings[i];

        pw.print ("    <isd:map");

        if (tm.encodingStyle != null) {
          pw.print (" encodingStyle=\"" + tm.encodingStyle +"\"");
        }

        if (tm.elementType != null) {
          pw.print (" xmlns:x=\"" + tm.elementType.getNamespaceURI () +
                    "\" qname=\"x:" + tm.elementType.getLocalPart () + "\"");
        }

        if (tm.javaType != null) {
          pw.print (" javaType=\"" + tm.javaType + "\"");
        }

        if (tm.xml2JavaClassName != null) {
          pw.print (" xml2JavaClassName=\"" + tm.xml2JavaClassName + "\"");
        }

        if (tm.java2XMLClassName != null) {
          pw.print (" java2XMLClassName=\"" + tm.java2XMLClassName + "\"");
        }

        pw.println ("/>");
      }

      pw.println ("  </isd:mappings>");
    }

    pw.println ("</isd:service>");
    pw.flush ();
  }

  public static DeploymentDescriptor fromXML(Reader rd)
    throws IllegalArgumentException {
    if (rd == null) {
      throw new IllegalArgumentException("Reader passed to " +
                                         "DeploymentDescriptor.fromXML(...) " +
                                         "must not be null.");
    }

    try {
      DocumentBuilder xdb = XMLParserUtils.getXMLDocBuilder();
      Document        doc = xdb.parse(new InputSource(rd));

      if (doc != null) {
        Element root = doc.getDocumentElement();

        return fromXML(root);
      } else {
        throw new Exception("Document came back null.");
      }
    } catch (Exception e) {
        throw new IllegalArgumentException("Problem parsing " +
                                           "deployment descriptor: " +
                                           e);
    }
  }

  /**
   * Build a deployment descriptor from a document corresponding to
   * the deployment descriptor DTD.
   */
  public static DeploymentDescriptor fromXML(Element root)
    throws IllegalArgumentException {
    if ((root == null) ||
        !root.getNamespaceURI().equals (Constants.NS_URI_XML_SOAP_DEPLOYMENT) ||
        !root.getLocalName().equals ("service")) {
      throw new IllegalArgumentException ("root is null or document element " +
                                          "is not {" +
                                          Constants.NS_URI_XML_SOAP_DEPLOYMENT +
                                          "}service");
    }

    DeploymentDescriptor dd = new DeploymentDescriptor ();
    NodeList nl;
    Element e;

    String id = DOMUtils.getAttribute (root, "id");
    if (id == null) {
      throw new IllegalArgumentException ("required 'id' attribute " +
                                          "missing in deployment descriptor");
    }
    dd.setID (id);
    
    // If we've been marked as checking mustUnderstands, do it.
    String checkMustUnderstands = DOMUtils.getAttribute(root, "checkMustUnderstands");
    if (checkMustUnderstands != null) {
      if (checkMustUnderstands.equals("true"))
        dd.checkMustUnderstands = true;
    }

    // check type of service
    String serviceTypeStr = DOMUtils.getAttribute (root, "type");
    if (serviceTypeStr != null) {
      if (serviceTypeStr.equals ("message")) {
        dd.setServiceType (SERVICE_TYPE_MESSAGE);
      } else {
        throw new IllegalArgumentException ("unknown value for 'type' " +
                                            "attribute: '" + serviceTypeStr +
                                            "': bad deployment descriptor");
      }
    }

    nl = root.getElementsByTagNameNS (Constants.NS_URI_XML_SOAP_DEPLOYMENT,
                                      "provider");
    if ((nl == null) || nl.getLength () != 1) {
      throw new IllegalArgumentException ("exactly one 'provider' element " +
                                          "missing in deployment descriptor");
    }
    e = (Element) nl.item (0);
    String typeStr = DOMUtils.getAttribute (e, "type");
    String scopeStr = DOMUtils.getAttribute (e, "scope");
    String methodsStr = DOMUtils.getAttribute (e, "methods");
    if ((typeStr == null) ||
        // (!typeStr.equals ("java") && !typeStr.equals ("script")) ||
        (scopeStr == null) ||
        (!scopeStr.equals ("Request") &&
         !scopeStr.equals ("Session") && !scopeStr.equals ("Application")) ||
        (methodsStr == null) || methodsStr.equals ("")) {
      throw new IllegalArgumentException ("invalid value for type or scope " +
                                          "or methods attribute in provider " +
                                          "element of deployment descriptor");
    }

    int scope = -1;
    String[] methods;

    Element saved_E = e ;
    nl = e.getElementsByTagNameNS(Constants.NS_URI_XML_SOAP_DEPLOYMENT,
                                  "option" );
    for ( int i = 0 ; nl != null && i < nl.getLength() ; i++ ) {
      String  key, value ;

      e = (Element) nl.item(i);
      key   = DOMUtils.getAttribute( e, "key" );
      value = DOMUtils.getAttribute( e, "value" );

      if ( key == null || key.equals("") )
        throw new IllegalArgumentException("Missing 'key' attribute on " +
                                           "'option' element in deployment " +
                                           "desriptor" );
      if ( dd.props == null ) dd.props = new Hashtable();
      dd.props.put( key, value );
    }
    e = saved_E ;

    if (typeStr.equals ("java")) {
      dd.setProviderType (DeploymentDescriptor.PROVIDER_JAVA);
      nl = e.getElementsByTagNameNS (Constants.NS_URI_XML_SOAP_DEPLOYMENT,
                                     "java");
      if ((nl == null) || nl.getLength () != 1) {
        throw new IllegalArgumentException ("exactly one 'java' element " +
                                            "missing in deployment " +
                                            "descriptor");
      }
      e = (Element) nl.item (0);
      String className = DOMUtils.getAttribute (e, "class");
      if (className == null) {
        throw new IllegalArgumentException ("<java> element requires " +
                                            "'class' attribute");
      }
      dd.setProviderClass (className);
      String isStatic = DOMUtils.getAttribute (e, "static");
      boolean isStaticBool = false;
      if (isStatic != null) {
        if (isStatic.equals ("false")) {
          isStaticBool = false;
        } else if (isStatic.equals ("true")) {
          isStaticBool = true;
        } else {
          throw new IllegalArgumentException ("'static' attribute of " +
                                              "<java> element must be " +
                                              "true or false");
        }
      }
      dd.setIsStatic (isStaticBool);

    } else if (typeStr.equals ("script")) {
      nl = e.getElementsByTagNameNS (Constants.NS_URI_XML_SOAP_DEPLOYMENT,
                                     "script");
      if ((nl == null) || nl.getLength () != 1) {
        throw new IllegalArgumentException ("exactly one 'script' element " +
                                            "missing in deployment " +
                                            "descriptor");
      }
      e = (Element) nl.item (0);
      dd.setScriptLanguage (DOMUtils.getAttribute (e, "language"));
      String source = DOMUtils.getAttribute (e, "source");
      if (source != null) {
        dd.setProviderType (DeploymentDescriptor.PROVIDER_SCRIPT_FILE);
        dd.setScriptFilenameOrString (source);
      } else {
        dd.setProviderType (DeploymentDescriptor.PROVIDER_SCRIPT_STRING);
        dd.setScriptFilenameOrString (DOMUtils.getChildCharacterData (e));
      }
    } else {
      // "type" element isn't one of the predefined ones so assume it's the
      // class name of a specific provider loader
      dd.setProviderType (DeploymentDescriptor.PROVIDER_USER_DEFINED);
      dd.setServiceClass (typeStr);

      // Support old 'java' tag
      nl = e.getElementsByTagNameNS (Constants.NS_URI_XML_SOAP_DEPLOYMENT,
                                     "java");
      if ( nl != null ) {
        if ( nl.getLength () > 1) {
          throw new IllegalArgumentException ("exactly one 'java' element " +
                                              "missing in deployment " +
                                              "descriptor");
        }
        if ( nl.getLength() != 0 ) {
          e = (Element) nl.item (0);
          String className = DOMUtils.getAttribute (e, "class");
          if (className == null) {
            throw new IllegalArgumentException ("<java> element requires " +
                                                "'class' attribute");
          }
          dd.setProviderClass (className);
          String isStatic = DOMUtils.getAttribute (e, "static");
          boolean isStaticBool = false;
          if (isStatic != null) {
            if (isStatic.equals ("false")) {
              isStaticBool = false;
            } else if (isStatic.equals ("true")) {
              isStaticBool = true;
            } else {
              throw new IllegalArgumentException ("'static' attribute of " +
                                                  "<java> element must be " +
                                                  "true or false");
            }
          }
          dd.setIsStatic (isStaticBool);
          // End of old 'java' tag
        }
      }
    }

    if (scopeStr.equals ("Request")) {
      scope = DeploymentDescriptor.SCOPE_REQUEST;
    } else if (scopeStr.equals ("Session")) {
      scope = DeploymentDescriptor.SCOPE_SESSION;
    } else { // scopeStr.equals ("Application")
      scope = DeploymentDescriptor.SCOPE_APPLICATION;
    }
    dd.setScope (scope);

    StringTokenizer st = new StringTokenizer (methodsStr);
    int nTokens = st.countTokens ();
    methods = new String[nTokens];
    for (int i = 0; i < nTokens; i++) {
      methods[i] = st.nextToken ();
    }
    dd.setMethods (methods);
    
    //read the fault listeners
    nl = root.getElementsByTagNameNS (Constants.NS_URI_XML_SOAP_DEPLOYMENT,
                                      "faultListener");
    String[] lis = new String[nl.getLength()];

    try {
      for (int i = 0; i < nl.getLength(); i++) {
        // Class.forName(DOMUtils.getChildCharacterData((Element)nl.item(i))).newInstance();
        lis[i] = DOMUtils.getChildCharacterData((Element)nl.item(i));
      }
    }
    catch (Exception ex) {
      throw new IllegalArgumentException(ex.getMessage());
    }

    dd.setFaultListener(lis);

    // read the type mappings
    nl = root.getElementsByTagNameNS (Constants.NS_URI_XML_SOAP_DEPLOYMENT,
                                      "mappings");
    if ((nl == null) || nl.getLength () > 1) {
      throw new IllegalArgumentException ("at most one 'mappings' element " +
                                          "allowed in deployment descriptor");
    }
    if (nl.getLength () == 1) {
      e = (Element) nl.item (0);

      String className = DOMUtils.getAttribute(e, "defaultRegistryClass");

      if (className != null) {
        dd.setDefaultSMRClass(className);
      }

      nl = e.getElementsByTagNameNS (Constants.NS_URI_XML_SOAP_DEPLOYMENT,
                                     "map");
      int nmaps = nl.getLength ();
      if (nmaps > 0) {
        TypeMapping[] tms = new TypeMapping[nmaps];
        dd.setMappings (tms);
        for (int i = 0; i < nmaps; i++) {
          e = (Element) nl.item (i);
          QName qname = DOMUtils.getQualifiedAttributeValue(e, "qname");
          tms[i] =
            new TypeMapping (DOMUtils.getAttribute (e, "encodingStyle"),
                             qname,
                             DOMUtils.getAttribute (e, "javaType"),
                             DOMUtils.getAttribute (e, "java2XMLClassName"),
                             DOMUtils.getAttribute (e, "xml2JavaClassName"));
        }
      }
    }

    return dd;
  }

  /**
   * What do u think this does?
   */
  public String toString () {
    StringBuffer methodsStrbuf = new StringBuffer ("[");
    for (int i = 0; i < methods.length; i++) {
      methodsStrbuf.append (methods[i]);
      if (i < methods.length-1) {
        methodsStrbuf.append (",");
      }
    }
    methodsStrbuf.append ("]");
    String header = "[DeploymentDescriptor id='" + id + "', " +
      ((serviceType != SERVICE_TYPE_RPC) ? "type='message', " : "") +
      "scope='" + scope + "', ";
    String body = null;
    if (providerType == PROVIDER_JAVA) {
      body = "class='" + providerClass + "', static='" + isStatic + "', ";
    } else if (providerType == PROVIDER_SCRIPT_FILE) {
      body = "source='" + scriptFilenameOrString + "', ";
      body += "language='" + scriptLanguage + "', ";
    } else if (providerType == PROVIDER_USER_DEFINED) {
      body = "type='" + serviceClass + "', class='" + providerClass ;
      body += "', static='" + isStatic + "', ";
    }

    StringBuffer lis = new StringBuffer("[");
    if (faultListener != null)
      for (int i = 0; i < faultListener.length;
        lis.append(faultListener[i]), lis.append(" "), i++);
    lis.append("]");

    StringBuffer opts = new StringBuffer();
    if (props != null) 
      opts.append( props.toString() );

    return header + body + "methods='" + methodsStrbuf + "', " +
      "faultListener='" + lis + "', " + "mappings='" +
      mappingsToString(mappings) + "'], " +
      "opts='" + opts ;
  }

  private static String mappingsToString(TypeMapping[] mappings) {
    if (mappings != null) {
      StringBuffer strBuf = new StringBuffer();

      for (int i = 0; i < mappings.length; i++) {
        strBuf.append((i > 0 ? " " : "") + mappings[i]);
      }

      return strBuf.toString();
    } else {
      return null;
    }
  }

  /**
   * Utility to generate an XML serialization registry from all the
   * type mappings registered into a deployment descriptor.
   *
   * @param dd the deployment descriptor
   * @return the xml serialization registry
   */
  public static SOAPMappingRegistry
      buildSOAPMappingRegistry (DeploymentDescriptor dd, SOAPContext ctx) {
    TypeMapping[] maps = dd.getMappings ();
    SOAPMappingRegistry smr = dd.getCachedSMR ();

    if (smr != null) {
      return smr;
    } else {
      String defaultSMRClassName = dd.getDefaultSMRClass();

      if (defaultSMRClassName != null) {
        try {
          Class defaultSMRClass = ctx.loadClass( defaultSMRClassName );

          smr = (SOAPMappingRegistry)defaultSMRClass.newInstance();
        }
        catch (Exception e) {
	  // this needs to be logged somewhere
	}
      }

      if (smr == null) {
	SOAPMappingRegistry baseReg = SOAPMappingRegistry.getBaseRegistry (
          Constants.NS_URI_CURRENT_SCHEMA_XSD);
	if (maps == null) {
	  dd.setCachedSMR (baseReg);
	  return baseReg;
	} else {
	  smr = new SOAPMappingRegistry (baseReg); 
        }
      }
    }

    if (maps != null) {
      for (int i = 0; i < maps.length; i++) {
        TypeMapping tm = maps[i];
        int step = 0;
        try {
          step = 0;
          Class javaType = null;
          if (tm.javaType != null) 
            javaType = ctx.loadClass( tm.javaType );
          step = 1;
          Serializer s = null;
          if (tm.java2XMLClassName != null) {
            Class c = ctx.loadClass( tm.java2XMLClassName );
            s = (Serializer) c.newInstance ();
          }
          step = 2;
          Deserializer d = null;
          if (tm.xml2JavaClassName != null) {
            Class c = ctx.loadClass( tm.xml2JavaClassName );
            d = (Deserializer) c.newInstance ();
          }
          smr.mapTypes (tm.encodingStyle, tm.elementType, javaType, s, d);
        } catch (Exception e2) {
          String m = "Deployment error in SOAP service '" + dd.getID () +
            "': ";
          if (step == 0) {
            m += "class name '" + tm.javaType + "' could not be resolved: ";
          } else if (step == 1) {
            m += "class name '" + tm.java2XMLClassName + "' could not be " +
              "resolved as a serializer: ";
          } else {
            m += "class name '" + tm.xml2JavaClassName + "' could not be " +
              "resolved as a deserializer: ";
          }
          throw new IllegalArgumentException (m + e2.getMessage ());
        }
      }
    }
    dd.setCachedSMR (smr);
    return smr;
  }
}
