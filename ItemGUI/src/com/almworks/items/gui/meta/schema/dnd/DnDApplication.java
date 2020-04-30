package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.editors.composition.CompositeEditor;
import com.almworks.items.gui.edit.engineactions.EditItemAction;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.CommitEditHelper;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.helper.EditFeature;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.sync.EditCommit;
import com.almworks.items.sync.EditDrain;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.util.LogHelper;
import com.almworks.util.Terms;
import com.almworks.util.collections.LongSet;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

class DnDApplication {
  private final List<ItemWrapper> myItems;
  private final ItemHypercube myTarget;
  private final String myFrameId;
  private final boolean myMove;
  private final GuiFeaturesManager myFeatures;
  private final Problems myProblems = new Problems();
  private final List<DnDFieldEditor> myEditors = Collections15.arrayList();
  private boolean mySilent = true;

  public DnDApplication(List<ItemWrapper> wrappers, GuiFeaturesManager features, ItemHypercube target, String frameId, boolean move) {
    myItems = wrappers;
    myFeatures = features;
    myTarget = target;
    myFrameId = frameId;
    myMove = move;
  }

  @Nullable
  public LoadedModelKey<?> findModelKey(long modelKey) {
    return myFeatures.getModelKeyCollector().getKey(modelKey);
  }

  @Nullable
  public LoadedModelKey<ItemKey> singleModelKey(long modelKey) {
    LoadedModelKey<?> mk = findModelKey(modelKey);
    if (mk == null) return null;
    LoadedModelKey<ItemKey> ref = mk.castScalar(ItemKey.class);
    LogHelper.assertError(ref != null, "Wrong model key", modelKey, mk);
    return ref;
  }

  @Nullable
  public LoadedModelKey<List<ItemKey>> multiModelKey(long modelKey) {
    LoadedModelKey<?> mk = findModelKey(modelKey);
    if (mk == null) return null;
    LoadedModelKey<List<ItemKey>> multiRef = mk.castList(ItemKey.class);
    LogHelper.assertError(multiRef != null, "Wrong model key", modelKey, mk);
    return multiRef;
  }

  public BaseEnumConstraintDescriptor getEnumConstraint(long constraint) {
    ConstraintDescriptor descriptor = myFeatures.getDescriptors().get(constraint);
    if (descriptor == null) return null;
    BaseEnumConstraintDescriptor enumDescriptor = Util.castNullable(BaseEnumConstraintDescriptor.class, descriptor);
    LogHelper.assertError(enumDescriptor != null, "Wrong descriptor class", constraint, descriptor);
    return enumDescriptor;
  }

  public Iterable<? extends ItemWrapper> getItems() {
    return myItems;
  }

  @NotNull
  public LongList getIncluded(DBAttribute<?> attribute) {
    SortedSet<Long> values = myTarget.getIncludedValues(attribute);
    if (values == null) return LongList.EMPTY;
    return LongArray.create(values);
  }

  @NotNull
  public LongList getExcluded(DBAttribute<?> attribute) {
    SortedSet<Long> values = myTarget.getExcludedValues(attribute);
    if (values == null) return LongList.EMPTY;
    return LongSet.create(values);
  }

  @Nullable
  public String getProblemMessage() {
    return myProblems.myMessage;
  }
  
  public Problems getProblems() {
    return myProblems;
  }

  public void addChangeEditor(DnDFieldEditor editor, boolean askUser) {
    myEditors.add(editor);
    if (askUser) mySilent = false;
  }

  @NotNull
  public List<DnDFieldEditor> getChanges() {
    return Collections.unmodifiableList(myEditors);
  }

  public boolean isSilent() {
    return mySilent;
  }

  public Collection<DBAttribute<?>> getAttributes() {
    return myTarget.getAxes();
  }

  public boolean isMove() {
    return myMove;
  }

