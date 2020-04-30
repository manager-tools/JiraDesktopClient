package com.almworks.util.xmlrpc;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;

class XmlRpcUtils {
  static final String LOCAL_HOST = "127.0.0.1";
  static final long ROBUST_DELAY = 3000;
  static final int SEND_ATTEMPTS = 2;
  static final Vector EMPTY_PARAMETERS = new Vector();
  static final String SERVICE_NAME = "almworks";

  public static final String RESPONSE_UNKNOWN_METHOD = "unknownMethod";
  public static final String RESPONSE_MALFORMED_MESSAGE = "malformedMessage";
  public static final String RESPONSE_MESSAGE_PROCESSING_EXCEPTION = "messageProcessingException";
  public static final String RESPONSE_OK = "ok";
  public static final String[] FAILURE_RESPONSES = {RESPONSE_UNKNOWN_METHOD, RESPONSE_MALFORMED_MESSAGE, RESPONSE_MESSAGE_PROCESSING_EXCEPTION};

  public static StringBuffer dumpCollection(StringBuffer buf, Collection<?> objects) {
    buf.append('(');
    if (objects != null) {
      boolean f = true;
      for (Object object : objects) {
        if (!f)
          buf.append(',');
        f = false;
        dumpObject(buf, object);
      }
    }
    buf.append(')');
    return buf;
  }

  private static void dumpObject(StringBuffer buf, Object object) {
    if (object == null)
      buf.append("null");
    else if (object instanceof Collection)
      dumpCollection(buf, (Collection<?>) object);
    else if (object instanceof Map)
      dumpMap(buf, (Map<?, ?>)object);
    else
      buf.append(String.valueOf(object));
  }

  public static StringBuffer dumpMap(StringBuffer buf, Map<?, ?> map) {
    buf.append('{');
    if (map != null) {
      boolean f = true;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (!f)
          buf.append(',');
        f = false;
        dumpObject(buf, entry.getKey());
        buf.append("=>");
        dumpObject(buf, entry.getValue());
      }
    }
    buf.append('}');
    return buf;
  }
}
