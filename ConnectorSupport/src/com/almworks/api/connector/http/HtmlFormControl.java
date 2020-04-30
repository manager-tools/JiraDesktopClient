package com.almworks.api.connector.http;

import com.almworks.util.xml.JDOMUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

public enum HtmlFormControl {
  TEXT_FIELD("input", "type", "text"),
  TEXT_AREA("textarea", null, null),
  DROP_DOWN("select", null, null)
    {
      public boolean matches(Element e) {
        if (!super.matches(e))
          return false;
        return !HtmlUtils.isMultipleSelect(e);
      }},
  LIST("select", "multiple", null)
    {
      public boolean matches(Element e) {
        if (!super.matches(e))
          return false;
        return HtmlUtils.isMultipleSelect(e);
      }
    },
  CHECKBOX("input", "type", "checkbox"),
  RADIO_BUTTON("input", "type", "radio"),
  BUTTON("input", "type", "button"),
  SUBMIT_BUTTON("input", "type", "submit"),
  RESET_BUTTON("input", "type", "reset"),;

  @NotNull
  private final String myTag;

  @Nullable
  private final String myAttribute;

  @Nullable
  private final String myAttributeValue;

  HtmlFormControl(String tag, String attribute, String attributeValue) {
    myTag = tag;
    myAttribute = attribute;
    myAttributeValue = attributeValue;
  }

  @NotNull
  public String getTag() {
    return myTag;
  }

  @Nullable
  public String getAttribute() {
    return myAttribute;
  }

  @Nullable
  public String getAttributeValue() {
    return myAttributeValue;
  }

  @Nullable
  public Element findOnForm(Element form, @Nullable String name) {
    Iterator<Element> ii = JDOMUtils.searchElementIterator(form, myTag);
    while (ii.hasNext()) {
      Element e = ii.next();
      if (matches(e)) {
        if (name == null || name.equals(JDOMUtils.getAttributeValue(e, "name", null, true))) {
          return e;
        }
      }
    }
    return null;
  }

  public boolean matches(Element e) {
    if (e == null)
      return false;
    if (!myTag.equalsIgnoreCase(e.getName()))
      return false;
    if (myAttribute != null) {
      String value = JDOMUtils.getAttributeValue(e, myAttribute, null, true);
      if (value == null)
        return false;
      if (myAttributeValue != null) {
        if (!myAttributeValue.equalsIgnoreCase(value)) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean isSelect() {
    return this == DROP_DOWN || this == LIST;
  }
}
