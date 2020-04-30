package com.almworks.jira.provider3.links.actions;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.NestedModelEditor;
import com.almworks.items.gui.edit.util.CompositeComponentControl;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.PerItemTransactionCache;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.components.AComboBox;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This editor allow user to add create links to single or several issue all of the same link type and direction. Issues
 * are entered via {@link KeyListEditor}.<br>
 * This editor adds new links to specified issues to any issue it is committed (to all issued edited by the model). If an
 * edited issue already has a link of same type and direction to user specified issue no duplicated link is created.
 */
public class AddLinksEditor extends NestedModelEditor {
  private static final PerItemTransactionCache<Map<String, Long>> RESOLVED_ISSUES = PerItemTransactionCache.create("resolvedIssues");
  private final boolean myInline;
  private final DirectionalTypeEditor myTypeEditor;
  private final KeyListEditor myKeysEditor;

  private AddLinksEditor(NameMnemonic labelText, boolean inline, NameMnemonic keysLabel, boolean singleKey, NameMnemonic typeLabel) {
    super(labelText);
    myInline = inline;
    myTypeEditor = new DirectionalTypeEditor(typeLabel);
    myKeysEditor = new KeyListEditor(keysLabel, singleKey);
  }

  public static AddLinksEditor linkIssues(NameMnemonic keysLabel, NameMnemonic typeLabel) {
    return new AddLinksEditor(NameMnemonic.EMPTY, false, keysLabel, false, typeLabel);
  }

  public static AddLinksEditor inlineAddLinks(NameMnemonic labelText, boolean single) {
    return new AddLinksEditor(labelText, true, labelText, single, labelText);
  }

  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    DefaultEditModel.Child nested = DefaultEditModel.Child.editItems(parent, LongList.EMPTY, false);
    return Pair.create(nested, Collections15.unmodifiableListCopy(myKeysEditor, myTypeEditor));
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    if (!myInline) return super.createComponents(life, model);
    EditModelState nested = getNestedModel(model);
    if (nested == null) return Collections15.emptyList();
    AComboBox<DirectionalLinkType> combo = new AComboBox<DirectionalLinkType>();
    JTextField keys = new JTextField();
    keys.setColumns(15);
    ComponentControl control = attach(life, model, combo, keys);
    if (control == null) return Collections.emptyList();
    JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(keys, BorderLayout.CENTER);
    panel.add(combo, BorderLayout.WEST);
    return CompositeComponentControl.single(ComponentControl.Dimensions.SINGLE_LINE, this, model, getLabelText(model), ComponentControl.Enabled.NOT_APPLICABLE, panel, control);
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    EditModelState nested = getNestedModel(model);
    return nested != null && myKeysEditor.hasValue(nested) && myTypeEditor.hasValue(nested);
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    EditModelState nested = getNestedModel(verifyContext.getModel());
    if (nested == null) return;
    if (!myKeysEditor.hasValue(nested) && !myTypeEditor.hasValue(nested)) return;
    super.verifyData(verifyContext);
  }

  public ComponentControl attach(Lifespan life, EditItemModel model, AComboBox<DirectionalLinkType> typeCombo, JTextComponent keysList) {
    EditItemModel nested = getNestedModel(model);
    if (nested == null) return null;
    ComponentControl typeComponent = myTypeEditor.attach(life, nested, typeCombo);
    ComponentControl keysComponent = myKeysEditor.attachTextComponent(life, nested, keysList, null);
    if (typeComponent == null || keysComponent == null) return null;
    return CompositeComponentControl.create(
      myInline ? ComponentControl.Dimensions.SINGLE_LINE : ComponentControl.Dimensions.TALL, this, model,
      getLabelText(model), ComponentControl.Enabled.NOT_APPLICABLE, null, typeComponent, keysComponent);
  }

  @Override
  public void commit(CommitContext parentContext) {
    EditItemModel nested = getNestedModel(parentContext.getModel());
    if (nested == null) return;
    CommitContext context = parentContext.subContext(nested);
    Long connection = context.readTrunk().getValue(SyncAttributes.CONNECTION);
    if (connection == null || connection <= 0) {
      LogHelper.error("Missing connection", context);
      return;
    }
    LogHelper.assertError(Util.equals(context.readTrunk().getValue(DBAttribute.TYPE), context.getReader().findMaterialized(Issue.DB_TYPE)), "Not an issue", context);
    LongList issues = myKeysEditor.getIssueItems(context.getReader(), connection, nested);
    DirectionalLinkType type = myTypeEditor.getValue(nested);
    if (issues.isEmpty() || type == null) return;
    type.createLinks(context, issues);
    myTypeEditor.storeLinkType(context, connection);
  }
  
  public DirectionalLinkType getLinkType(EditItemModel model) {
    DefaultEditModel.Child nested = getNestedModel(model);
    return nested != null ? myTypeEditor.getValue(nested) : null;
  }
  
  public List<String> getIssueKeys(EditItemModel model) {
    DefaultEditModel.Child nested = getNestedModel(model);
    return nested != null ? myKeysEditor.getIssueKeys(nested) : Collections.<String>emptyList();
  }

  /**
   * @see DirectionalTypeEditor#setValue(com.almworks.items.gui.edit.EditItemModel, DirectionalLinkType)
   */
  public void setLinkType(EditItemModel model, DirectionalLinkType link) {
    DefaultEditModel.Child nested = getNestedModel(model);
    if (nested == null) return;
    myTypeEditor.setValue(nested, link);
  }

  public static LongList resolveIssues(DBReader reader, long connection, List<String> keys) {
    if (keys.isEmpty()) return LongList.EMPTY;
    Map<String, Long> map = RESOLVED_ISSUES.get(reader, connection);
    if (map == null) {
      map = Collections15.hashMap();
      RESOLVED_ISSUES.put(reader, connection, map);
    }
    LongArray result = new LongArray();
    for (String key : keys) {
      if (key == null) continue;
      Long issue = map.get(key);
      if (issue == null) {
        LongArray found =
          reader.query(DPEquals.create(Issue.KEY, key).and(DPEquals.create(SyncAttributes.CONNECTION, connection)))
            .copyItemsSorted();
        if (found.isEmpty()) continue;
        LogHelper.assertError(found.size() == 1, "Duplicated key", key, found);
        issue = found.get(0);
        if (SyncUtils.readTrunk(reader, issue).isInvisible()) continue;
        map.put(key, issue);
      }
      result.add(issue);
    }
    result.sortUnique();
    return result;
  }

  public void setOppositeKeys(EditItemModel model, List<String> keys) {
    EditItemModel nested = getNestedModel(model);
    if (nested == null) LogHelper.error("Missing model", model, keys);
    else myKeysEditor.setIssueKeys(nested, keys);
  }
}
