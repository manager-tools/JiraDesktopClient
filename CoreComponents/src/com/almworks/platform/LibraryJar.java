package com.almworks.platform;

import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.util.files.ExtensionFileFilter;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jdom.JDOMException;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class LibraryJar {
  private static final String PLATFORM_XML = "META-INF/platform.xml";
  private static final long TIME_EPSILON = 10000;
  private static final Set<String> EMPTY = new HashSet<String>();

  private final File myJarFile;
  private final File myComponentCacheXmlDir;

  private static final long YEARS_30 = 30L * 365 * 24 * 60 * 60 * 1000;

  LibraryJar(File jarFile, File componentCacheXmlDir) {
    myJarFile = jarFile;
    myComponentCacheXmlDir = componentCacheXmlDir;
  }

  public boolean checkJar() {
    boolean r = myJarFile.isFile() && myJarFile.canRead();
    if (!r) {
      Log.warn("cannot read jar (" + myJarFile + ")");
    }
    return r;
  }

  public Set<String> getComponents() {
    InputStream xmlStream = null;
    try {
      xmlStream = getBundledXml();
      if (xmlStream == null) {
        xmlStream = getHintXml();
        if (xmlStream == null) {
          return scanJarForComponents();
        }
      }
      try {
        return XmlHintFiles.readHintStream(xmlStream);
      } catch (JDOMException e) {
        return scanJarForComponents();
      }
    } catch (IOException e) {
      Log.warn("reading myJarFile (" + myJarFile + "):", e);
      return EMPTY;
    } finally {
      if (xmlStream != null)
        try {
          xmlStream.close();
        } catch (IOException e) {
        }
    }
  }

  private Set<String> scanJarForComponents() throws IOException {
    Set<String> result = scanJar();
    File file = getHintFile();
    try {
      writeHintFile(result, file);
    } catch (IOException e) {
      Log.warn("cannot write hint file: " + file);
    }
    return result;
  }

  private void writeHintFile(Set<String> result, File hintFile) throws IOException {
    if (hintFile.exists() && !hintFile.canWrite()) {
      throw new IOException();
    }
    FileOutputStream stream = null;
    try {
      stream = new FileOutputStream(hintFile);
      XmlHintFiles.writeHintStream(stream, result);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(stream);
    }
    if (hintFile.isFile()) {
      long lastModified = myJarFile.lastModified();
      if (lastModified > YEARS_30) {
        // sane time
        boolean success = hintFile.setLastModified(lastModified);
        if (!success)
          Log.warn("cannot set mtime to " + hintFile);
      }
    }
  }

  private Set<String> scanJar() throws IOException {
    assert Log.debug("scanning " + myJarFile);
    final char fileSeparator = System.getProperty("file.separator", "/").charAt(0);
    Set<String> result = new HashSet<String>();

    ClassLoader loader = createScanClassLoader();

    JarFile jar = new JarFile(myJarFile);
    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      if (entry.isDirectory())
        continue;
      String fileName = entry.getName();
      if (!fileName.endsWith(".class"))
        continue;
      String className = fileName.substring(0, fileName.length() - 6).replace('/', '.').replace('\\', '.').replace(
        fileSeparator, '.');
      assert Log.debug("loading " + className);
      Class clazz = null;
      try {
        clazz = loader.loadClass(className);
      } catch (Throwable e) {
        Log.debug("cannot load class (" + className + ") from jar (" + myJarFile + ")", e);
        continue;
      }

      if (
        clazz != null &&
        ComponentDescriptor.class.isAssignableFrom(clazz) &&
        !clazz.isInterface() &&
        !Modifier.isAbstract(clazz.getModifiers())
      )
        result.add(className);
    }
    return result;
  }

  private ClassLoader createScanClassLoader() throws MalformedURLException {
    ArrayList<URL> urls = new ArrayList<URL>();
    if (myJarFile.getParentFile() == null) {
      urls.add(new URL("file", null, myJarFile.getPath()));
    } else {
      File[] jars = myJarFile.getParentFile().listFiles(new ExtensionFileFilter("jar", false));
      for (int i = 0; i < jars.length; i++)
        urls.add(new URL("file", null, jars[i].getPath()));
    }
    URL[] urlarray = urls.toArray(new URL[urls.size()]);
    ClassLoader loader = new URLClassLoader(urlarray, ComponentConfiguration.class.getClassLoader());
    return loader;
  }


  private InputStream getHintXml() throws FileNotFoundException {
    File xmlFile = getHintFile();
    if (xmlFile == null || !xmlFile.isFile())
      return null;
    if (!xmlFile.canRead()) {
      Log.warn("cannot read hint file (" + xmlFile + ")");
      return null;
    }
    long diff = Math.abs(xmlFile.lastModified() - myJarFile.lastModified());
    if (diff > TIME_EPSILON) {
      Log.debug("ignoring hints (" + xmlFile + ")(" + diff + ")");
      return null;
    }
    return new FileInputStream(xmlFile);
  }

  @Nullable
  private File getHintFile() {
    String name = myJarFile.getName();
    if (!Util.upper(name).endsWith(".JAR"))
      return null;
    name = name.substring(0, name.length() - 4) + ".xml";
    File parent;
    if (myComponentCacheXmlDir == null || !myComponentCacheXmlDir.isDirectory())
      parent = myJarFile.getParentFile();
    else
      parent = myComponentCacheXmlDir;
    return new File(parent, name);
  }

  private InputStream getBundledXml() throws IOException {
    JarFile jar = new JarFile(myJarFile);
    JarEntry entry = jar.getJarEntry(PLATFORM_XML);
    if (entry == null)
      return null;
    return jar.getInputStream(entry);
  }
}
