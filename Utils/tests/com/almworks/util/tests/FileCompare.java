package com.almworks.util.tests;

import com.almworks.util.collections.Equality;
import com.almworks.util.files.FileUtil;
import com.almworks.util.text.LineTokenizer;
import com.almworks.util.text.TextUtil;
import junit.framework.Assert;
import junit.framework.ComparisonFailure;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.*;
import java.util.List;

/**
 * @author : Dyoma
 */
public class FileCompare {
  private final ClassLoader myClassLoader;
  private final Equality<String> myStringComparison;
  private String myCharsetName = "ISO-8859-1";
  private String myPathPrefix = "";
  private String mySourcePrefix = "";

  public FileCompare(ClassLoader classLoader) {
    this(classLoader, Equality.GENERAL);
  }

  public FileCompare(ClassLoader classLoader, Equality<String> stringComparison) {
    myClassLoader = classLoader;
    myStringComparison = stringComparison;
  }

  public void compareText(String fileName, Element actualElement) {
    Document document = new Document(actualElement);
    XMLOutputter outputter = new XMLOutputter();
    Format format = Format.getRawFormat();
    format.setLineSeparator(TextUtil.LINE_SEPARATOR);
    outputter.setFormat(format);
    StringWriter stringWriter = new StringWriter();
    try {
      outputter.output(document, stringWriter);
    } catch (IOException e) {
      fail(e);
    }
    String xmlText = stringWriter.toString();
    compareText(fileName, xmlText);
  }

  public void compareText(String fileName, String actualText) {
    fileName = getFullFileName(fileName);
    try {
      String expected = new String(FileUtil.loadResource(fileName, myClassLoader), myCharsetName);
      if (!myStringComparison.areEqual(expected, actualText)) {
        File sourceFile = new File(mySourcePrefix + fileName);
        if (!sourceFile.exists())
          System.err.println("Source file not found: " + sourceFile.toString());
        throw new ComparisonFailure("", expected, actualText);
//        throw new FileComparisonFailure("", expected, actualText, sourceFile.toString());
      }
    } catch (IOException e) {
      fail(e);
    }
  }

  private String getFullFileName(String fileName) {
    return myPathPrefix + fileName;
  }

  public void setCharsetName(String charsetName) {
    myCharsetName = charsetName;
  }

  public void setSourcePrefix(String sourcePrefix) {
    mySourcePrefix = normalizePathPrefix(sourcePrefix);
  }

  public void setPathPrefix(String pathPrefix) {
    myPathPrefix = normalizePathPrefix(pathPrefix);
  }

  private String normalizePathPrefix(String pathPrefix) {
    if (pathPrefix == null)
      pathPrefix = "";
    pathPrefix = pathPrefix.replace('\\', '/');
    if (!pathPrefix.endsWith("/"))
      pathPrefix += "/";
    return pathPrefix;
  }

  private static void fail(Exception e) {
    Assert.fail(e.getMessage());
  }

  /**
   * For test data creation purpose only
   * @param fileName name of test data file to create
   * @param bytes test data
   * @throws IOException
   */
  public void writeFile(String fileName, byte[] bytes) throws IOException {
    FileOutputStream stream = new FileOutputStream(new File(mySourcePrefix + getFullFileName(fileName)));
    stream.write(bytes);
    stream.close();
  }

  public InputStream getStream(String fileName) {
    return myClassLoader.getResourceAsStream(getFullFileName(fileName));
  }

  public static final Equality<String> NO_LINE_SEPARATORS = new Equality<String>() {
    public boolean areEqual(String s, String s1) {
      List<String> lines1 = LineTokenizer.getLines(s);
      List<String> lines2 = LineTokenizer.getLines(s1);
      return lines1.equals(lines2);
    }
  };
}
