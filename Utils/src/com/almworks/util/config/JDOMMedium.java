package com.almworks.util.config;

import com.almworks.util.collections.Convertor;
import org.jdom.Element;

import java.util.List;

/**
 * @author : Dyoma
 */
class JDOMMedium extends JDOMReadonlyMedium implements Medium {

  public JDOMMedium(Element element) {
    super(element, new Convertor<Element, Medium>() {
      public Medium convert(Element value) {
        return new JDOMMedium(value);
      }
    });
  }

  public void setSettings(String unencodedName, List<String> values) {
    String settingName = XMLWriter.encode(unencodedName);
    JDOMReadonlyMedium.JDOMSubMedium<String> settings = getSettings();
    settings.getChildren(unencodedName).clear();
    for (String value : values) {
      Element child = new Element(settingName);
      if (value.length() == 0)
        child.setAttribute(EMPTY_SETTING, "");
      child.setText(value);
      getElement().addContent(child);
    }
  }

  public Medium createSubset(String subsetName) {
    subsetName = XMLWriter.encode(subsetName);
    Element element = new Element(subsetName);
    getElement().addContent(element);
    return new JDOMMedium(element);
  }

  public void removeMe() {
    Element parent = getElement().getParentElement();
    assert parent != null;
    parent.removeContent(getElement());
  }

  public void clear() {
    getElement().removeContent();
  }
}
