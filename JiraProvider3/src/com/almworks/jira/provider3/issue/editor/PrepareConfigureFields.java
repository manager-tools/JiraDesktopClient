package com.almworks.jira.provider3.issue.editor;

import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.issue.features.edit.EditIssueFeature;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.ACheckboxList;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.ui.DialogEditor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;

class PrepareConfigureFields implements ReadTransaction<Object>, Runnable {
  private static final Set<ServerFields.Field> NOT_CONTROLLABLE = Collections15.unmodifiableSetCopy(
    ServerFields.PROJECT, ServerFields.ISSUE_TYPE, ServerFields.COMMENTS, ServerFields.WORK_LOG, ServerFields.ATTACHMENT, ServerFields.LINKS);

  private final JiraConnection3 myConnection;
  private final DialogManager myDialogs;
  private final SyncManager myManager;
  private final Map<ResolvedField,Boolean> myEditorStates = Collections15.hashMap();
  @Nullable
  private final Procedure<Map<ResolvedField, Boolean>> myOnDone;
  private final boolean myUseRecent;

  private PrepareConfigureFields(JiraConnection3 connection, SyncManager manager, DialogManager dialogs, @Nullable Procedure<Map<ResolvedField, Boolean>> onDone,
    boolean useRecent) {
    myConnection = connection;
    myManager = manager;
    myDialogs = dialogs;
    myOnDone = onDone;
    myUseRecent = useRecent;
  }

  public static void start(ActionContext context, JiraConnection3 connection, Procedure<Map<ResolvedField, Boolean>> onDone, boolean useRecents) throws CantPerformException {
    SyncManager manager = context.getSourceObject(SyncManager.ROLE);
    manager.enquireRead(DBPriority.FOREGROUND, new PrepareConfigureFields(connection, manager, context.getSourceObject(DialogManager.ROLE), onDone, useRecents));
  }

  @Override
  public Object transaction(DBReader reader) throws DBOperationCancelledException {
    long connectionItem = myConnection.getConnectionItem();
    LongArray fields = CustomField.queryKnownKey(reader, connectionItem);
    LongList hidden = Jira.HIDDEN_EDITORS.getValue(reader, connectionItem);
    CustomFieldsComponent fieldEditors = myConnection.getCustomFields();
    for (ServerFields.Field field : ServerFields.EDITABLE_FIELDS) {
      if (NOT_CONTROLLABLE.contains(field)) continue;
      ResolvedField resolvedField = ResolvedField.loadStatic(reader, field);
      if (resolvedField != null) myEditorStates.put(resolvedField, !hidden.contains(resolvedField.getItem()));
    }
    for (ItemVersion field : SyncUtils.readItems(reader, fields)) {
      ResolvedField resolvedField = ResolvedField.load(field);
      if (resolvedField == null || fieldEditors.createFieldEditor(field) == null) continue;
      myEditorStates.put(resolvedField, !hidden.contains(field.getItem()));
    }
    ThreadGate.AWT.execute(this);
    return null;
  }

  @Override
  public void run() {
    DialogEditorBuilder builder = myDialogs.createEditor("jira.configureCustomFields");
    builder.hideApplyButton();
    builder.setModal(true);
    Configuration recentConfig = myUseRecent ? myConnection.getConnectionConfig("configureFields", "recent") : null;
    final MyDialogEditor editor = new MyDialogEditor(myEditorStates, myManager, myConnection.getConnectionItem(), recentConfig);
    builder.setContent(editor);
    builder.setInitialFocusOwner(editor.myList);
    builder.setTitle(EditIssueFeature.I18N.getString("edit.screens.action.configureFields.window.title"));
    builder.setModal(myOnDone != null);
    builder.showWindow();
    if (myOnDone != null) {
      Map<ResolvedField, Boolean> lastApplied = editor.getLastApplied();
      if (lastApplied != null) myOnDone.invoke(lastApplied);
    }
  }

  private static class MyDialogEditor extends SimpleModifiable implements DialogEditor {
    private final RecentController<ResolvedField> myRecents = new RecentController<ResolvedField>();
    private final ACheckboxList myList = ACheckboxList.newCheckBoxList();
    private final JComponent myComponent;
    private final Map<ResolvedField,Boolean> myInitialState;
    private final SyncManager myManager;
    private final long myConnection;
    private final AListModel<ResolvedField> myFieldsModel;
    private Map<ResolvedField, Boolean> myLastApplied = null;

