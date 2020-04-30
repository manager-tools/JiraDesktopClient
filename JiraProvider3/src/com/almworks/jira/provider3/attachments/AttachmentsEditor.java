package com.almworks.jira.provider3.attachments;

import com.almworks.api.actions.AttachScreenshotAction;
import com.almworks.api.config.MiscConfig;
import com.almworks.api.download.DownloadManager;
import com.almworks.api.download.DownloadOwner;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.engine.gui.attachments.*;
import com.almworks.integers.LongArray;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.L;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.components.ScrollPaneBorder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.MapMedium;
import com.almworks.util.files.FileUtil;
import com.almworks.util.images.Icons;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.SwingTreeUtil;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.almworks.engine.gui.attachments.AttachmentProperty.*;

public class AttachmentsEditor extends BaseFieldEditor {
  public static final AttachmentsEditor INSTANCE = new AttachmentsEditor();
  private static final TypedKey<AttachmentsEditor> ROLE = TypedKey.create("AttachmentsEditor");
  private static final TypedKey<EditItemModel> EDITOR_MODEL = TypedKey.create("AttachmentsEditorModel");
  private static final TypedKey<Configuration> CONFIG_KEY = TypedKey.create("attachments/config");

  private final TypedKey<List<? extends Attachment>> myValue = TypedKey.create("attachments/value");
  private final TypedKey<List<AttachmentImpl>> myRemoved = TypedKey.create("attachments/removed");
  private final TypedKey<List<AttachmentImpl>> myRenamed = TypedKey.create("attachments/renamed");
  private final TypedKey<List<Attachment>> myLocked = TypedKey.create("attachments/locked");
  private final TypedKey<Integer> myAddCounter = TypedKey.create("attachments/addCounter");
  private final TypedKey<String> myIssueKey = TypedKey.create("attachments/issueKey");
  private final TypedKey<Boolean> myForceUpdate = TypedKey.create("attachments/forceUpdate");

  AttachmentsEditor() {
    super(NameMnemonic.rawText("Attachments"));
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    EngineConsts.ensureGuiFeatureManager(source, model);
    List<Attachment> initialValue, locked;
    List<ItemVersion> items = source.readItems(model.getEditingItems());
    String key = null;
    if (model.isNewItem()) {
      initialValue = Collections.emptyList();
      locked = Collections.emptyList();
    } else if (items.size() == 1) {
      initialValue = Collections15.arrayList();
      locked = Collections15.arrayList();
      GuiFeaturesManager guiFeaturesManager = EngineConsts.getGuiFeaturesManager(model);
      JiraConnection3 connection = EngineConsts.getConnection(JiraConnection3.class, model);
      ItemVersion issue = items.get(0);
      LongArray atts = issue.getSlaves(com.almworks.jira.provider3.schema.Attachment.ISSUE);
      for (ItemVersion att : source.readItems(atts)) {
        Attachment attachment = AttachmentImpl.load(att, guiFeaturesManager, connection);
        initialValue.add(attachment);
        if (!editPrepare.addItems(LongArray.create(att.getItem()))) locked.add(attachment);
      }
      key = Issue.KEY.getValue(issue.getItem(), source.getReader());
    } else return; // Edit multiple
    model.putHint(myValue, initialValue);
    model.putHint(myLocked, locked);
    model.putHint(myIssueKey, key);
    model.registerEditor(this);
    RenameAttachmentDialog.prepareModel(source, model, editPrepare);
  }

  public static void setConfig(EditModelState model, Configuration parentConfig) {
    model.putHint(CONFIG_KEY, parentConfig.getOrCreateSubset("attachmentsEditor"));
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    List<? extends Attachment> atts = model.getValue(myValue);
    if (atts == null) return Collections.emptyList(); // Edit multiple issues
    JComponent component = createComponent(life, model, null, false);
    if (component == null) return Collections.emptyList();
    return SimpleComponentControl.singleComponent(component, ComponentControl.Dimensions.TALL, this, model, ComponentControl.Enabled.ENABLED);
  }

