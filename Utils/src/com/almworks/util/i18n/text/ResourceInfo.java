package com.almworks.util.i18n.text;

import com.almworks.util.LogHelper;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceInfo {
  private static final ResourceBundle EMPTY;
  static {
    PropertyResourceBundle bundle;
    try {
      bundle = new PropertyResourceBundle(new StringReader(""));
    } catch (IOException e) {
      bundle = null;
      LogHelper.error(e);
    }
    EMPTY = bundle;
  }

  private static final ResourceBundle.Control UTF_8 = new UTF8Control();
  private final ClassLoader myLoader;
  private final String myPath;
  private final ConcurrentHashMap<Locale, ResourceBundle> myLoaded = new ConcurrentHashMap<Locale, ResourceBundle>();

  public ResourceInfo(ClassLoader loader, String path) {
    myLoader = loader;
    myPath = path;
  }

  @NotNull
  public String getString(String suffix, Locale locale) {
    ResourceBundle bundle = ensureLoaded(locale);
    if (bundle == null) return "";
    String string;
    try {
      string = bundle.getString(suffix);
    } catch (MissingResourceException e) {
      string = null;
    }
    if (string == null && bundle != EMPTY) LogHelper.error("Missing value", suffix, locale, this);
    return Util.NN(string);
  }

  private ResourceBundle ensureLoaded(Locale locale) {
    ResourceBundle bundle = myLoaded.get(locale);
    if (bundle == null) {
      try {
        bundle = ResourceBundle.getBundle(myPath, locale, myLoader, UTF_8);
      } catch (Exception e) {
        LogHelper.error(e, this, locale);
        bundle = EMPTY;
      }
      myLoaded.putIfAbsent(locale, bundle);
    }
    return bundle;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<Locale, ResourceBundle> entry : myLoaded.entrySet()) {
      if (entry.getValue() == EMPTY) builder.append("EMPTY (" + entry.getKey() + ") ");
    }
    builder.append(myPath).append(" ").append(myLoader);
    return builder.toString();
  }


  private static class UTF8Control extends ResourceBundle.Control {
    public ResourceBundle newBundle
      (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
      throws IllegalAccessException, InstantiationException, IOException
    {
      // The below is a copy of the default implementation.
      String bundleName = toBundleName(baseName, locale);
      String resourceName = toResourceName(bundleName, "properties");
      ResourceBundle bundle = null;
      InputStream stream = null;
      if (reload) {
        URL url = loader.getResource(resourceName);
        if (url != null) {
          URLConnection connection = url.openConnection();
          if (connection != null) {
            connection.setUseCaches(false);
            stream = connection.getInputStream();
          }
        }
      } else {
        stream = loader.getResourceAsStream(resourceName);
      }
      if (stream != null) {
        try {
          // Only this line is changed to make it to read properties files as UTF-8.
          bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
        } finally {
          stream.close();
        }
      }
      return bundle;
    }

    @Override
    public List<String> getFormats(String baseName) {
      return FORMAT_PROPERTIES;
    }
  }
}
