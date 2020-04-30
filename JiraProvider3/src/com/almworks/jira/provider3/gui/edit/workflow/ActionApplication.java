package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ModelKey;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.cache.util.CachedItem;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.edit.FieldSet;
import com.almworks.jira.provider3.schema.WorkflowAction;
import com.almworks.util.LogHelper;
import com.almworks.util.Trio;
import com.almworks.util.collections.LongSet;
import com.almworks.util.ui.actions.EnableState;
import com.almworks.util.ui.actions.PresentationKey;
import gnu.trove.TLongLongHashMap;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class ActionApplication {
  private final TLongLongHashMap myActions;
  private final JiraConnection3 myConnection;
  private final LongList myFields;
  private final long myTargetStatus;
  private final String myActionName;

  private ActionApplication(TLongLongHashMap actions, JiraConnection3 connection, LongList fields, long targetStatus, String actionName) {
    myActions = actions;
    myConnection = connection;
    myFields = fields;
    myTargetStatus = targetStatus;
    myActionName = actionName;
  }

  public FieldSet collectFieldSet(VersionSource source) {
    LongSet actions = new LongSet();
    LongSet mandatory = new LongSet();
    for (long action : myActions.getValues()) {
      if (!actions.addValue(action)) continue;
      mandatory.addAll(WorkflowAction.MANDATORY_FIELDS.getValue(source.forItem(action)));
    }
    return FieldSet.create(myConnection, myFields, mandatory);
  }

  public long getTargetStatus() {
    return myTargetStatus;
  }

  public long getAction(long item) {
    return myActions.get(item);
  }

  @NotNull
  public LongList getFields() {
    return myFields;
  }

  @Nullable
  public static ActionApplication create(List<ItemWrapper> issues, JiraConnection3 connection, String actionName) {
    WFActionsLoader2 loader = connection.getProvider().getWorkflowActions();
    WLActionSets actionSets = connection.getActor(WLActionSets.ROLE);
    GuiFeaturesManager features = connection.getGuiFeatures();
    LoadedModelKey<ItemKey> keyProject = features.findScalarKey(MetaSchema.KEY_PROJECT, ItemKey.class);
    LoadedModelKey<ItemKey> keyType = features.findScalarKey(MetaSchema.KEY_ISSUE_TYPE, ItemKey.class);
    LoadedModelKey<ItemKey> keyStatus = features.findScalarKey(MetaSchema.KEY_STATUS, ItemKey.class);
    LoadedModelKey<LongList> keyActions = features.findScalarKey(MetaSchema.KEY_WORKFLOW_ACTIONS, LongList.class);
    if (keyProject == null || keyType == null || keyStatus == null || keyActions == null) {
      LogHelper.error("Missing key", keyProject, keyType, keyStatus, keyActions);
      return null;
    }
    long connectionItem = connection.getConnectionItem();
    List<CachedItem> actions = loader.getActions(connectionItem, actionName, null);
    TLongObjectHashMap<CachedItem> applicables = new TLongObjectHashMap<>();
    for (CachedItem action : actions) {
      Integer id = action.getValue(WorkflowAction.ID);
      if (id == null) continue;
      LongSet items = selectApplicable(action, issues, keyProject, keyType, keyStatus, keyActions, actionSets);
      for (LongIterator ii : items) {
        long item = ii.value();
        CachedItem known = applicables.get(item);
        if (known == null) applicables.put(item, action);
        else if (action.getItem() != known.getItem()) {
          LogHelper.warning("Two different applicable actions detected", action.getItem(), known.getItem(), actionName);
          return null;
        }
      }
    }
    LongList commonFields = null;
    long commonStatus = 0;
    TLongLongHashMap actionItems = new TLongLongHashMap();
    for (ItemWrapper issue : issues) {
      CachedItem action = applicables.get(issue.getItem());
      if (action == null) return null;
      actionItems.put(issue.getItem(), action.getItem());
      LongList actionFields = action.getValue(WorkflowAction.FIELDS);
      if (commonFields != null && !commonFields.equals(actionFields)) return null; // Actions requires different fields - cannot be applied to the issue set
      else commonFields = actionFields;
      long status = WorkflowAction.TARGET_STATUS.getValue(action);
      if (commonStatus == 0) commonStatus = status;
      else if (status != 0 && status != commonStatus) return null; // Different target status - cannot be applied to the issue set
    }
    if (commonFields == null) return null; // No applicable action known
    if (commonStatus == 0) commonStatus = chooseCommonStatus(actions);
    return new ActionApplication(actionItems, connection, commonFields, commonStatus, actionName);
  }

  private static long chooseCommonStatus(List<CachedItem> actions) {
    long status = 0;
    for (CachedItem action : actions) {
      long st = WorkflowAction.TARGET_STATUS.getValue(action);
      if (st <= 0) continue;
      if (status == 0) status = st;
      else if (status != st) return 0;
    }
    return status;
  }

  private static LongSet selectApplicable(CachedItem action, List<ItemWrapper> issues, LoadedModelKey<ItemKey> keyProject, LoadedModelKey<ItemKey> keyType,
    LoadedModelKey<ItemKey> keyStatus, LoadedModelKey<LongList> keyActions, WLActionSets actionSets) {
    Set<Trio<Long, Long, Long>> applicable = Collections15.hashSet();
    LongSet result = new LongSet();
    for (ItemWrapper wrapper : issues) {
      LongList actions = keyActions.getValue(wrapper.getLastDBValues());
      if (actions != null) {
        if (actions.binarySearch(action.getItem()) < 0) continue;
      } else {
        long project = getItem(wrapper, keyProject);
        long type = getItem(wrapper, keyType);
        long status = getItem(wrapper, keyStatus);
        if (project <= 0 || type <= 0 || status <= 0) continue;
        Trio<Long, Long, Long> trio = Trio.create(project, type, status);
        if (!applicable.contains(trio)) {
          if (!actionSets.isApplicable(action.getItem(), project, type, status)) continue;
          applicable.add(trio);
        }
      }
      result.add(wrapper.getItem());
    }
    return result;
  }

  private static long getItem(ItemWrapper wrapper, ModelKey<ItemKey> key) {
    ItemKey itemKey = key.getValue(wrapper.getLastDBValues());
    return itemKey == null ? 0 : itemKey.getItem();
  }

  public String getWindowId() {
    long fieldHash = 0;
    for (int i = 0; i < myFields.size(); i++) fieldHash += myFields.get(i);
    return "jira.workflow." + myActionName + "." + fieldHash;
  }

  public long getConnection() {
    return myConnection.getConnectionItem();
  }

  public EditDescriptor.Impl createEditDescriptor(List<ItemWrapper> issues) {
    String windowTitle = myActionName;
    int targetCount = issues.size();
    if (targetCount > 1) windowTitle += " (" + targetCount + " Issues)";
    else windowTitle = JiraActions.prependIssueKey(issues, windowTitle);
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame(getWindowId(), windowTitle, null);
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    descriptor.putPresentationProperty(PresentationKey.ENABLE, EnableState.INVISIBLE);
    descriptor.setDescriptionStrings(
      windowTitle,
      "Updated issue" + (targetCount > 1 ? "s were" : " was") + " saved in the local database.",
      "Save updated issues in the local database without uploading to server",
      "Save updated issues and upload to server");
    descriptor.putPresentationProperty(PresentationKey.ENABLE, EnableState.DISABLED);
    descriptor.putPresentationProperty(PresentationKey.ENABLE, EnableState.ENABLED);
    return descriptor;
  }
}
