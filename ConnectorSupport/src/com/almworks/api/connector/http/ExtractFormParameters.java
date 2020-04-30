package com.almworks.api.connector.http;

import com.almworks.util.collections.MultiMap;
import com.almworks.util.xml.JDOMUtils;
import org.jdom.Element;

import java.util.List;

public class ExtractFormParameters {
  private final Element myForm;
  private final boolean myIncludeSubmitButton;
  private final MultiMap<String, String> myParameters = MultiMap.create();
  private final MultiMap<String, Element> myElements = MultiMap.create();

  public ExtractFormParameters(Element form, boolean includeSubmitButton) {
    myForm = form;
    myIncludeSubmitButton = includeSubmitButton;
  }

  public MultiMap<String, String> getParameters() {
    return myParameters;
  }

  public MultiMap<String, Element> getElements() {
    return myElements;
  }

  public void perform() {
    findInputs();
    extractRequiredFormSelects();
    findTextAreas();
    if (myIncludeSubmitButton) {
      findSubmit();
    }
  }

  private void findSubmit() {
    Element submit = JDOMUtils.searchElement(myForm, "input", "type", "submit");
    if (submit != null) {
      String name = JDOMUtils.getAttributeValue(submit, "name", "", false).trim();
      if (name.length() > 0) {
        String value = JDOMUtils.getAttributeValue(submit, "value", null, true);
        if (value == null) {
          value = JDOMUtils.getTextTrim(submit);
        }
        myParameters.add(name, value);
        myElements.add(name, submit);
      }
    }
  }

  private void findTextAreas() {
    List<Element> list = JDOMUtils.searchElements(myForm, "textarea");
    for (Element textArea : list) {
      String name = JDOMUtils.getAttributeValue(textArea, "name", "", false);
      if (name.length() == 0)
        continue;
      myParameters.add(name, JDOMUtils.getText(textArea));
      myElements.add(name, textArea);
    }
  }

  private void extractRequiredFormSelects() {
    final List<Element> selects = JDOMUtils.searchElements(myForm, "select");
    for (Element selectElement : selects) {
      extractRequiredFormSelect(selectElement);
    }
  }

  private void extractRequiredFormSelect(Element selectElement) {
    String name = JDOMUtils.getAttributeValue(selectElement, "name", null, true);
    if (name != null) {
      boolean multiple = HtmlUtils.isMultipleSelect(selectElement);
      final List<Element> options = JDOMUtils.searchElements(selectElement, "option");
      boolean anySelected = false;
      for (Element optionElement : options) {
        if (optionElement.getAttribute("selected") != null) {
          anySelected = true;
          myParameters.add(name, HtmlUtils.getOptionValue(optionElement));
          if (!multiple) {
            break;
          }
        }
      }
      if (!anySelected && !multiple && options.size() > 0) {
        // browsers submit first option by default
        // http://www.w3.org/TR/html4/interact/forms.html#edef-SELECT
        myParameters.add(name, HtmlUtils.getOptionValue(options.get(0)));
      }
      myElements.add(name, selectElement);
    }
  }

  private void findInputs() {
    final List<Element> elements = JDOMUtils.searchElements(myForm, "input");
    for (int i = 0; i < elements.size(); i++) {
      Element element = elements.get(i);
      String type = JDOMUtils.getAttributeValue(element, "type", "text", false);
      if (isTypeIgnored(type))
        continue;
      if (isNotChecked(element, type))
        continue;
      final String name = JDOMUtils.getAttributeValue(element, "name", null, true);
      if (name != null) {
        String value = JDOMUtils.getAttributeValue(element, "value", "", true);
        myParameters.add(name, value);
        myElements.add(name, element);
      }
    }
  }

  private static boolean isTypeIgnored(String type) {
    return "reset".equalsIgnoreCase(type) || "image".equalsIgnoreCase(type) || "button".equalsIgnoreCase(type) ||
      "file".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type);
  }

  private static boolean isNotChecked(Element element, String type) {
    return ("radio".equalsIgnoreCase(type) || "checkbox".equalsIgnoreCase(type)) &&
      JDOMUtils.getAttributeValue(element, "checked", null, false) == null;
  }
}
