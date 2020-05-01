package com.almworks.jira.provider3.sync.download2.details;

import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.sax.JSONCollector;
import com.almworks.restconnector.json.sax.LocationHandler;
import com.almworks.restconnector.json.sax.PeekObjectEntry;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashSet;

public class CollectOperations {
  private static final JSONKey<String> ID = JSONKey.text("id");

  private final LocationHandler myIssueHandler = PeekObjectEntry.objectEntry("operations", PeekObjectEntry.objectEntry("linkGroups", new LocationHandler() {
    @Override
    public void visit(Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
      doVisit(what, start, key, value);
    }
  }));
  private final LocationHandler myLinkHandler = JSONCollector.objectConsumer(new Procedure<JSONObject>() {
    @Override
    public void invoke(JSONObject link) {
      String id = ID.getValue(link);
      if (id == null) return; // Some links does not have ID (example: reference to XML: /si/jira.issueviews:issue-xml/<issueKey>/<issueKey>.xml
      if (!myIds.add(id)) LogHelper.error("Duplicated id", id);
    }
  });
  private int myInLinks = 0;
  private final HashSet<String> myIds = Collections15.hashSet();

  public LocationHandler getIssueHandler() {
    return myIssueHandler;
  }

  public void doVisit(LocationHandler.Location what, boolean start, @Nullable String key, @Nullable Object value) throws ParseException, IOException {
    if (what == LocationHandler.Location.TOP && start) {
      myInLinks = 0;
      return;
    }
    if (start && what == LocationHandler.Location.ENTRY && "links".equals(key)) {
      myInLinks = 1;
      return;
    } else if (myInLinks == 0) return;
    else if (start && what != LocationHandler.Location.PRIMITIVE) myInLinks++;
    if (myInLinks >= 3) {
      if (myInLinks == 3 && start) {
        myLinkHandler.visit(LocationHandler.Location.TOP, true, null, null);
//        System.out.println("************ START");
      }
      myLinkHandler.visit(what, start, key, value);
//      System.out.println("Processing: " + what + " " + start + " " + key + " " + value);
      if (myInLinks == 3 && !start) {
        myLinkHandler.visit(LocationHandler.Location.TOP, false, null, null);
//        System.out.println("************ END");
      }
    }
    if (!start) myInLinks--;
  }
}
