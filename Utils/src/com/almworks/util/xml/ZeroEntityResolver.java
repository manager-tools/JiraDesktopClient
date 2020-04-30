package com.almworks.util.xml;

import org.almworks.util.Util;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ZeroEntityResolver implements EntityResolver {
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
    if (systemId == null)
      return null;
    systemId = Util.upper(systemId.trim());
    if (!systemId.endsWith(".DTD"))
      return null;
    InputSource result = new InputSource(new StringReader(""));
    result.setPublicId(publicId);
    result.setSystemId(systemId);
    return result;
  }
}
