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

package org.apache.soap.rpc;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.*;
import org.apache.soap.encoding.*;
import org.apache.soap.server.*;

/**
 * An <code>RPCMessage</code> is the base class that <code>Call</code> and
 * <code>Response</code> extend from. Any work that is common to both
 * <code>Call</code> and <code>Response</code> is done here.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 * @author Sanjiva Weerawarana (sanjiva@watson.ibm.com)
 */
public class RPCMessage implements Serializer {
  protected String targetObjectURI;
  protected String fullTargetObjectURI;
  protected String methodName;
  protected Vector params;
  protected Header header;
  protected String encodingStyleURI;
  protected SOAPContext ctx;

  protected RPCMessage(String targetObjectURI, String methodName,
                       Vector params, Header header,
                       String encodingStyleURI, SOAPContext ctx) {
    setTargetObjectURI(targetObjectURI);
    this.methodName       = methodName;
    this.params           = params;
    this.header           = header;
    this.encodingStyleURI = encodingStyleURI;
    this.ctx              = ctx;
  }

  public void setTargetObjectURI(String targetObjectURI) {
    // Any incoming URI must be the full URI
    this.fullTargetObjectURI = targetObjectURI;

    // Now, we should splice the URI into the actual resource to connect to,
    // and the key.
    this.targetObjectURI = StringUtils.parseFullTargetObjectURI(
                              targetObjectURI);
  }

  public String getTargetObjectURI() {
    return targetObjectURI;
  }

  public void setFullTargetObjectURI(String targetObjectURI) {
    setTargetObjectURI(targetObjectURI);
  }

