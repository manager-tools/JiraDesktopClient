package com.almworks.jira.provider3.sync.download2.details;

import com.almworks.api.connector.ConnectorException;
import com.almworks.integers.IntArray;
import com.almworks.jira.connector2.JiraInternalException;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.JqlSearch;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.jql.JqlQuery;
import com.almworks.restconnector.json.sax.CompositeHandler;
import com.almworks.restconnector.json.sax.JSONCollector;
import com.almworks.restconnector.json.sax.LocationHandler;
import com.almworks.restconnector.json.sax.PeekArrayElement;
import com.almworks.util.LogHelper;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RestQueryPager {
  @NotNull
  private final JqlQuery myJql;
  /**
   * HTTP response status codes that are treated as end of query. Otherwise failure exception is thrown.
   */
  private final IntArray myNoResultCodes = new IntArray();
  private String[] myFields = null;
  private int myTotal = -1;
  private int myMaxResult = -1;
  private int myStart;

  public RestQueryPager(@NotNull JqlQuery jql) {
    myJql = jql;
  }

  public static RestQueryPager allFields(@NotNull JqlQuery jql) {
    RestQueryPager pager = new RestQueryPager(jql);
    pager.setFields(new String[]{"*all"});
    return pager;
  }

  /**
   * If not null specified used to set fields via {@link JqlSearch#addFields(String...)}
   */
  public void setFields(@Nullable String[] fields) {
    myFields = fields;
  }

  public void setFields(ServerFields.Field ... fields) {
    String[] strIds = new String[fields.length];
    for (int i = 0; i < fields.length; i++) strIds[i] = fields[i].getJiraId();
    setFields(strIds);
  }

  private JqlSearch createSearch() {
    JqlSearch search = new JqlSearch(myJql);
    if (myFields != null) search.addFields(myFields);
    return search;
  }

  /**
   * @return total number of issues stratified the query or -1 if no total is loaded yet.
   */
  public int getTotal() {
    return myTotal;
  }

  /**
   * @see #myNoResultCodes
   */
  public void addNoResultCode(int httpCode) {
    myNoResultCodes.add(httpCode);
  }

  /**
   * Set next page start
   */
  public void setStart(int start) {
    myStart = start;
  }

  /**
   * Sets desired max query result. It should no be too big because of it may lead to JIRA failure.
   * @param maxResult
   */
  public void setMaxResult(int maxResult) {
    myMaxResult = maxResult;
  }

  /**
   * @param issueHandler handler that consumes issues (elements of "issues" array)
   * @return page size. This can be equal to number of loaded issues or can be greater if last page is loaded.<br>
   * May return 0 if nothing is actually loaded and so page size is not known
   */
  public int loadNext(RestSession session, LocationHandler issueHandler) throws ConnectorException {
    JqlSearch search = createSearch();
    if (myMaxResult < 0) search.setDefaultMaxResult();
    else search.setMaxResult(myMaxResult);
    search.setStart(myStart);
    RestResponse response = search.request(session);
    if (!response.isSuccessful()) {
      int statusCode = response.getStatusCode();
      if (myNoResultCodes.contains(statusCode)) {
        myTotal = myStart; // Set query ended state
        return 0;
      }
      RestResponse.ErrorResponse errorResponse = response.createErrorResponse();
      if (statusCode == 400) {
        ConnectorException problem = search.maybeInaccessibleProject(session, errorResponse);
        if (problem != null) throw problem;
      }
      LogHelper.warning("Query failed", statusCode, response.getLastUrl(), search);
      throw errorResponse.toException();
    }
    JSONCollector getTotal = new JSONCollector(null);
    JSONCollector getMaxResults = new JSONCollector(null);
    JSONCollector getStartAt = new JSONCollector(null);
    response.parseJSON(new CompositeHandler(
      getStartAt.peekObjectEntry("startAt"),
      getMaxResults.peekObjectEntry("maxResults"),
      getTotal.peekObjectEntry("total"),
      PeekArrayElement.entryArray("issues", issueHandler)
    ));
    Integer maxResults = getMaxResults.getInteger();
    Integer total = getTotal.getInteger();
    Integer startAt = getStartAt.getInteger();
    if (maxResults == null || total == null || startAt == null) {
      LogHelper.error("Missing result data", maxResults, total, startAt);
      throw new JiraInternalException("Failed to load query. Cannot understand server reply.");
    }
    myTotal = total;
    LogHelper.assertError(startAt == myStart, "Wrong start", myStart, startAt);
    return maxResults;
  }

  /**
   * Loads whole query from current start up to end or up to {@link #myMaxResult} if positive value is specified<br>
   * When optional progress is provided informs it about progress. If an optional activity template is provided - shows current loading state.
   * @param issueHandler issues consumer same as in {@link #loadNext(com.almworks.restconnector.RestSession, com.almworks.restconnector.json.sax.LocationHandler)}
   * @param firstActivity progress activity message when total query size is not known (happens before first page is loaded)
   * @param nextActivity progress activity pattern when total number of issues is known. arg1 - current start, arg2 - {@link #getTotal() total count} (always not negative)
   * @see #loadNext(com.almworks.restconnector.RestSession, com.almworks.restconnector.json.sax.LocationHandler)
   */
  public void loadAll(RestSession session, LocationHandler issueHandler, @Nullable ProgressInfo progress, @Nullable LocalizedAccessor.Value firstActivity, @Nullable LocalizedAccessor.Message2 nextActivity) throws ConnectorException {
    while (true) {
      if (progress != null) {
        String message;
        if (myTotal >= 0 && nextActivity != null) message = nextActivity.formatMessage(String.valueOf(myStart), String.valueOf(myTotal));
        else if (myTotal < 0 && firstActivity != null) message = firstActivity.create();
        else message = null;
        if (message != null) progress.startActivity(message);
        else progress.checkCancelled();
      }
      int maxResults = loadNext(session, issueHandler);
      if (progress != null) {
        int left = myTotal - myStart;
        (left == 0 ? progress : progress.spawn(Math.min(1.0, ((double) maxResults)/ left))).setDone();
      }
      myStart += maxResults;
      if (myStart >= myTotal) break;
      if (myMaxResult > 0 && maxResults >= myMaxResult) break;
      if (maxResults <= 0) {
        LogHelper.error("No issues loaded", myStart, myMaxResult, myTotal, maxResults);
        break;
      }
    }
  }
}
