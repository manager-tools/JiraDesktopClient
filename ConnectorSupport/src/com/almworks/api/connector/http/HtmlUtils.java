package com.almworks.api.connector.http;

import com.almworks.util.Pair;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.commons.Condition;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Collections15;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.xerces.xni.parser.XMLDocumentFilter;
import org.cyberneko.html.parsers.DOMParser;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class HtmlUtils {
  public static final Convertor<Pair<String, String>, NameValuePair> PAIR_TO_NVP =
    new Convertor<Pair<String, String>, NameValuePair>() {
      public NameValuePair convert(Pair<String, String> pair) {
        return new NameValuePair(pair.getFirst(), pair.getSecond());
      }
    };

  private HtmlUtils() {
  }

  public static HtmlForm findForm(Element html, Condition<HtmlForm> condition) {
    List<Element> forms = JDOMUtils.searchElements(html, "form");
    for (Element e : forms) {
      HtmlForm form = new HtmlForm(e);
      if (condition.isAccepted(form))
        return form;
    }
    return null;
  }

  public static MultiMap<String, String> extractDefaultFormParameters(Element form) {
    return extractDefaultFormParameters(form, false);
  }

  public static MultiMap<String, String> extractDefaultFormParameters(Element form, boolean includeSubmitButton) {
    ExtractFormParameters extract = new ExtractFormParameters(form, includeSubmitButton);
    extract.perform();
    return extract.getParameters();
  }

  public static List<String> getSelectOptionValues(Element select) {
    assert "select".equalsIgnoreCase(select.getName());
    List<String> result = Collections15.arrayList();
    Iterator<Element> ii = JDOMUtils.searchElementIterator(select, "option");
    while (ii.hasNext()) {
      String value = getOptionValue(ii.next());
      if (value != null) {
        result.add(value);
      }
    }
    return result;
  }

  public static boolean isMultipleSelect(Element selectElement) {
    String multiple = JDOMUtils.getAttributeValue(selectElement, "multiple", null, false);
    return multiple != null && !"false".equalsIgnoreCase(multiple);
  }

  public static String getOptionValue(Element optionElement) {
    String value = JDOMUtils.getAttributeValue(optionElement, "value", null, true);
    if (value == null) {
      value = JDOMUtils.getText(optionElement);
    }
    return value;
  }

  public static boolean hasCssClass(Element element, String className) {
    String[] classes = getCssClasses(element);
    if (classes == null)
      return false;
    for (String elementClass : classes) {
      if (elementClass.equals(className))
        return true;
    }
    return false;
  }

  @Nullable
  public static String[] getCssClasses(Element element) {
    String classAttribute = JDOMUtils.getAttributeValue(element, "class", null, false);
    if (classAttribute == null)
      return null;
    return classAttribute.split("\\s+");
  }

  @NotNull
  public static Document buildHtmlDocument(InputSource content) throws SAXException, IOException {
    DOMParser parser = prepareNeko();
    try {
      parser.parse(content);
    } catch (Exception e) {
      if (e instanceof SAXException)
        throw (SAXException) e;
      if (e instanceof IOException)
        throw (IOException) e;
      throw new SAXException("HTML parsing error: " + e.getMessage(), e);
    }
    org.w3c.dom.Document w3cDoc = parser.getDocument();
    DOMBuilder domBuilder = new DOMBuilder();
    Document document;
    try {
      document = domBuilder.build(w3cDoc);
    } catch (Exception e) {
      throw new SAXException("HTML parsing error: " + e.getMessage(), e);
    }
//    ALMWorksHack.clearThreadData();
    return document;
  }

  static DOMParser prepareNeko() throws SAXException {
    DOMParser domParser = new DOMParser();
    domParser.setFeature("http://cyberneko.org/html/features/scanner/notify-builtin-refs", true);
    // must not be shared
    domParser.setProperty("http://cyberneko.org/html/properties/filters", new XMLDocumentFilter[] {new LightPurifier()});
    return domParser;
  }

  public static class NameAndCssClass extends Condition<Element> {
    private final String myName;
    private final String myClass;

    public NameAndCssClass(String name, String aClass) {
      myName = name;
      myClass = aClass;
    }

    @Override
    public boolean isAccepted(Element e) {
      return myName.equalsIgnoreCase(e.getName()) && hasCssClass(e, myClass);
    }
  }
}