  public void apply(ActionContext context) throws CantPerformException {
    final MyEditFeature editor = createEditFeature();
    if (!mySilent) {
      EditItemAction.startEdit(context, editor);
      return;
    }
    final LongArray items = new LongArray();
    final DefaultEditModel.Root model = editor.setupModel(context, items);
    items.sortUnique();
    final SyncManager syncManager = context.getSourceObject(SyncManager.ROLE);
    syncManager.commitEdit(items, new EditCommit() {
      private CommitEditHelper myHelper;

      @Override
      public void performCommit(EditDrain drain) throws DBOperationCancelledException {
        editor.prepareEdit(drain.getReader(), model, null);
        myHelper = CommitEditHelper.create(syncManager, model);
        myHelper.performCommit(drain);
      }

      @Override
      public void onCommitFinished(boolean success) {
        if (myHelper != null)
          myHelper.onCommitFinished(success);
      }
    });
  }

  private MyEditFeature createEditFeature() throws CantPerformException {
    final LongArray items = LongArray.create(ItemWrapper.GET_ITEM.collectList(myItems));
    String title;
    boolean single;
    if (myItems.size() == 1) {
      ItemWrapper item = myItems.get(0);
      String id = CantPerformException.ensureNotNull(item.services().getConnection()).getDisplayableItemId(item);
      if (id == null || id.isEmpty()) id = "New " + Terms.ref_Artifact;
      title = "Change " + id;
      single = true;
    } else {
      title = "Change " + myItems.size() + " " + Terms.ref_Artifacts;
      single = false;
    }
    title = Local.parse(title);
    EditDescriptor.Impl descriptor = EditDescriptor.Impl.frame(myFrameId, title, null);
    descriptor.addCommonActions(EditDescriptor.COMMON_ACTIONS);
    String itemName = Local.parse(single ? Terms.ref_artifact : Terms.ref_artifacts);
    descriptor.setDescriptionStrings(
      title,
      "Updated " + itemName + (single ? " was" : " were") + " saved in the local database.",
      "Save updated " + itemName + " in the local database without uploading to server",
      "Save updated " + itemName + " and upload to server"
    );
    Collection<Long> connections = ItemHypercubeUtils.getIncludedConnections(myTarget);
    long connection = connections.size() == 1 ? connections.iterator().next() : 0;
    return new MyEditFeature(descriptor, items, myEditors, connection, myTarget);
  }

  public TargetValues getTargetValues(DBAttribute<?> attribute) {
    LongList included = getIncluded(attribute);
    LongList excluded = getExcluded(attribute);
    return TargetValues.create(included, excluded);
  }

  private static class MyEditFeature implements EditFeature {
    private final EditDescriptor.Impl myDescriptor;
    private final LongList myItems;
    private final long myConnection;
    private final ItemHypercube myTarget;
    private final FieldEditor myEditor;

    public MyEditFeature(EditDescriptor.Impl descriptor, LongList items, List<? extends FieldEditor> editors, long connection, ItemHypercube target) {
      myDescriptor = descriptor;
      myItems = items;
      myConnection = connection;
      myTarget = target;
      myEditor = CompositeEditor.create(editors);
    }

    @Override
    public EditDescriptor checkContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
      return myDescriptor;
    }

    @Override
    public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
      itemsToLock.addAll(myItems);
      DefaultEditModel.Root model = DefaultEditModel.Root.editItems(myItems);
      if (myConnection > 0) {
        Connection connection = context.getSourceObject(Engine.ROLE).getConnectionManager().findByItem(myConnection);
        if (connection != null) EngineConsts.setupConnection(model, connection);
      }
      model.setHypercube(myTarget);
      return model;
    }

    @Override
    public void prepareEdit(DBReader reader, DefaultEditModel.Root model, @Nullable EditPrepare editPrepare) {
      myEditor.prepareModel(BranchSource.trunk(reader), model, editPrepare);
    }

    @Override
    public JComponent editModel(Lifespan life, EditItemModel model, Configuration editorConfig) {
      return VerticalLinePlacement.buildTopComponent(life, model, myEditor);
    }
  }

  public static class Problems {
    private String myMessage;
    private int myValue = -1;

    public void addNoValue(String message) {
      add(0, message);
    }

    public void addBasic(String message) {
      add(1, message);
    }

    public void addNotSupported(String message) {
      add(2, message);
    }

    private void add(int value, String notSupportedMessage) {
      if (myValue >= value) return;
      myValue = value;
      myMessage = notSupportedMessage;
    }
  }
}