  public void attach(Lifespan life, final EditItemModel editModel, final PlaceHolder attachmentsPlaceHolder) {
    List<? extends Attachment> atts = editModel.getValue(myValue);
    if (atts == null) {
      attachmentsPlaceHolder.setVisible(false);
      return;
    }
    JComponent component = createComponent(life, editModel, attachmentsPlaceHolder, true);
    attachmentsPlaceHolder.show(component);
  }

  private JComponent createComponent(Lifespan life, final EditItemModel editModel, @Nullable JComponent outerComponent, final boolean hideEmpty) {
    final OrderListModel<Attachment> model = new OrderListModel<Attachment>(editModel.getValue(myValue));
    SortedListDecorator<Attachment> sortedModel = SortedListDecorator.create(life, model, Attachment.ORDER);
    Configuration config = editModel.getValue(CONFIG_KEY);
    if (config == null) config = MapMedium.createConfig();

    JiraConnection3 connection = EngineConsts.getConnection(JiraConnection3.class, editModel);
    if (connection == null) return null;
    Configuration panelGlobalConfig = connection.getActor(MiscConfig.ROLE).getConfig("attachmentsPanel");
    final Configuration panelLocalConfig = config.getOrCreateSubset("attachmentsPanel");
    MenuBuilder popupMenu = AttachmentUtils.createAttachmentPopupMenu(JiraActions.EDITOR_DELETE_ATTACHMENT, JiraActions.EDITOR_RENAME_ATTACHMENT/*, null*/);
    final AttachmentsPanel<Attachment> panel = new AttachmentsPanel<Attachment>(popupMenu, Attachment.ROLE, panelGlobalConfig, panelLocalConfig);
    panel.setTableMode();
    addProperties(panel, FILE_NAME, MIME_TYPE, DATE, USER, SIZE);
    panel.initialize();
    DownloadManager downloadManager = connection.getActor(DownloadManager.ROLE);
    panel.show(sortedModel, downloadManager);
    final JComponent panelComponent = panel.getComponent();
    JComponent ownComponent = withBorder(panelComponent);
    final JComponent topComponent = outerComponent != null ? outerComponent : ownComponent;

    ChangeListener listener = new ChangeListener() {
      private List<Attachment> myPrev = null;

      @Override
      public void onChange() {
        List<? extends Attachment> value = editModel.getValue(myValue);
        Boolean forceUpdate = editModel.getValue(myForceUpdate);
        if (myPrev != null && myPrev.equals(value) && (forceUpdate == null || !forceUpdate)) return;
        if (myPrev == null) myPrev = Collections15.arrayList();
        myPrev.clear();
        myPrev.addAll(value);
        model.replaceElementsSet(value);
        editModel.putHint(myForceUpdate, false);
        int minWidth = 100;
        int prefHeight = panel.getPreferredHeight(minWidth);
        int maxHeight = 150;
        panelComponent.setPreferredSize(new Dimension(minWidth, Math.min(maxHeight, prefHeight)));
        if (hideEmpty) topComponent.setVisible(!value.isEmpty());
        SwingTreeUtil.revalidateWindow(panelComponent);
      }
    };
    listener.onChange();
    editModel.addAWTChangeListener(life, listener);
    ConstProvider.addGlobalValue(topComponent, EDITOR_MODEL, editModel);
    ConstProvider.addGlobalValue(topComponent, ROLE, this);
    return ownComponent;
  }

  private JComponent withBorder(JComponent panelComponent) {
    return Aqua.isAqua() ? panelComponent : new ScrollPaneBorder(panelComponent);
  }

