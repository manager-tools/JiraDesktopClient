package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.jira.provider3.remotedata.issue.fields.IssueFieldValue;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFields;
import com.almworks.jira.provider3.remotedata.issue.fields.IssueFormValue;
import com.almworks.restconnector.operations.RestServerInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Collections15;
import org.jdom.Element;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FormParameters {
  private static final int P_GENERIC = 0;
  private static final int P_CASCADE = 1;
  private final FormWrapper myForm;
  private final Map<String, Integer> myParams;
  private final RestServerInfo myServerInfo;

  private FormParameters(FormWrapper form, Map<String, Integer> params, RestServerInfo serverInfo) {
    myForm = form;
    myParams = params;
    myServerInfo = serverInfo;
  }

  private static final Pattern CASCADE_CHILD = Pattern.compile("([^:]*):1");
  public static FormParameters create(FormWrapper form, Set<String> ignoreParams, RestServerInfo serverInfo) {
    HashMap<String, Integer> paramTypes = Collections15.hashMap();
    for (String param : form.getAllParameterNames()) {
      if (ignoreParams.contains(param)) continue;
      Matcher m = CASCADE_CHILD.matcher(param);
      if (m.matches()) {
        String key = m.group(1);
        paramTypes.put(key, P_CASCADE);
      } else if (!paramTypes.containsKey(param)) paramTypes.put(param, P_GENERIC);
    }
    return new FormParameters(form, paramTypes, serverInfo);
  }

  public Collection<String> getParameters() {
    return myParams.keySet();
  }

  public boolean isRequired(String param) {
    Element element = myForm.getParameterElement(param);
    if (element == null) return false;
    Element label = JDOMUtils.searchElement(myForm.getElement(), "label", "for", param);
    return label != null && JDOMUtils.searchElement(label, "span", "class", "required") != null;
  }

  /**
   * @return true if a value for param is found and it has change. In this case updated value is set into form
   */
  public boolean trySetValue(String param, Collection<IssueFieldValue> values) {
    Integer type = myParams.get(param);
    if (type == null) {
      LogHelper.error("Unknown parameter", param);
      return false;
    }
    IssueFormValue value = IssueFields.findFormValue(values, param);
    if (value == null) return false;
    if (!value.isChanged()) return false;
    String[] update = value.getFormValue(myServerInfo);
    if (P_GENERIC == type) myForm.setValues(param, update);
    else if (P_CASCADE == type) {
      if (update.length != 2) {
        LogHelper.error("Wrong cascade value", Arrays.asList(update));
        return false;
      }
      myForm.setValue(param, update[0]);
      myForm.setValue(param + ":1", update[1]);
    }
    return true;
  }
}
