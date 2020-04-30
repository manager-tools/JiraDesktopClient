package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.ConnectorException;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.services.JiraPatterns;
import com.almworks.jira.provider3.sync.download2.rest.JqlSearch;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.jql.JqlQuery;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.util.LogHelper;
import com.almworks.util.xml.JDOMUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class LoadCommentVisibility {
  private static final JSONKey<Integer> ISSUE_ID = JSONKey.integer("id");
  private static final JSONKey<String> ISSUE_KEY = JSONKey.text("key");

  /**
   * Extracts visibility groups (skips project roles) from Add Comment page
   * @return null if no visibility selector found<br>
   * empty list if only project roles are allowed for comment visibility<br>
   * not empty list of groups if groups are allowed for comment visibility
   */
  public static List<String> loadCommentVisibilityGroups(RestSession session) throws ConnectorException {
    // Load "any issue" ID-KEY
    JSONObject issue = new JqlSearch(JqlQuery.EMPTY).addFields("key").querySingle(session);
    if (issue == null) return null;
    Integer id = ISSUE_ID.getValue(issue);
    String key = ISSUE_KEY.getValue(issue);
    if (id == null || key == null) {
      LogHelper.error("Failed to get issue ID/KEY", id, key);
      throw new JiraInternalException("Failed to load comments visibility");
    }
    Document page = loadAddCommentPage(id.toString(), key, session);
    return processAddComment(page);
  }

  public static Document loadAddCommentPage(String issueId, String issueKey, RestSession session) throws ConnectorException {
    // this call is for the warnings
    JiraPatterns.canBeAnIssueKey(issueKey);

    String addCommentUrl = "secure/AddComment!default.jspa?id=" + issueId + "&decorator=none";
    RestResponse response = session.doGet(addCommentUrl, RequestPolicy.SAFE_TO_RETRY);
    response.ensureSuccessful();
    return response.getHtml();
  }

  @Nullable
  private static List<String> processAddComment(Document page) {
    if (page == null) return null;
    Element select = JDOMUtils.searchElement(page.getRootElement(), "select", "name", "commentLevel");
    if (select == null) {
      return null;
    }
    Iterator<Element> ii = JDOMUtils.searchElementIterator(select, "option");
    ArrayList<String> groups = Collections15.arrayList();
    while (ii.hasNext()) {
      Element option = ii.next();
      String value = JDOMUtils.getAttributeValue(option, "value", "", true);
      if (value.length() == 0) continue;
      String name = JDOMUtils.getText(option);
      if (!value.startsWith("group:")) continue;
      String id = value.substring(6);
      if (!id.equalsIgnoreCase(name)) {
        Log.warn("suspicious group id [" + id + "][" + name + "]");
      }
      groups.add(id);
    }
    return groups;
  }
}