  public static void addProperties(AttachmentsPanel<Attachment> panel, AttachmentProperty<Attachment, ?>... properties) {
    for (AttachmentProperty<Attachment, ?> property : properties) {
      panel.addProperty(property);
    }
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    List<? extends Attachment> initialValue = model.getInitialValue(myValue);
    List<AttachmentImpl> removed = model.getValue(myRemoved);
    boolean hasRemoved = removed != null && !removed.isEmpty();
    List<AttachmentImpl> renamed = model.getValue(myRenamed);
    boolean hasRenamed = renamed != null && !renamed.isEmpty();
    return (initialValue != null && !initialValue.equals(model.getValue(myValue)))
        || hasRemoved || hasRenamed;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    List<? extends Attachment> value = model.getValue(myValue);
    return value != null && !value.isEmpty();
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
  }

  @Override
  public void commit(CommitContext context) {
    ItemVersionCreator creator = context.getCreator();
    EditItemModel model = context.getModel();
    List<? extends Attachment> atts = model.getValue(myValue);
    for (Attachment att : atts) {
      NewAttachment newAtt = Util.castNullable(NewAttachment.class, att);
      if (newAtt != null) AttachEditFeature.createAttachment(creator, newAtt.myFile, newAtt.myName, newAtt.myMimeType);
    }
    List<AttachmentImpl> removed = model.getValue(myRemoved);
    boolean hasRemoved = false;
    if (removed != null) {
      hasRemoved = true;
      for (AttachmentImpl att : removed) context.getDrain().changeItem(att.getItem()).delete();
    }
    List<AttachmentImpl> renamed = model.getValue(myRenamed);
    if (renamed != null) for (AttachmentImpl att : renamed) {
      ItemVersionCreator ivc = context.getDrain().changeItem(att.getItem());
      if (!(hasRemoved && removed.contains(att)) && ivc.getValue(com.almworks.jira.provider3.schema.Attachment.FILE_URL) == null){
        ivc.setValue(com.almworks.jira.provider3.schema.Attachment.ATTACHMENT_NAME, att.getName());
      }
    }
  }

  @Override
  public void onItemsChanged(EditItemModel editModel, TLongObjectHashMap<ItemValues> newValues) {
  }

  public void addAttachments(EditItemModel model, NewAttachment ... newAtts) {
    if (model == null || newAtts == null || newAtts.length == 0)
      return;
    List<? extends Attachment> value = model.getValue(myValue);
    ArrayList<Attachment> newValue = Collections15.arrayList(value);
    Integer addCounter = model.getValue(myAddCounter);
    int counter = addCounter == null ? 0 : addCounter;
    for (NewAttachment newAtt : newAtts) {
      if (newValue.contains(newAtt))
        continue;
      newAtt.mySortingNumber = -++counter;
      newValue.add(newAtt);
    }
    model.putValue(myValue, newValue);
    model.putHint(myAddCounter, counter);
  }

  public void removeAttachments(EditItemModel model, List<Attachment> atts) {
    if (model == null || atts == null || atts.isEmpty()) return;
    List<? extends Attachment> value = model.getValue(myValue);
    ArrayList<Attachment> newValue = Collections15.arrayList(value);
    List<AttachmentImpl> removedList = model.getValue(myRemoved);
    if (removedList == null)
      removedList = Collections15.arrayList();
    boolean changed = false;
    for (Attachment att : atts) {
      int index = newValue.indexOf(att);
      if (index < 0)
        continue;
      changed = true;
      AttachmentImpl removed = Util.castNullable(AttachmentImpl.class, newValue.remove(index));
      if (removed != null) removedList.add(removed);
    }
    if (changed) model.putValue(myValue, newValue);
    if (!removedList.isEmpty()) model.putValue(myRemoved, removedList);
  }

