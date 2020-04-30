package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.download2.process.util.ProgressInfo;
import com.almworks.jira.provider3.sync.download2.rest.*;
import com.almworks.jira.provider3.sync.schema.ServerPriority;
import com.almworks.jira.provider3.sync.schema.ServerResolution;
import com.almworks.jira.provider3.sync.schema.ServerStatus;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.List;

class LoadGenericStatics {
  private static final List<Operation> OPERATIONS = Collections15.arrayList(
    new Operation("priorities") {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress) throws ConnectorException, ParseException {
        progress.startActivity("priorities");
        StoreIterator it = new StoreIterator(transaction, ServerPriority.TYPE, ServerPriority.ID, JRPriority.ID, RestOperations.priorities(session));
        it.managerDeleteBag();
        Pair<JSONObject,EntityHolder> pair;
        while ((pair = it.next()) != null) {
          JSONObject object = pair.getFirst();
          EntityHolder holder = pair.getSecond();
          holder.setNNValue(ServerPriority.ORDER, it.getIndex());
          holder.setNNValue(ServerPriority.NAME, JRPriority.NAME.getValue(object));
          holder.setNNValue(ServerPriority.ICON_URL, JRPriority.ICON.getValue(object));
          holder.setNNValue(ServerPriority.COLOR, JRPriority.COLOR.getValue(object));
          holder.setNNValue(ServerPriority.DESCRIPTION, JRPriority.DESCRIPTION.getValue(object));
          holder.setValue(ServerPriority.ONLY_IN_PROJECTS, null);
        }
        progress.setDone();
      }
    },
    new Operation("resolutions") {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress) throws ConnectorException, ParseException {
        progress.startActivity("resolutions");
        StoreIterator it = new StoreIterator(transaction, ServerResolution.TYPE, ServerResolution.ID, JRResolution.ID, RestOperations.resolutions(session));
        it.managerDeleteBag();
        Pair<JSONObject,EntityHolder> pair;
        while ((pair = it.next()) != null) {
          JSONObject object = pair.getFirst();
          EntityHolder holder = pair.getSecond();
          holder.setNNValue(ServerResolution.ORDER, it.getIndex());
          holder.setNNValue(ServerResolution.NAME, JRResolution.NAME.getValue(object));
          holder.setNNValue(ServerResolution.ICON_URL, JRResolution.ICON.getValue(object));
          holder.setNNValue(ServerResolution.DESCRIPTION, JRResolution.DESCRIPTION.getValue(object));
          holder.setValue(ServerResolution.ONLY_IN_PROJECTS, null);
        }
        progress.setDone();
      }
    },
    new Operation("statuses") {
      @Override
      public void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress) throws ConnectorException, ParseException {
        progress.startActivity("statuses");
        StoreIterator it = new StoreIterator(transaction, ServerStatus.TYPE, ServerStatus.ID, JRStatus.ID, RestOperations.statuses(session));
        it.managerDeleteBag();
        Pair<JSONObject, EntityHolder> pair;
        while ((pair = it.next()) != null) {
          JSONObject object = pair.getFirst();
          EntityHolder holder = pair.getSecond();
          holder.setNNValue(ServerStatus.ORDER, it.getIndex());
          holder.setNNValue(ServerStatus.NAME, JRStatus.NAME.getValue(object));
          holder.setNNValue(ServerStatus.ICON_URL, JRStatus.ICON.getValue(object));
          holder.setNNValue(ServerStatus.DESCRIPTION, JRStatus.DESCRIPTION.getValue(object));
          holder.setValue(ServerStatus.ONLY_IN_PROJECTS, null);
          JSONObject jsonCategory = JRStatus.CATEGORY.getValue(object);
          Entity category = jsonCategory != null ? JRStatusCategory.JSON_CONVERTOR.convert(jsonCategory) : null;
          holder.setNNValue(ServerStatus.CATEGORY, category);
        }
        progress.setDone();
      }
    }
  );

  public static void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress) throws CancelledException {
    ProgressInfo[] progresses = progress.split(OPERATIONS.size());
    for (int i = 0, operationsSize = OPERATIONS.size(); i < operationsSize; i++) {
      Operation operation = OPERATIONS.get(i);
      try {
        operation.perform(session, transaction, progresses[i]);
      } catch (ConnectorException e) {
        LogHelper.warning("Failed to load", operation, e);
      } catch (ParseException e) {
        LogHelper.warning("Failed to parse", operation, e);
      }
    }
  }

  private static abstract class Operation {
    private final String myDebugName;

    protected Operation(String debugName) {
      myDebugName = debugName;
    }

    public abstract void perform(RestSession session, EntityTransaction transaction, ProgressInfo progress) throws ConnectorException, ParseException;

    @Override
    public String toString() {
      return "'Load " + myDebugName + "'";
    }
  }
}
