package com.almworks.util.xml;

import org.almworks.util.Log;

import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Entity;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.IOException;

class HTMLEntityResolver {
  private static final String DTD_NAME = "html32";
  private static final HTMLEntityResolver ourInstance = new HTMLEntityResolver();
  private final DTD myDTD;

  private HTMLEntityResolver() {
    // a swing hack to initialize html32 dtd
    new ParserDelegator();

    DTD dtd = null;
    try {
      dtd = DTD.getDTD(DTD_NAME);
    } catch (IOException e) {
      Log.warn("cannot load dtd " + DTD_NAME);
    }
    myDTD = dtd;
  }

  public static synchronized HTMLEntityResolver getInstance() {
    return ourInstance;
  }

  public char getEntityChar(String entityName) {
    if (myDTD == null)
      return 0;
    Entity entity = myDTD.getEntity(entityName);
    if (entity == null)
      return 0;
    char[] data = entity.getData();
    assert data != null : myDTD;
    if (data == null)
      return 0;
    assert data.length == 1 : data;
    if (data.length != 1)
      return 0;
    return data[0];
  }
}