  private void renameAttachment(EditItemModel model, Attachment att, String newName) {
    att.setName(newName);
    AttachmentImpl ai = Util.castNullable(AttachmentImpl.class, att);
    if (ai == null) return;
    List<AttachmentImpl> renamed = model.getValue(myRenamed);
    if (renamed == null) {
      renamed = Collections15.arrayList();
      model.putHint(myRenamed, renamed);
    }
    if (!renamed.contains(ai)) renamed.add(ai);
    model.putHint(myForceUpdate, true);
    model.fireChanged();
  }

  public String getSourceKey(EditItemModel model) {
    return model != null ? model.getValue(myIssueKey) : null;
  }

  private static class NewAttachment extends Attachment {
    private final File myFile;
    private long mySortingNumber;
    private final String mySizeString;
    private final long mySize;
    private String myMimeType;
    private String myName;

    public NewAttachment(File f, String name, String mimeType) {
      myFile = f;
      mySortingNumber = System.currentTimeMillis();
      mySize = myFile.length();
      mySizeString = String.valueOf(mySize);
      myName = name;
      myMimeType = mimeType;
    }

    @NotNull
    @Override
    public DownloadOwner getDownloadOwner() {
      assert false;
      return null;
    }

    @Override
    public String getDownloadArgument() {
      assert false;
      return null;
    }

    @Override
    public String getUrl() {
      return null;
    }

    @Override
    public String getName() {
      return myName;
    }

    public void setName(String name) {
      myName = name;
    }

    @Override
    public long getExpectedSize() {
      return mySize;
    }

    @Override
    public boolean isLocal() {
      return true;
    }

    @Override
    public String getMimeType(@Nullable AttachmentDownloadStatus<? extends Attachment> downloadStatus) {
      return myMimeType;
    }

    @Override
    public String getExpectedSizeText() {
      return mySizeString;
    }

    @Override
    public String getUser() {
      return null;
    }

    @Override
    public String getDateString() {
      return null;
    }

    @Override
    public Date getDate() {
      return null;
    }

    @Override
    public File getLocalFile(@Nullable AttachmentDownloadStatus<? extends Attachment> downloadStatus) {
      return myFile;
    }

    @Override
    public long getOrderForSorting() {
      return mySortingNumber;
    }

    @Override
    public boolean equals(Object obj) {
      final NewAttachment other = Util.castNullable(NewAttachment.class, obj);
      return other != null && myFile.equals(other.myFile);
    }
  }

  private static abstract class Action extends SimpleAction {
    protected Action(@Nullable String name) {
      this(name, null);
    }

    protected Action(@Nullable String name, @Nullable Icon icon) {
      super(L.actionName(name), icon);
      watchRole(ROLE);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(ROLE);
      EditItemModel editModel = context.getSourceObject(EDITOR_MODEL);
      CantPerformException.ensure(EngineConsts.getConnectionItem(editModel) > 0);
    }
  }

