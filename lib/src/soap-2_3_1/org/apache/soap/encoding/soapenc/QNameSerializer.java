package org.apache.soap.encoding.soapenc;

import org.apache.soap.Constants;
import org.apache.soap.util.Bean;
import org.apache.soap.util.xml.*;
import org.apache.soap.rpc.SOAPContext;
import org.w3c.dom.*;
import java.io.*;

/** A QNameSerializer serializes a QName as follows:
 * <elementName xmlns:ns="QNameURIPart">ns:QNameLocalPart</elementName>
 */
public class QNameSerializer implements Serializer, Deserializer
{
	public void marshall(String inScopeEncStyle, Class javaType, Object src,
                             Object context, Writer sink, NSStack nsStack,
                             XMLJavaMappingRegistry xjmr, SOAPContext ctx)
		throws IllegalArgumentException, IOException
	{
		sink.write('<' + context.toString());
		
		QName qName = (QName)src;
		
		// Make sure our namespace has a valid prefix in this doc.
		String prefix = nsStack.getPrefixFromURI(qName.getNamespaceURI(), sink);
		
	    QName elementType = xjmr.queryElementType(javaType,
                                              Constants.NS_URI_SOAP_ENC);
		
		String xsiNSPrefix =
				nsStack.getPrefixFromURI(Constants.NS_URI_CURRENT_SCHEMA_XSI, sink);
		String elementTypeNSPrefix =
				nsStack.getPrefixFromURI(elementType.getNamespaceURI(), sink);

		sink.write(' ' + xsiNSPrefix + ':' + Constants.ATTR_TYPE + "=\"" +
				   elementTypeNSPrefix + ':' +
				   elementType.getLocalPart() + '\"');

		sink.write('>' + prefix + ':' + qName.getLocalPart());
		sink.write("</" + context.toString() + '>');
	}
  
	public Bean unmarshall(String inScopeEncStyle, QName elementType, Node src,
                               XMLJavaMappingRegistry xjmr,
                               SOAPContext ctx)
		throws IllegalArgumentException
	{
		Element root = (Element)src;
		String value = DOMUtils.getChildCharacterData(root);
		
		int idx = value.indexOf(":");
		if (idx <= 0)
			throw new IllegalArgumentException("No NamespaceURI while deserializing QName");
		
		String prefix = value.substring(0,idx);
		String uri = DOMUtils.getNamespaceURIFromPrefix(src, prefix);
		
		QName qName = new QName(uri, value.substring(idx + 1));

		return new Bean(QName.class, qName);
	}  
}
