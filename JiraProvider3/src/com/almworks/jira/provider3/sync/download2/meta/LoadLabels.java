package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.CannotParseException;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.custom.fieldtypes.enums.multi.LabelsEnumFieldType;
import com.almworks.jira.provider3.sync.download2.details.CustomFieldsSchema;
import com.almworks.jira.provider3.sync.download2.meta.core.LoadMetaContext;
import com.almworks.jira.provider3.sync.download2.meta.core.MetaOperation;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LoadLabels extends MetaOperation {
  private static final String LABELS_REPORT = "com.atlassian.jira.plugin.system.project:labels-heatmap-panel";
  private static final LocalizedAccessor.Value M_LOAD_LABELS = LoadRestMeta.I18N.getFactory("progress.meta.loadLabels");
  private final Map<String, Set<String>> myLabels = Collections15.hashMap();

  public LoadLabels() {
    super(5);
  }

  @Override
  public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress, LoadMetaContext context) throws JiraInternalException, CancelledException {
    ProjectsAndTypes projects = context.getData(LoadRestMeta.PROJECTS);
    progress.startActivity(M_LOAD_LABELS.create());
    ProgressInfo[] progressInfos = progress.split(projects.getProjectCount());
    Set<Integer> filter = context.getDataOrNull(LoadRestMeta.PROJECT_FILTER);
    for (int i = 0; i < projects.getProjectCount(); i++) {
      if (filter == null || filter.contains(projects.getProjectIdAt(i))) {
        String key = projects.getProjectKeyAt(i);
        String name = projects.getDisplayableProjectNameAt(i);
        if (key != null) {
          progressInfos[i].startActivity(name);
          loadLabelsForProject(session, key);
        }
      }
      progressInfos[i].setDone();
    }
    for (Map.Entry<String, Set<String>> entry : myLabels.entrySet()) LabelsEnumFieldType.storeLabels(transaction, entry.getKey(), entry.getValue());
    progress.setDone();
  }

  private void loadLabelsForProject(RestSession session, String projectKey) {
    String path = String.format("browse/%s?selectedTab=%s&labels.view=all&decorator=none", projectKey, LABELS_REPORT);
    ArrayList<String> fields = Collections15.arrayList();
    String firstField = null;
    try {
      firstField = loadLabels(session, path, fields);
    } catch (ConnectorException e) {
      LogHelper.warning("Load labels failed", projectKey, e.getMessage());
      return;
    }
    for (String field : fields) {
      if (Util.equals(firstField, field)) continue;
      try {
        loadLabels(session, path + "&selected.field=" + field, null);
      } catch (ConnectorException e) {
        LogHelper.warning("Load labels field failed", field, projectKey, e);
      }
    }
  }

  private static final Pattern FIELD_HREF = Pattern.compile("^\\/browse\\/.*\\?selectedTab=.*labels-heatmap-panel.*\\&selected.field=([^\\&]+)");
  private static final Pattern LABEL_HREF = Pattern.compile("^\\/secure\\/IssueNavigator\\.jspa\\?reset=true\\&jqlQuery=([^\\&\\s]+)");

  /**
   * Processes labels report. <br>
   * Collects all other labels fields (if otherFields parameter is not null).
   * Collects labels for current selected fields, and returns field ID.
   * @return processed field or null if no field processed or no labels detected
   */
  private  String loadLabels(RestSession session, String path, @Nullable Collection<String> otherFields) throws ConnectorException {
    RestResponse response = session.doGet(path, RequestPolicy.SAFE_TO_RETRY);
    response.ensureSuccessful();
    Document html;
    try {
      html = response.getHtml();
    } catch (CannotParseException e) {
      return null;
    }
    Element div = JDOMUtils.searchElement(html.getRootElement(), "div", "id", "project-tab"); // JIRA 5.x HTML
    if (div == null) div = JDOMUtils.searchElement(html.getRootElement(), "section", "id", "project-tab"); // JIRA 6.x HTML
    if (div == null) return null;
    Iterator<Element> it = JDOMUtils.searchElementIterator(div, "a");
    String labelField = null;
    boolean foundLabels = false;
    while(it.hasNext()) {
      final Element a = it.next();
      final String href = JDOMUtils.getAttributeValue(a, "href", null, true);
      if(href == null) continue;

      if(!foundLabels) foundLabels = href.contains(LABELS_REPORT);

      if(otherFields != null) {
        final Matcher m = FIELD_HREF.matcher(href);
        if(m.find()) {
          otherFields.add(m.group(1));
          continue;
        }
      }

      final Matcher m = LABEL_HREF.matcher(href);
      if(m.find()) {
        LabelQuery query = LabelQuery.parse(m.group(1));
        if (query == null) continue;

        Set<String> labels;
        final String cfid = query.myField;
        if (labelField != null && !labelField.equals(cfid)) {
          LogHelper.warning("inconsistent labels report", cfid, labelField);
          continue;
        }
        labelField = cfid;
        labels = myLabels.get(cfid);
        if(labels == null) {
          labels = Collections15.linkedHashSet();
          myLabels.put(cfid, labels);
        }
        labels.add(query.myLabel);
      }
    }
    return foundLabels ? labelField : null;
  }

  private static class LabelQuery {
    private static final Pattern LABEL_QUERY = Pattern.compile("project\\s*=\\s*\\d+\\s+AND\\s+(\\S+)\\s*=\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private final String myField;
    private final String myLabel;

    public LabelQuery(String field, String label) {
      myField = field;
      myLabel = label;
    }

    @Nullable
    public static LabelQuery parse(String jql) {
      final String decoded;
      try {
        decoded = URLDecoder.decode(jql, "UTF-8");
      } catch(UnsupportedEncodingException e) {
        Log.debug(e);
        return null;
      }
      Matcher m = LABEL_QUERY.matcher(decoded);
      if (!m.find()) return null;
      String fieldId = m.group(1);
      Matcher idMatcher = ServerCustomField.JQL_ID_PATTERN.matcher(fieldId);
      if (idMatcher.matches()) fieldId = CustomFieldsSchema.CUSTOMFIELD_PREFIX + idMatcher.group(1);
      return new LabelQuery(fieldId, unescape(unquote(m.group(2), "\"", "\"")));
    }

    private static String unescape(String value) {
      return value.replace("\\\\", "\\").replace("\\\"", "\"");
    }

    private static String unquote(String value, String quote1, String quote2) {
      if(value.startsWith(quote1) && value.endsWith(quote2)) {
        return value.substring(quote1.length(), value.length() - quote2.length());
      }
      return value;
    }
  }
}

