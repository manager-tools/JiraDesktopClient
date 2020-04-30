package com.almworks.jira.provider3.links.actions;

import com.almworks.api.actions.ItemActionUtils;
import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.util.BaseFieldEditor;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.items.sync.util.ItemValues;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.links.*;
import com.almworks.jira.provider3.schema.*;
import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.AScrollPane;
import com.almworks.util.components.ATable;
import com.almworks.util.components.SizeDelegate;
import com.almworks.util.components.TreeModelBridge;
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

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinksEditor extends BaseFieldEditor {
  public static final LinksEditor INSTANCE = new LinksEditor();
  public static final TypedKey<LinksEditor> ROLE = TypedKey.create("LinksEditor");
  private static final TypedKey<EditItemModel> EDITOR_MODEL = TypedKey.create("LinksEditorModel");

  private final TypedKey<List<? extends LoadedLink>> myValue = TypedKey.create("links/value");
  private final TypedKey<List<LoadedLink2>> myRemoved = TypedKey.create("links/removed");
  private final TypedKey<String> myIssueKey = TypedKey.create("links/issueKey");
  private final TypedKey<Boolean> myForceUpdate = TypedKey.create("links/forceUpdate");
  private final TypedKey<DirectionalLinkType> myLastLinkType = TypedKey.create("links/lastLinkType");

  private LinksEditor() {
    super(NameMnemonic.rawText("Links"));
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    EngineConsts.ensureGuiFeatureManager(source, model);
    LongList items = model.getEditingItems();
    List<LoadedLink2> initialValue;
    String key;
    if (model.isNewItem()) {
      initialValue = Collections.emptyList();
      key = "New Issue";
    } else if (items.size() == 1) {
      long issue = items.get(0);
      initialValue = Collections15.arrayList();
      key = Issue.KEY.getValue(issue, source.getReader());
      if (key == null) key = "New Issue";
      for (ItemVersion link : source.readItems(source.getReader().query(Link.SOURCE.queryEqual(issue).or(Link.TARGET.queryEqual(issue))).copyItemsSorted())) {
        initialValue.add(LoadedLink2.load(link, issue));
        editPrepare.addItems(LongArray.create(link.getItem()));
      }
      LoadedLink2.filterIsotropic(source, initialValue);
    } else return;
    model.putHint(myValue, initialValue);
    model.putHint(myIssueKey, key);
    model.registerEditor(this);
    AddLinksDialog.prepareModel(source, model, editPrepare);
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    GuiFeaturesManager manager = EngineConsts.getGuiFeaturesManager(model);
    if (model.getValue(myValue) == null || manager == null) return Collections.emptyList();
    LinksTree tree = LinksTree.create(manager, null);
    ATable<? extends Pair<?, TreeModelBridge<?>>> table = tree.showAsTree();
    attach(life, model, tree, table);
    AScrollPane scrollPane = new AScrollPane(table);
    scrollPane.setSizeDelegate(new SizeDelegate.Fixed().setPrefWidth(200).setMaxHeight(100));
    return SimpleComponentControl.singleComponent(scrollPane, ComponentControl.Dimensions.TALL, this, model, ComponentControl.Enabled.ALWAYS_ENABLED);
  }

  public void attach(Lifespan life, EditItemModel model, ATable<? extends Pair<?, TreeModelBridge<?>>> table) {
    GuiFeaturesManager manager = EngineConsts.getGuiFeaturesManager(model);
    if (manager == null) return;
    LinksTree tree = LinksTree.create(manager, null);
    tree.attachTable(table);
    attach(life, model, tree, table);
  }

  private void attach(Lifespan life, final EditItemModel model, final LinksTree tree, final ATable<? extends Pair<?, TreeModelBridge<?>>> table) {
    String key = model.getValue(myIssueKey);
    List<? extends LoadedLink> links = model.getValue(myValue);
    if (key == null || links == null) {
      table.setVisible(false);
      return;
    }
    tree.setRootObject(key);
    tree.update(links);
    ChangeListener listener = new ChangeListener() {
      private List<LoadedLink> myPrev = null;

      @Override
      public void onChange() {
        List<? extends LoadedLink> value = model.getValue(myValue);
        Boolean forceUpdate = model.getValue(myForceUpdate);
        if (myPrev != null && myPrev.equals(value) && (forceUpdate == null || !forceUpdate))
          return;
        if (myPrev == null)
          myPrev = Collections15.arrayList();
        myPrev.clear();
        myPrev.addAll(value);
        tree.update(value);
        model.putHint(myForceUpdate, false);
        if (value.isEmpty()) table.setVisible(false);
        else {
          table.setVisible(true);
          table.resizeColumns();
          JScrollPane scrollPane = SwingTreeUtil.findAncestorOfType(table, JScrollPane.class);
          if (scrollPane != null && scrollPane.getViewport().getView() == table) {
            int prefHeight = table.getPreferredSize().height + 5;
            int maxHeight = 150;
            scrollPane.setPreferredSize(new Dimension(0, Math.min(prefHeight, maxHeight)));
          }
          SwingTreeUtil.revalidateWindow(table);
        }
      }
    };
    listener.onChange();
    model.addAWTChangeListener(life, listener);
    LoadLinkProvider.install(table, LoadedLink.LINK).provideGlobal();
    ConstProvider.addGlobalValue(table, ROLE, this);
    ConstProvider.addGlobalValue(table, EDITOR_MODEL, model);
    MenuBuilder menu = new MenuBuilder();
    menu.addAction(JiraActions.EDITOR_ADD_LINKS);
    menu.addAction(JiraActions.EDITOR_REMOVE_LINKS);
    menu.addToComponent(life, table.getSwingComponent());
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    List<? extends LoadedLink> initialValue = model.getInitialValue(myValue);
    List<LoadedLink2> removed = model.getValue(myRemoved);
    return (initialValue != null && !initialValue.equals(model.getValue(myValue)))
      || (removed != null && !removed.isEmpty());
  }

  @Override
  public boolean hasValue(EditModelState model) {
    List<? extends LoadedLink> value = model.getValue(myValue);
    return value != null && !value.isEmpty();
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
  }

  @Override
  public void commit(CommitContext context) {
    EditItemModel issueModel = context.getModel();
    List<? extends LoadedLink> links = issueModel.getValue(myValue);
    for (LoadedLink link : links) {
      NewLink newLink = Util.castNullable(NewLink.class, link);
      if (newLink != null) newLink.create(context);
    }
    List<LoadedLink2> removed = issueModel.getValue(myRemoved);
    if (removed != null) {
      LongArray removeItems = ItemActionUtils.collectItems(removed);
      JiraLinks.deleteLinks(context.getDrain(), removeItems);
    }
    DirectionalTypeEditor.storeLinkType(context.getDrain(), context.readTrunk().getValue(SyncAttributes.CONNECTION), issueModel.getValue(myLastLinkType));
  }

  @Override
  public void onItemsChanged(EditItemModel model, TLongObjectHashMap<ItemValues> newValues) {
  }

  private String getSourceKey(EditItemModel model) {
    return model != null ? model.getValue(myIssueKey) : null;
  }

  private void addLinks(EditItemModel issueModel, DirectionalLinkType type, List<String> keys, Database db) {
    GuiFeaturesManager manager = EngineConsts.getGuiFeaturesManager(issueModel);
    if (issueModel == null || type == null || keys == null || keys.isEmpty() || manager == null) return;
    List<? extends LoadedLink> value = issueModel.getValue(myValue);
    String thisKey = getSourceKey(issueModel);
    ArrayList<NewLink> newLinks = Collections15.arrayList();
    for (String key : keys) {
      if (thisKey.equals(key))
        continue;
      NewLink link = new NewLink(type, key);
      if (LinksTree.indexOf(manager, value, link) >= 0 || LinksTree.indexOf(manager, newLinks, link) >= 0) continue;
      newLinks.add(link);
    }
    if (newLinks.size() > 0) {
      ArrayList<LoadedLink> newValue = Collections15.arrayList(value);
      newValue.addAll(newLinks);
      issueModel.putValue(myValue, newValue);
      long connectionItem = EngineConsts.getConnectionItem(issueModel);
      getOppositeInfo(connectionItem, newLinks, db, issueModel);
    }
  }

  private void getOppositeInfo(final long connectionItem, final ArrayList<NewLink> newLinks, Database db, final EditItemModel model) {
    db.readBackground(new ReadTransaction<Object>() {
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        BranchSource versionSource = BranchSource.trunk(reader);
        for (NewLink link : newLinks) {
          List<String> key = Collections.singletonList(link.getOppositeString(LoadedLink.KEY));
          List<ItemVersion> itemVersions = versionSource.readItems(AddLinksEditor.resolveIssues(reader, connectionItem, key));
          if (itemVersions.size() == 1)
            link.applyOppositeInfo(itemVersions.get(0));
          else if (itemVersions.size() == 0)
            link.markAsUnresolved();
        }
        model.putHint(myForceUpdate, true);
        model.fireChanged();
        return null;
      }
    });
  }

  private void removeLinks(EditItemModel model, List<LoadedLink> links) {
    GuiFeaturesManager manager = EngineConsts.getGuiFeaturesManager(model);
    if (model == null || links == null || links.isEmpty() || manager == null) return;
    List<? extends LoadedLink> value = model.getValue(myValue);
    ArrayList<LoadedLink> newValue = Collections15.arrayList(value);
    List<LoadedLink2> removedList = model.getValue(myRemoved);
    if (removedList == null) removedList = Collections15.arrayList();
    boolean changed = false;
    for (LoadedLink link : links) {
      int index = LinksTree.indexOf(manager, newValue, link);
      if (index < 0) continue;
      changed = true;
      LoadedLink2 removed = Util.castNullable(LoadedLink2.class, newValue.remove(index));
      if (removed != null) removedList.add(removed);
    }
    if (changed) model.putValue(myValue, newValue);
    if (!removedList.isEmpty()) model.putValue(myRemoved, removedList);
  }

  private static class NewLink implements LoadedLink {
    private final DirectionalLinkType myType;
    private String myOppositeSummary;
    private final String myOppositeKey;
    private long myOppositeStatus;
    private long myOppositeIssueType;
    private long myOppositePriority;

    private NewLink(DirectionalLinkType type, String oppositeKey) {
      myType = type;
      myOppositeKey = oppositeKey;
    }

    public void create(CommitContext issueContext) {
      Long connection = issueContext.readTrunk().getValue(SyncAttributes.CONNECTION);
      if (connection == null || connection < 0) return;
      LongList issues = AddLinksEditor.resolveIssues(issueContext.getReader(), connection, Collections.singletonList(myOppositeKey));
      if (issues.size() != 1) return;
      myType.createLinks(issueContext, issues);
    }

    @Override
    public boolean getOutward() {
      return myType.getOutward();
    }

    @Override
    public long getType() {
      return myType.getType();
    }

    @Override
    public String getDescription(GuiFeaturesManager manager) {
      return myType.getDescription();
    }

    @Override
    public String getOppositeString(@NotNull TypedKey<String> key) {
      if (KEY.equals(key)) return myOppositeKey;
      else if (SUMMARY.equals(key)) return myOppositeSummary;
      return null;
    }

    @Override
    public ItemKey getOppositeEnum(GuiFeaturesManager manager, @NotNull TypedKey<ItemKey> key) {
      if (STATUS.equals(key)) return getItemKey(manager, Status.ENUM_TYPE, myOppositeStatus);
      else if (ISSUE_TYPE.equals(key)) return getItemKey(manager, IssueType.ENUM_TYPE, myOppositeIssueType);
      else if (PRIORITY.equals(key)) return getItemKey(manager, Priority.ENUM_TYPE, myOppositePriority);
      return null;
    }

    private ItemKey getItemKey(GuiFeaturesManager manager, DBStaticObject enumType, long item) {
      EnumTypesCollector.Loaded type = manager.getEnumTypes().getType(enumType);
      return type == null ? null : type.getResolvedItem(item);
    }
    
    public void applyOppositeInfo(ItemVersion oppositeIssue) {
      myOppositeSummary = oppositeIssue.getValue(Issue.SUMMARY);
      myOppositeStatus = oppositeIssue.getNNValue(Issue.STATUS, 0l);
      myOppositeIssueType = oppositeIssue.getNNValue(Issue.ISSUE_TYPE, 0l);
      myOppositePriority = oppositeIssue.getNNValue(Issue.PRIORITY, 0l);
    }

    public void markAsUnresolved() {
      myOppositeSummary = "<Issue not found>";
    }
  }

  public static final AnAction ADD_LINK = new SimpleAction("Add Links\u2026", Icons.ACTION_CREATE_LINK) {
    {
      watchRole(ROLE);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      LinksEditor editor = context.getSourceObject(ROLE);
      EditItemModel model = context.getSourceObject(EDITOR_MODEL);
      CantPerformException.ensureNotNull(editor.getSourceKey(model));
      CantPerformException.ensure(EngineConsts.getConnectionItem(model) > 0);
      CantPerformException.ensure(AddLinksDialog.checkModel(model));
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      final EditItemModel issueModel = context.getSourceObject(EDITOR_MODEL);
      LinksEditor editor = context.getSourceObject(ROLE);
      Pair<DirectionalLinkType, List<String>> pair = AddLinksDialog.show(context, issueModel, editor.getSourceKey(issueModel));
      if (pair == null) return;
      DirectionalLinkType linkType = pair.getFirst();
      editor.addLinks(issueModel, linkType, pair.getSecond(), context.getSourceObject(Database.ROLE));
      AddLinksDialog.updateLastLink(issueModel, linkType);
      if (linkType != null) issueModel.putHint(editor.myLastLinkType, linkType);
    }
  };

  public static final AnAction REMOVE_LINKS = new SimpleAction("Remove Links", Icons.ACTION_REMOVE_LINK) {
    {
      watchRole(ROLE);
      watchRole(LoadedLink.LINK);
    }
    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(ROLE);
      context.getSourceObject(EDITOR_MODEL);
      List<LoadedLink> links = context.getSourceCollection(LoadedLink.LINK);
      CantPerformException.ensureNotEmpty(links);
      context.putPresentationProperty(PresentationKey.NAME, links.size() > 1 ? "Remove Links" : "Remove Link");
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      LinksEditor editor = context.getSourceObject(ROLE);
      EditItemModel model = context.getSourceObject(EDITOR_MODEL);
      List<LoadedLink> links = context.getSourceCollection(LoadedLink.LINK);
      editor.removeLinks(model, links);
    }
  };
}
