package com.almworks.jira.provider3.sync.download2;

import com.almworks.restconnector.json.sax.LocationHandler;
import com.almworks.util.io.IOUtils;
import junit.framework.Assert;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class TestResources {
  private final String myRoot;
  private final ClassLoader myLoader;

  public TestResources(String root, ClassLoader loader) {
    myRoot = root;
    myLoader = loader;
  }

  public static TestResources create(Class<?> aClass, String root) {
    return new TestResources(root, aClass.getClassLoader());
  }

  public void parseJsonResource(String path, LocationHandler handler) throws IOException, ParseException {
    InputStreamReader reader = getReader(path);
    try {
      JSONParser parser = new JSONParser();
      parser.parse(reader, new LocationHandler.ContentAdapter(handler));
    } finally {
      reader.close();
    }
  }

  private InputStreamReader getReader(String path) throws UnsupportedEncodingException {
    return new InputStreamReader(getStream(path), "UTF-8");
  }

  public InputStream getStream(String path) {
    return myLoader.getResourceAsStream(myRoot + path);
  }

  public String loadText(String path) throws IOException {
    InputStreamReader reader = getReader(path);
    try {
      return IOUtils.readAll(reader);
    } finally {
      reader.close();
    }
  }

  public void assertTextEquals(String expectedPath, String actual) throws IOException {
    Assert.assertEquals(loadText(expectedPath), actual);
  }

  public Object loadJson(String path) throws IOException, ParseException {
    return new JSONParser().parse(loadText(path));
  }
}
