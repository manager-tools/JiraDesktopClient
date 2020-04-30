package com.almworks.api.install;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PropertyUtils {
  public static interface StreamSource {
    InputStream openStream() throws IOException;
  }

  public static Properties loadProperties(Class<?> clazz, String resource) {
    return loadProperties(fromResource(clazz, resource));
  }

  public static StreamSource fromResource(final Class<?> clazz, final String resource) {
    return new StreamSource() {
      @Override
      public InputStream openStream() throws IOException {
        return clazz.getClassLoader().getResourceAsStream(resource);
      }
    };
  }

  public static Properties loadProperties(File file) {
    return loadProperties(fromFile(file));
  }

  public static StreamSource fromFile(final File file) {
    return new StreamSource() {
      @Override
      public InputStream openStream() throws IOException {
        if(file != null && file.isFile() && file.canRead()) {
          return new FileInputStream(file);
        }
        return null;
      }
    };
  }

  public static Properties loadProperties(StreamSource source) {
    final Properties props = new Properties();

    InputStream stream = null;
    try {
      stream = source.openStream();
      if(stream != null) {
        props.load(stream);
      }
    } catch(IOException e) {
      logWarn(e.getMessage(), e);
    } catch(IllegalArgumentException e) {
      logWarn(e.getMessage(), e);
    } finally {
      closeStream(stream);
    }

    return props;
  }

  private static void logWarn(String msg, Throwable e) {
    Logger.getLogger(PropertyUtils.class.getName()).log(Level.WARNING, msg, e);
  }

  private static void closeStream(Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  public static void changeProperties(Properties props, Map<String, String> diff) {
    for(final Map.Entry<String, String> e : diff.entrySet()) {
      if(e.getValue() != null) {
        props.setProperty(e.getKey(), e.getValue());
      } else {
        props.remove(e.getKey());
      }
    }
  }

  public static synchronized void saveProperties(Map<String, String> diff, File file, String comment) {
    final Properties props = loadProperties(file);
    changeProperties(props, diff);
    storeProperties(props, file, comment);
  }

  private static void storeProperties(Properties props, File file, String comment) {
    OutputStream stream = null;
    try {
      stream = new FileOutputStream(file);
      props.store(stream, comment);
    } catch(IOException e) {
      logWarn(e.getMessage(), e);
    } finally {
      closeStream(stream);
    }
  }
}
