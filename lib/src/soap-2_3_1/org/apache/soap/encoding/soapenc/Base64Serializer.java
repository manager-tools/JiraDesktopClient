package org.apache.soap.encoding.soapenc;

import java.io.*;
import org.w3c.dom.*;
import org.apache.soap.util.*;
import org.apache.soap.util.xml.*;
import org.apache.soap.*;
import org.apache.soap.rpc.*;

/**
 * A <code>Base64Serializer</code> is used to serialize and deserialize
 * byte arrays using the <code>SOAP-ENC</code> encoding style. The byte
 * arrays are encoded using the SOAP-ENC:base64 subtype.
 *
 * @author Matthew J. Duftler (duftler@us.ibm.com)
 */
public class Base64Serializer implements Serializer, Deserializer
{
  public void marshall(String inScopeEncStyle, Class javaType, Object src,
                       Object context, Writer sink, NSStack nsStack,
                       XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException, IOException
  {
    nsStack.pushScope();

    byte[] bytes = (byte[])src;

    if (bytes == null)
    {
      SoapEncUtils.generateNullStructure(inScopeEncStyle,
                                     javaType,
                                     context,
                                     sink,
                                     nsStack,
                                     xjmr);
    }
    else
    {
      SoapEncUtils.generateStructureHeader(inScopeEncStyle,
                                         javaType,
                                         context,
                                         sink,
                                         nsStack,
                                         xjmr);

      sink.write(Base64.encode(bytes) + "</" + context + '>');
    }

    nsStack.popScope();
  }

  public Bean unmarshall(String inScopeEncStyle, QName elementType, Node src,
                         XMLJavaMappingRegistry xjmr, SOAPContext ctx)
    throws IllegalArgumentException
  {
    Element root = (Element)src;
    String value = DOMUtils.getChildCharacterData(root);

    return new Bean(byte[].class, Base64.decode(value));
  }
}