    private MyDialogEditor(Map<ResolvedField, Boolean> initialState, SyncManager manager, long connection, @Nullable Configuration recentConfig) {
      myManager = manager;
      myConnection = connection;
      myInitialState = Collections15.hashMap(initialState);
      ArrayList<ResolvedField> list = Collections15.arrayList(initialState.keySet());
      Collections.sort(list, ResolvedField.BY_DISPLAY_NAME);
      myFieldsModel = setupList(list, recentConfig);
      JScrollPane scrollPane = new JScrollPane(myList);
      scrollPane.setPreferredSize(new Dimension(500, 350));
      myComponent = new JPanel(UIUtil.createBorderLayout());
      myComponent.add(scrollPane, BorderLayout.CENTER);
      JLabel hintMessage = new JLabel(FileUtil.loadTextResource("com/almworks/jira/provider3/issue/editor/configureFieldsHint.html", getClass().getClassLoader()));
      myComponent.add(hintMessage, BorderLayout.NORTH);
    }

    @SuppressWarnings("unchecked")
    private AListModel<ResolvedField> setupList(ArrayList<ResolvedField> list, @Nullable Configuration recentConfig) {
      myRecents.setRenderer(ResolvedField.RENDERER);
      myRecents.setIdentityConvertor(ResolvedField.GET_JIRA_ID);
      AListModel<ResolvedField> model = FixedListModel.create(list);
      myRecents.setup(model, recentConfig);
      myRecents.duplicateSelection(Lifespan.FOREVER, myList.getCheckedAccessor());
      myList.setCollectionModel(myRecents.getDecoratedModel());
      myList.setCanvasRenderer(myRecents.getDecoratedRenderer());
      ListSpeedSearch.install(myList);
      return model;
    }

    @Nullable
    public Map<ResolvedField, Boolean> getLastApplied() {
      return myLastApplied;
    }

    @Override
    public Modifiable getModifiable() {
      return this;
    }

    @Override
    public void reset() {
      ArrayList<ResolvedField> selected = Collections15.arrayList();
      for (ResolvedField field : myFieldsModel) {
        Boolean state = myInitialState.get(field);
        if (state == null) LogHelper.error("Missing state", field);
        else if (state) selected.add(field);
      }
      //noinspection unchecked
      myList.getCheckedAccessor().setSelected(selected);
    }

    @Override
    public boolean isModified() {
      HashSet<ResolvedField> selected = getSelected();
      for (ResolvedField field : myFieldsModel) {
        boolean current = selected.contains(field);
        Boolean initial = myInitialState.get(field);
        if (initial != null && current != initial) return true;
      }
      return false;
    }

    @Override
    public void apply() throws CantPerformExceptionExplained {
      final HashMap<ResolvedField, Boolean> newState = getCurrentState();
      HashSet<ResolvedField> difference = Collections15.hashSet();
      for (Map.Entry<ResolvedField, Boolean> entry : newState.entrySet()) {
        ResolvedField field = entry.getKey();
        Boolean was = myInitialState.get(field);
        if (was != null && !was.equals(entry.getValue())) difference.add(field);
      }
      myRecents.addToRecent(difference);
      myLastApplied = newState;
      myInitialState.clear();
      myInitialState.putAll(newState);
      myManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
        @Override
        public void write(DBDrain drain) throws DBOperationCancelledException {
          ItemVersionCreator connection = drain.changeItem(myConnection);
          if (!connection.isAlive()) return;
          ArrayList<ItemProxy> hidden = Collections15.arrayList();
          for (Map.Entry<ResolvedField, Boolean> entry : newState.entrySet()) {
            if (!entry.getValue()) hidden.add(new ItemProxy.Item(entry.getKey().getItem()));
          }
          Jira.HIDDEN_EDITORS.setValue(connection, hidden);
        }

        @Override
        public void onFinished(DBResult<?> result) {
        }
      });
    }

    private HashMap<ResolvedField, Boolean> getCurrentState() {
      HashSet<ResolvedField> selected = getSelected();
      HashMap<ResolvedField, Boolean> newState = Collections15.hashMap();
      for (ResolvedField field : myFieldsModel) {
        newState.put(field, selected.contains(field));
      }
      return newState;
    }

    private HashSet<ResolvedField> getSelected() {
      HashSet<ResolvedField> selected = Collections15.hashSet();
      for (Object obj : myList.getCheckedAccessor().getSelectedItems()) {
        ResolvedField field = RecentController.unwrap(obj);
        selected.add(field);
      }
      return selected;
    }

    @Override
    public JComponent getComponent() {
      return myComponent;
    }
  }
}
