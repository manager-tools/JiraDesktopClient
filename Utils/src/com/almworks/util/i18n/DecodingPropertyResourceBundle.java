package com.almworks.util.i18n;

import com.almworks.util.files.FileUtil;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Collections15;

import java.io.*;
import java.util.*;

/**
 * Copied from PropertyResourceBundle
 */
public class DecodingPropertyResourceBundle extends ResourceBundle {
  private static final String ENCODING = "encoding";

  private final Map<String, String> myMap;

  public DecodingPropertyResourceBundle(File file, String keyPrefix) throws IOException {
    Properties properties = new Properties();
    load(file, properties);
    String encoding = properties.getProperty(ENCODING);
    if (encoding != null && !"ISO-8859-1".equalsIgnoreCase(encoding)) {
      properties.clear();
      decodeAndReload(file, encoding, properties);
    }
    HashMap<String, String> map = new HashMap(properties);
    prefixKeys(map, keyPrefix);
    myMap = map;
  }

  private void load(File file, Properties properties) throws IOException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      InputStream in = new BufferedInputStream(fis);
      properties.load(in);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(fis);
    }
  }

  private void decodeAndReload(File file, String encoding, Properties properties) throws IOException {
    byte[] bytes = FileUtil.native2ascii(file, encoding);
    InputStream in = new ByteArrayInputStream(bytes);
    properties.load(in);
    in.close();
  }

  private void prefixKeys(HashMap<String, String> map, String keyPrefix) throws IOException {
    if (keyPrefix == null || keyPrefix.length() == 0)
      return;
    Map.Entry<String, String>[] entries = map.entrySet().toArray(new Map.Entry[map.size()]);
    for (Map.Entry<String, String> entry : entries) {
      String key = entry.getKey();
      map.put(keyPrefix + key, map.remove(key));
    }
  }

  /**
   * Implementation of ResourceBundle.getKeys.
   */
  public Enumeration<String> getKeys() {
    return Collections15.enumerate(myMap.keySet());
  }

  // Implements java.util.ResourceBundle.handleGetObject; inherits javadoc specification.
  public Object handleGetObject(String key) {
    if (key == null) {
      throw new NullPointerException();
    }
    return myMap.get(key);
  }
}
