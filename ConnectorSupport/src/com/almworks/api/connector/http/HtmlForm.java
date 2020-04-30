package com.almworks.api.connector.http;

import com.almworks.util.Pair;
import com.almworks.util.xml.JDOMUtils;
import org.jdom.Element;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates &lt;form&gt; html element.
 */
public class HtmlForm {
  private final Element myElement;
  private Method myMethod;
  private List<Pair<String, String>> myRequiredFormParameters;

  public HtmlForm(Element element) {
    assert element.getName().equalsIgnoreCase("form");
    myElement = element;
  }

  public synchronized Method getMethod() {
    if (myMethod == null) {
      String method = JDOMUtils.getAttributeValue(myElement, "method", null, false);
      if ("post".equalsIgnoreCase(method)) {
        myMethod = Method.POST;
      } else {
        myMethod = Method.GET;
      }
    }
    return myMethod;
  }

  public List<Pair<String, String>> getRequiredFormParameters() {
    if (myRequiredFormParameters == null) {
      myRequiredFormParameters = HtmlUtils.extractDefaultFormParameters(myElement).toPairList();
      myRequiredFormParameters = Collections.unmodifiableList(myRequiredFormParameters);
    }
    return myRequiredFormParameters;
  }

  public Element getElement() {
    return myElement;
  }

  public static enum Method {
    POST,
    GET
  }
}