  public String getFullTargetObjectURI() {
    return fullTargetObjectURI;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setParams(Vector params) {
    this.params = params;
  }

  public Vector getParams() {
    return params;
  }

  public void setHeader(Header header) {
    this.header = header;
  }

  public Header getHeader() {
    return header;
  }

  public void setEncodingStyleURI(String encodingStyleURI) {
    this.encodingStyleURI = encodingStyleURI;
  }

  public String getEncodingStyleURI() {
    return encodingStyleURI;
  }

  protected void setSOAPContext(SOAPContext ctx) {
    this.ctx = ctx;
  }

  public SOAPContext getSOAPContext() {
    return ctx;
  }

  protected Envelope buildEnvelope(boolean isResponse) {
    // Construct a new envelope for this message.
    Envelope env = new Envelope();
    Body body = new Body();
    Vector bodyEntries = new Vector();

    bodyEntries.addElement(new Bean(
        (isResponse ? Response.class : Call.class),
        this));
    body.setBodyEntries(bodyEntries);
    env.setBody(body);
    env.setHeader(header);

    return env;
  }

  protected static RPCMessage extractFromEnvelope(Envelope env,
                                                  ServiceManager svcMgr,
                                                  boolean isResponse,
                                                  SOAPMappingRegistry respSMR,
                                                  SOAPContext ctx)
    throws IllegalArgumentException {
    Body body = env.getBody();
    Vector bodyEntries = body.getBodyEntries();
    RPCMessage msg = null;

    // Unmarshall the message, which is the first body entry.
    if (bodyEntries.size() > 0) {
      Element msgEl = (Element)bodyEntries.elementAt(0);
      Class toClass = isResponse ? Response.class : Call.class;
      String declEnvEncStyle = env.getAttribute(new QName(
        Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE));
      String declBodyEncStyle = body.getAttribute(new QName(
        Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE));
      String actualEncStyle = declBodyEncStyle != null
                              ? declBodyEncStyle
                              : declEnvEncStyle;

      msg = RPCMessage.unmarshall(actualEncStyle, msgEl, toClass, svcMgr,
                                  respSMR, ctx);
      msg.setHeader(env.getHeader());

      return msg;
    } else {
      throw new IllegalArgumentException("An '" + Constants.Q_ELEM_BODY +
                                         "' element must contain a " +
                                         "child element.");
    }
  }

  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException {
    nsStack.pushScope();

    RPCMessage msg               = (RPCMessage)src;
    boolean    isResponse        = (javaType == Response.class);
    String     targetObjectURI   = Utils.cleanString(
                                       msg.getFullTargetObjectURI());
    String     methodName        = msg.getMethodName();
    Vector     params            = msg.getParams();
    String     suffix            = isResponse
                                   ? RPCConstants.RESPONSE_SUFFIX
                                   : "";
    String     declMsgEncStyle   = msg.getEncodingStyleURI();
    String     actualMsgEncStyle = declMsgEncStyle != null
                                   ? declMsgEncStyle
                                   : inScopeEncStyle;

    // If this message is a response, check for a fault.
    if (isResponse) {
      Response resp = (Response)msg;

      if (!resp.generatedFault()) {
        // Get the prefix for the targetObjectURI.
        StringWriter nsDeclSW = new StringWriter();
        String targetObjectNSPrefix = nsStack.getPrefixFromURI(
          targetObjectURI, nsDeclSW);

        sink.write('<' + targetObjectNSPrefix + ':' +
                   methodName + suffix + nsDeclSW);

        // Determine the prefix associated with the NS_URI_SOAP_ENV
        // namespace URI.
        String soapEnvNSPrefix = nsStack.getPrefixFromURI(
          Constants.NS_URI_SOAP_ENV, sink);

        if (declMsgEncStyle != null
            && !declMsgEncStyle.equals(inScopeEncStyle)) {
          sink.write(' ' + soapEnvNSPrefix + ':' +
                     Constants.ATTR_ENCODING_STYLE + "=\"" +
                     declMsgEncStyle + '\"');
        }

        sink.write('>' + StringUtils.lineSeparator);

        // Get the returnValue.
        Parameter ret = resp.getReturnValue();

        if (ret != null) {
          String declParamEncStyle = ret.getEncodingStyleURI();
          String actualParamEncStyle = declParamEncStyle != null
                                       ? declParamEncStyle
                                       : actualMsgEncStyle;
          Serializer ser = xjmr.querySerializer(Parameter.class,
                                                actualParamEncStyle);

          ser.marshall(actualMsgEncStyle, Parameter.class, ret, null,
                       sink, nsStack, xjmr, ctx);

          sink.write(StringUtils.lineSeparator);
        }

        serializeParams(params, actualMsgEncStyle, sink, nsStack, xjmr, ctx);

        sink.write("</" + targetObjectNSPrefix + ':' +
                   methodName + suffix + '>' +
                   StringUtils.lineSeparator);
      } else {
        // Get the fault information.
        Fault fault = resp.getFault();

        fault.marshall(actualMsgEncStyle, sink, nsStack, xjmr, ctx);
      }
    } else {
      // Get the prefix for the targetObjectURI.
      StringWriter nsDeclSW = new StringWriter();
      String targetObjectNSPrefix = nsStack.getPrefixFromURI(targetObjectURI,
                                                             nsDeclSW);

      sink.write('<' + targetObjectNSPrefix + ':' +
                 methodName + suffix + nsDeclSW);

      // Determine the prefix associated with the NS_URI_SOAP_ENV
      // namespace URI.
      String soapEnvNSPrefix = nsStack.getPrefixFromURI(
        Constants.NS_URI_SOAP_ENV, sink);

      if (declMsgEncStyle != null
          && !declMsgEncStyle.equals(inScopeEncStyle)) {
        sink.write(' ' + soapEnvNSPrefix + ':' +
                   Constants.ATTR_ENCODING_STYLE + "=\"" +
                   declMsgEncStyle + '\"');
      }

      sink.write('>' + StringUtils.lineSeparator);

      serializeParams(params, actualMsgEncStyle, sink, nsStack, xjmr, ctx);

      sink.write("</" + targetObjectNSPrefix + ':' +
                 methodName + suffix + '>');
    }

    nsStack.popScope();
  }

  private void serializeParams(Vector params, String inScopeEncStyle,
                               Writer sink, NSStack nsStack,
                               XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IOException {
    // If parameters exist, serialize them.
    if (params != null) {
      int size = params.size();

      for (int i = 0; i < size; i++) {
        Parameter param = (Parameter)params.elementAt(i);
        String declParamEncStyle = param.getEncodingStyleURI();
        String actualParamEncStyle = declParamEncStyle != null
                                     ? declParamEncStyle
                                     : inScopeEncStyle;
        Serializer ser = xjmr.querySerializer(Parameter.class,
                                              actualParamEncStyle);

        ser.marshall(inScopeEncStyle, Parameter.class, param, null, sink,
                     nsStack, xjmr, ctx);

        sink.write(StringUtils.lineSeparator);
      }
    }
  }

  public static RPCMessage unmarshall(String inScopeEncStyle, Node src,
                                      Class toClass, ServiceManager svcMgr,
                                      SOAPMappingRegistry respSMR,
                                      SOAPContext ctx)
    throws IllegalArgumentException {
    SOAPMappingRegistry smr       = null;
    Element   root                = (Element)src;
    boolean   isResponse          = (toClass == Response.class);
    String    fullTargetObjectURI = null;
    String    targetObjectURI     = null;
    String    methodName          = null;
    Parameter returnValue         = null;
    Fault     fault               = null;
    Vector    params              = null;
    String    declMsgEncStyle     = DOMUtils.getAttributeNS(root,
      Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE);
    String    actualMsgEncStyle   = declMsgEncStyle != null
                                    ? declMsgEncStyle
                                    : inScopeEncStyle;

    // Is it a fault?
    if (isResponse && Constants.Q_ELEM_FAULT.matches(root)) {
      fault = Fault.unmarshall(actualMsgEncStyle, root, respSMR, ctx);
    } else {
      // Must be a method call or a faultless response.
      String tagName = root.getLocalName();

      // This is the 'full' URI.
      fullTargetObjectURI = root.getNamespaceURI();
      targetObjectURI = StringUtils.parseFullTargetObjectURI(
                            fullTargetObjectURI);

      // Determine the XML serialization registry based on whether
      // I'm on the server side or on the client side.
      if (!isResponse) {
        // I'm on the server side unmarshalling a call.
        DeploymentDescriptor dd = null;
        try {
          dd = svcMgr.query(targetObjectURI);
          smr = DeploymentDescriptor.buildSOAPMappingRegistry(dd, ctx);
        } catch (SOAPException e) {
          throw new IllegalArgumentException("Unable to resolve " +
                                             "targetObjectURI '" +
                                             targetObjectURI + "'.");
        }
      } else {
        // I'm on the client unmarshalling a response.
        smr = respSMR;
      }
      
      // For RPC, default to SOAP section 5 encoding if no
      // encodingStyle attribute is set.
      smr.setDefaultEncodingStyle(Constants.NS_URI_SOAP_ENC);

      methodName = tagName;

      /*
        Sanity check: the name of the method element should be the
        methodName for a call and should be methodName+"Response"
        for a response. Note: currently no way to know the methodName
        other than from the tag name.
      */
      if (isResponse && methodName.endsWith(RPCConstants.RESPONSE_SUFFIX)) {
        // Strip "Response" from end of tagName to derive methodName.
        methodName = methodName.substring(0, methodName.length() - 8);
      }

      Element tempEl = DOMUtils.getFirstChildElement(root);

      // Get the return value.
      if (isResponse && tempEl != null) {
        String declParamEncStyle = DOMUtils.getAttributeNS(tempEl,
          Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE);
        String actualParamEncStyle = declParamEncStyle != null
                                     ? declParamEncStyle
                                     : actualMsgEncStyle;
        Bean returnBean = smr.unmarshall(actualParamEncStyle,
                                         RPCConstants.Q_ELEM_PARAMETER,
                                         tempEl, ctx);

        returnValue = (Parameter)returnBean.value;

        if (declParamEncStyle != null)
        {
          returnValue.setEncodingStyleURI(declParamEncStyle);
        }

        tempEl = DOMUtils.getNextSiblingElement(tempEl);
      }

      // Get the parameters.
      if (tempEl != null) {
        for (params = new Vector();
             tempEl != null;
             tempEl = DOMUtils.getNextSiblingElement(tempEl)) {
          String declParamEncStyle = DOMUtils.getAttributeNS(tempEl,
            Constants.NS_URI_SOAP_ENV, Constants.ATTR_ENCODING_STYLE);
          String actualParamEncStyle = declParamEncStyle != null
                                       ? declParamEncStyle
                                       : actualMsgEncStyle;
          Bean paramBean = smr.unmarshall(actualParamEncStyle,
                                          RPCConstants.Q_ELEM_PARAMETER,
                                          tempEl, ctx);
          Parameter param = (Parameter)paramBean.value;

          if (declParamEncStyle != null) {
            param.setEncodingStyleURI(declParamEncStyle);
          }

          params.addElement(param);
        }
      }
    }

    RPCMessage msg = isResponse
                     ? (fault == null
                        ? (RPCMessage)new Response(fullTargetObjectURI, methodName,
                                                   returnValue, params, null,
                                                   declMsgEncStyle, ctx)
                        : (RPCMessage)new Response(fullTargetObjectURI, methodName,
                                                   fault, params, null,
                                                   declMsgEncStyle, ctx))
                     : (RPCMessage)new Call(fullTargetObjectURI, methodName,
                                            params, null, actualMsgEncStyle, ctx);

    if (msg instanceof Call) {
      ((Call)msg).setSOAPMappingRegistry(smr);
    }

    return msg;
  }

  public String toString() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    boolean isResponse = this instanceof Response;

    pw.print("[Header=" + header + "] " +
             "[methodName=" + methodName + "] " +
             "[targetObjectURI=" + targetObjectURI + "] " +
             "[encodingStyleURI=" + encodingStyleURI + "] " +
             "[SOAPContext=" + ctx + "] ");

    if (isResponse) {
      Response res = (Response)this;

      if (res.generatedFault()) {
        pw.print("[fault=" + res.getFault() + "] ");
      } else {
        pw.println("[return=" + res.getReturnValue() + "] ");
      }
    }

    pw.print("[Params={");

    if (params != null) {
      for (int i = 0; i < params.size(); i++) {
        if (i > 0) {
          pw.print(", ");
        }

        pw.print("[" + params.elementAt(i) + "]");
      }
    }

    pw.print("}]");

    return sw.toString();
  }
}
