package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.api.connector.http.ExtractFormParameters;
import com.almworks.util.LocalLog;
import com.almworks.util.Pair;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Const;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dyoma
 */
class FormWrapper {
  private final Element myForm;
  private final MultiMap<String, String> myParameters;
  private final String myName;
  private final ExtractFormParameters myElements;
  private final LocalLog log;

  public FormWrapper(Element form, MultiMap<String, String> parameters, String name, ExtractFormParameters extract) {
    myForm = form;
    myParameters = parameters;
    myName = name;
    myElements = extract;
    log = LocalLog.topLevel("form:" + name);
  }

  @Nullable
  public static FormWrapper find(Document document, String action, boolean extractSubmit) {
    Element form = findFormElement(document, action);
    if (form == null)
      return null;
    return create(form, action, extractSubmit);
  }

  public static Element findFormElement(Document document, String action) {
    Element form = JDOMUtils.searchElement(document.getRootElement(), "form", "action", action);
    if (form == null)
      return null;
    return form;
  }

  private static FormWrapper create(Element form, String action, boolean extractSubmit) {
    ExtractFormParameters extract = new ExtractFormParameters(form, extractSubmit);
    extract.perform();
    MultiMap<String, String> parameters = MultiMap.create(extract.getParameters());
    return new FormWrapper(form, parameters, action, extract);
  }

  @Nullable
  public static FormWrapper find(Document document, boolean extractSubmit, String... actionParts) {
    Pair<Element, String> elementAction = findFormElement(document, actionParts);
    return elementAction != null ? create(elementAction.getFirst(), elementAction.getSecond(), extractSubmit) : null;
  }

  public static Pair<Element, String> findFormElement(Document document, String... actionParts) {
    Iterator<Element> it = JDOMUtils.searchElementIterator(document.getRootElement(), "form");
  IT: while(it.hasNext()) {
      final Element form = it.next();
      final String action = form.getAttributeValue("action");
      if (action != null && !action.isEmpty()) {
        for (final String part : actionParts) {
          if (action.indexOf(part) < 0) {
            continue IT;
          }
        }
        return Pair.create(form, action);
      }
    }
    return null;
  }

  public void setValue(String parameter, String value) {
    myParameters.replaceAll(parameter, value);
  }

  public void setValues(String parameter, String[] values) {
    if (values == null) values = Const.EMPTY_STRINGS;
    myParameters.replaceAll(parameter, Arrays.asList(values));
  }

  public void setNNToStringValue(String parameter, Object value) {
    if (value == null) return;
    setValue(parameter, value.toString());
  }

  public Element getParameterElement(String parameter) {
    return myElements.getElements().getSingle(parameter);
  }

  @Nullable
  public String getValue(String parameter) {
    return myParameters.getSingle(parameter);
  }

  public List<String> getAllValues(String parameter) {
    return myParameters.getAll(parameter);
  }

  public String toString() {
    return "form(" + myName + ")";
  }

  public FormWrapper copy(String name) {
    return new FormWrapper(myForm, MultiMap.create(myParameters), name, myElements);
  }

  public Element getElement() {
    return myForm;
  }

  public MultiMap<String, String> getParameters() {
    return myParameters;
  }

  public Collection<String> getAllParameterNames() {
    return myElements.getElements().keySet();
  }

  public List<String> collectOptions(String parameter) {
    Element select = getParameterElement(parameter);
    if (select == null) {
      log.warning("Missing form parameter:", parameter);
      return Collections.emptyList();
    }
    List<Element> options = JDOMUtils.searchElements(select, "option");
    return options.stream()
      .map(o -> JDOMUtils.getAttributeValue(o, "value", "", false))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }
}
