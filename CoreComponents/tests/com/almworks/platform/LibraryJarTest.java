package com.almworks.platform;

import com.almworks.platform.components.TestComponents;
import com.almworks.util.tests.BaseTestCase;

import java.io.*;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * :todoc:
 *
 * @author sereda
 */
public class LibraryJarTest extends BaseTestCase {
  private static final String PLATFORM_XML = "META-INF/platform.xml";

  private File myJarFile;
  private boolean myAborted;
  private Exception myAbortStack;
  private boolean myWarned;
  private Exception myWarnedStack;

  private LibraryJar myLibJar;
  private static final String INCORRECT_XML = "<library><component class=\"java.lang.String\"/></library>";

  protected void setUp() throws Exception {
    super.setUp();
    myJarFile = File.createTempFile("LibraryJarTest", ".jar");
    myJarFile.deleteOnExit();
    myAborted = false;
    myWarned = false;
    myLibJar = new LibraryJar(myJarFile, null);
  }

  protected void tearDown() throws Exception {
    myLibJar = null;
    myJarFile = null;
    super.tearDown();
  }

  // =================== TEST METHODS =========================

  public void testJarWithBundledInfo() throws Exception {
    writeJar(new Object[][]{
      {PLATFORM_XML, TestComponents.XML},
    });
    checkJar(TestComponents.CLASS_NAMES);
    checkFlags(false);
  }


  public void testJarWithHintFile() throws Exception {
    writeJar();
    myJarFile.setLastModified(myJarFile.lastModified() - 3000);
    writeXmlFile(TestComponents.XML);
    checkJar(TestComponents.CLASS_NAMES);
    checkFlags(false);
  }

  public void testJarWithBundledAndHintFile() throws Exception {
    writeJar(new Object[][]{{PLATFORM_XML, TestComponents.XML}});
    writeXmlFile(INCORRECT_XML);
    checkJar(TestComponents.CLASS_NAMES);
    checkFlags(false);
  }

  public void testHintFileCreation() throws Exception {
    writeJar(TestComponents.CLASSES);
    checkJar(TestComponents.CLASS_NAMES);
    checkFlags(false);
  }

  public void testHintFileOverwriting() throws Exception {
    writeJar(TestComponents.CLASSES);
    writeXmlFile(INCORRECT_XML);
    myJarFile.setLastModified(myJarFile.lastModified() + 13000);
    checkJar(TestComponents.CLASS_NAMES);
    checkFlags(false);
  }

  public void testJarMtime() throws Exception {
    writeJar(TestComponents.CLASSES);
    sleep(1000);
    checkJar(TestComponents.CLASS_NAMES);
    checkFlags(false);
    long timeJar = myJarFile.lastModified();
    long timeXml = getXmlFile().lastModified();
    assertEquals(timeJar, timeXml);
  }

  public void testLateXml() throws Exception {
    writeJar(TestComponents.CLASSES);
    writeXmlFile(INCORRECT_XML);
    myJarFile.setLastModified(myJarFile.lastModified() - 13000);
    checkJar(TestComponents.CLASS_NAMES);
    checkFlags(false);
  }

  // ==================================================================

  private void checkJar(String[] classNames) {
    Set<String> r = myLibJar.getComponents();
    assertTrue(r.size() == classNames.length);
    for (int i = 0; i < TestComponents.CLASS_NAMES.length; i++)
      assertTrue(r.contains(TestComponents.CLASS_NAMES[i]));
  }

  private File getXmlFile() {
    String s = myJarFile.getPath();
    File xmlFile = new File(s.substring(0, s.length() - 4).concat(".xml"));
    return xmlFile;
  }

  private void writeJar() throws IOException {
    writeJar((Object[][]) null);
  }

  private void writeJar(Class[] classes) throws IOException {
    Object[][] files = new Object[classes.length][];
    for (int i = 0; i < files.length; i++) {
      String fileName = TestComponents.CLASSES[i].getName().replace('.', '/').concat(".class");
      files[i] = new Object[]{fileName, TestComponents.CLASSES[i]};
    }
    writeJar(files);
  }

  private void writeJar(Object[][] files) throws IOException {
    JarOutputStream stream = new JarOutputStream(new FileOutputStream(myJarFile));
    if (files != null && files.length > 0) {
      for (int i = 0; i < files.length; i++) {
        String fileName = (String) files[i][0];
        Object fileContent = files[i][1];
        stream.putNextEntry(new JarEntry(fileName));
        if (fileContent instanceof byte[])
          stream.write((byte[]) fileContent);
        else if (fileContent instanceof String)
          stream.write(((String) fileContent).getBytes());
        else if (fileContent instanceof Class)
          stream.write(readClassBytes((Class) fileContent));
        else
          throw new IllegalArgumentException(fileContent.toString());
        stream.closeEntry();
      }
    } else {
      // cannot create completely empty jar :(
      stream.putNextEntry(new JarEntry("jar-is-empty"));
      stream.write("jar-is-empty".getBytes());
      stream.closeEntry();
    }
    stream.close();
  }

  protected void checkFlags(boolean ignoreWarnings) throws Exception {
    if (myAborted)
      throw myAbortStack;
    if (myWarned && !ignoreWarnings)
      throw myWarnedStack;
  }


  public void testCheckJar() throws Exception {
    assertTrue(myLibJar.checkJar());
    checkFlags(true);
  }

  private void writeXmlFile(String xml) throws IOException {
    File xmlFile = getXmlFile();
    FileOutputStream xmlStream = new FileOutputStream(xmlFile);
    new PrintStream(xmlStream).print(xml);
    xmlStream.close();
  }


  private byte[] readClassBytes(Class clazz) throws IOException {
    String className = clazz.getName();
    className = className.substring(className.lastIndexOf('.') + 1);
    InputStream input = clazz.getResourceAsStream(className + ".class");
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    while (true) {
      int n = input.read(buffer);
      if (n <= 0)
        break;
      output.write(buffer, 0, n);
    }
    input.close();
    output.close();
    return output.toByteArray();
  }
}
