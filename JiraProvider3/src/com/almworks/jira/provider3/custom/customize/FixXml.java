package com.almworks.jira.provider3.custom.customize;

import com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;

class FixXml extends DefaultHandler {
  public static final String NO_PROBLEM = "Success";
  private SAXException myProblem;
  private String myFirstTag;
  private final String myXml;

  private FixXml(String xml) {
    myXml = xml;
  }

  @Nullable
  public static FixXml fixXml(String xml) {
    SAXParser parser;
    try {
      parser = SAXParserFactory.newInstance().newSAXParser();
    } catch (Exception e) {
      LogHelper.error(e);
      return null;
    }
    FixXml result = new FixXml(xml);
    try {
      parser.parse(new InputSource(new StringReader(xml)), result);
    } catch (SAXException e) {
      result.setProblem(e);
    } catch (IOException e) {
      LogHelper.error(e);
      return null;
    }
    return result;
  }

  @Nullable
  public SAXException getProblem() {
    return myProblem;
  }

  @Nullable
  public String getFixed() {
    if (myProblem != null) return null;
    if (FieldKeysLoader.ROOT_TAG.equals(myFirstTag)) return myXml;
    if (FieldKeysLoader.TAG_FIELD.equals(myFirstTag)) {
      return "<" + FieldKeysLoader.ROOT_TAG + " " + FieldKeysLoader.A_VERSION + "=\"" + FieldKeysLoader.EXPECTED_VERSION + "\">\n" + myXml + "\n</" + FieldKeysLoader.ROOT_TAG + ">";
    }
    LogHelper.error("Unexpected first tag", myFirstTag);
    return null;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    if (!FieldKeysLoader.ROOT_TAG.equals(qName) && !FieldKeysLoader.TAG_FIELD.equals(qName)) throw new SAXException("Wrong tag '" + qName + "'");
    myFirstTag = qName;
    throw new SAXException(NO_PROBLEM);
  }

  public void setProblem(SAXException problem) {
    if (problem == null || NO_PROBLEM.equals(problem.getMessage())) return;
    myProblem = problem;
  }
}