  public static final AnAction EDITOR_DELETE_ATTACHMENT = new Action("Delete Attachments") {
    {
      watchRole(Attachment.ROLE);
    }
    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      super.customUpdate(context);
      List<Attachment> attachments = context.getSourceCollection(Attachment.ROLE);
      CantPerformException.ensureNotEmpty(attachments);
      AttachmentsEditor editor = context.getSourceObject(ROLE);
      EditItemModel model = context.getSourceObject(EDITOR_MODEL);
      List<Attachment> locked = model.getValue(editor.myLocked);
      for (Attachment a : attachments) {
        CantPerformException.ensure(a.isLocal());
        if (locked != null) CantPerformException.ensure(!locked.contains(a));
      }

      String name = "Delete Attachment";
      if (attachments.size() > 1)
        name = name + "s";
      context.putPresentationProperty(PresentationKey.NAME, L.actionName(name));
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      AttachmentsEditor editor = context.getSourceObject(ROLE);
      EditItemModel model = context.getSourceObject(EDITOR_MODEL);
      List<Attachment> atts = context.getSourceCollection(Attachment.ROLE);
//      String message;
//      message = atts.size() == 1 ?
//        "Are you sure you want to delete " + atts.get(0).getDisplayName() + "?" :
//        "Are you sure you want to delete " + atts.size() + " attachments?";
//      if (!DialogsUtil.askConfirmation(context.getComponent(), message, "Delete Attachments")) return;
      editor.removeAttachments(model, atts);
    }
  };

  public static final AnAction EDITOR_RENAME_ATTACHMENT = new Action("Rename Attachment") {
    {
      watchRole(Attachment.ROLE);
    }
    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      super.customUpdate(context);
      Attachment att = context.getSourceObject(Attachment.ROLE);
      CantPerformException.ensure(att.isLocal());
      AttachmentsEditor editor = context.getSourceObject(ROLE);
      EditItemModel model = context.getSourceObject(EDITOR_MODEL);
      List<Attachment> locked = model.getValue(editor.myLocked);
      if (locked != null) CantPerformException.ensure(!locked.contains(att));
      CantPerformException.ensure(RenameAttachmentDialog.checkModel(model));
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      AttachmentsEditor editor = context.getSourceObject(ROLE);
      EditItemModel model = context.getSourceObject(EDITOR_MODEL);
      Attachment att = context.getSourceObject(Attachment.ROLE);
      String oldName = att.getName();
      String newName = RenameAttachmentDialog.show(context, model, editor.getSourceKey(model), oldName);
      if (newName != null && !newName.equals(oldName)) editor.renameAttachment(model, att, newName);
    }
  };

  public static final AnAction EDITOR_ATTACH_FILES = new Action("Attach Files\u2026", Icons.ACTION_ATTACH_FILE) {
    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      final EditItemModel editModel = context.getSourceObject(EDITOR_MODEL);
      AttachmentsEditor editor = context.getSourceObject(ROLE);
      JiraConnection3 connection = CantPerformException.ensureNotNull(EngineConsts.getConnection(JiraConnection3.class, editModel));
      Configuration config = connection.getConnectionConfig("attachments", "attachFile");
      File[] files = AttachmentChooserOpen.show(context.getComponent(), config, true, 0);
      if (files != null) {
        NewAttachment[] na = new NewAttachment[files.length];
        for (int i = 0 ; i < files.length; i++) {
          String filename = files[i].getName();
          na[i] = new NewAttachment(files[i], filename, FileUtil.guessMimeType(filename));
        }
        editor.addAttachments(editModel, na);
      }
    }
  };

  public static final AnAction EDITOR_ATTACH_SCREENSHOT = new Action("Attach Screenshot", Icons.ACTION_ATTACH_SCREENSHOT) {
    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      final EditItemModel editModel = context.getSourceObject(EDITOR_MODEL);
      final AttachmentsEditor editor = context.getSourceObject(ROLE);
      MiscConfig miscConfig = context.getSourceObject(MiscConfig.ROLE);
      AttachScreenshotAction.attach(context, null, miscConfig, new AddAttachmentCallback() {
        @Override
        public void addAttachment(File file, String name, String mimeType) {
          editor.addAttachments(editModel, new NewAttachment(file, name, mimeType));
        }
      });
    }
  };

  public static final AnAction EDITOR_ATTACH_TEXT = new Action("Attach Text", Icons.ACTION_ATTACH_TEXT) {
    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      final EditItemModel editModel = context.getSourceObject(EDITOR_MODEL);
      final AttachmentsEditor editor = context.getSourceObject(ROLE);
      DialogBuilder builder = context.getSourceObject(DialogManager.ROLE).createBuilder("jira.attachText");
      MiscConfig miscConfig = context.getSourceObject(MiscConfig.ROLE);
      AttachTextAction.attach(builder, editor.getSourceKey(editModel), miscConfig, new AddAttachmentCallback() {
        @Override
        public void addAttachment(File file, String name, String mimeType) {
          editor.addAttachments(editModel, new NewAttachment(file, name, mimeType));
        }
      });
    }
  };
}
