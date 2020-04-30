package com.almworks.jira.provider3.links.actions;

import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.MockEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.gui.edit.util.SimpleComponentControl;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collections;
import java.util.List;

class DirectionalTypeEditor extends MockEditor {
  private static final CanvasRenderable.TextRenderable NO_LINK_TYPE = new CanvasRenderable.TextRenderable(
    Font.ITALIC, "<Link type>");
  private final TypedKey<DirectionalLinkType> myTypeKey;
  private final TypedKey<DirectionalLinkType> myInitialType;

  public DirectionalTypeEditor(NameMnemonic labelText) {
    super(labelText);
    myTypeKey = TypedKey.create(labelText + "/type");
    myInitialType = TypedKey.create(labelText + "/initialType");
  }

  @Override
  public void prepareModel(VersionSource source, EditItemModel model, EditPrepare editPrepare) {
    if (EngineConsts.ensureGuiFeatureManager(source, model) == null || EngineConsts.getConnectionItem(model) <= 0) return;
    model.registerEditor(this);
    loadLastLinkType(source, model);
  }

  private void loadLastLinkType(VersionSource source, EditItemModel model) {
    long connection = EngineConsts.getConnectionItem(model);
    if (connection <= 0) return;
    byte[] bytes = source.forItem(connection).getValue(Jira.LAST_LINK_TYPE);
    if (bytes == null || bytes.length == 0) return;
    ByteArray.Stream stream = new ByteArray.Stream(bytes);
    long linkType = stream.nextLong();
    boolean outward = stream.nextBoolean();
    if (!stream.isSuccessfullyAtEnd()) return;
    DirectionalLinkType loaded = DirectionalLinkType.load(source.forItem(linkType), outward);
    if (loaded != null) {
      model.putHint(myInitialType, loaded);
      model.putHint(myTypeKey, loaded);
    }
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    AComboBox<DirectionalLinkType> combo = new AComboBox<DirectionalLinkType>();
    ComponentControl control = attach(life, model, combo);
    return control != null ? Collections.singletonList(control) : Collections15.<ComponentControl>emptyList();
  }

  /**
   * Set the current value and stores it as initial.
   */
  public void setValue(EditItemModel model, DirectionalLinkType linkType) {
    model.putValue(myTypeKey, linkType);
    model.putHint(myInitialType, linkType);
  }

  @Nullable
  public ComponentControl attach(Lifespan life, final EditItemModel model, AComboBox<DirectionalLinkType> combo) {
    GuiFeaturesManager features = EngineConsts.getGuiFeaturesManager(model);
    long connection = EngineConsts.getConnectionItem(model);
    if (features == null || connection <= 0) return null;
    DirectionalLinkType linkType = model.getValue(myTypeKey);
    final SelectionInListModel<DirectionalLinkType> comboModel = setupTypesCombo(life, combo, features, connection);
    comboModel.setSelectedItem(linkType);
    comboModel.addSelectionChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        DirectionalLinkType type = comboModel.getSelectedItem();
        model.putValue(myTypeKey, type);
      }
    });
    FieldEditorUtil.registerComponent(model, this, combo);
    return SimpleComponentControl.singleLine(combo, this, model, ComponentControl.Enabled.NOT_APPLICABLE);
  }

  public static SelectionInListModel<DirectionalLinkType> setupTypesCombo(Lifespan life, AComboBox<DirectionalLinkType> combo,
    GuiFeaturesManager features, long connection)
  {
    combo.setCanvasRenderer(Renderers.nullRenderer(DirectionalLinkType.RENDERER, NO_LINK_TYPE));
    AListModel<DirectionalLinkType> typesModel = DirectionalLinkType.createModel(life, features, connection);
    final SelectionInListModel<DirectionalLinkType> comboModel = SelectionInListModel.create(life, typesModel, null);
    combo.setModel(comboModel);
    return comboModel;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return getValue(model) != null;
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    return !Util.equals(model.getValue(myTypeKey), model.getValue(myInitialType));
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    DirectionalLinkType type = getValue(verifyContext.getModel());
    if (type != null) return;
    verifyContext.addError(this, "Please select link type");
  }

  public DirectionalLinkType getValue(EditModelState model) {
    return model.getValue(myTypeKey);
  }

  public void storeLinkType(CommitContext context, Long connection) {
    if (connection == null || connection <= 0) return;
    DirectionalLinkType newType = context.getModel().getValue(myTypeKey);
    DirectionalLinkType initialType = context.getModel().getValue(myInitialType);
    if (newType == null || newType.equals(initialType)) return;
    storeLinkType(context.getDrain(), connection, newType);
  }

  public static void storeLinkType(DBDrain drain, Long connection, DirectionalLinkType newType) {
    if (connection == null || connection <= 0 || newType == null) return;
    ByteArray bytes = new ByteArray();
    bytes.addLong(newType.getType());
    bytes.addBoolean(newType.getOutward());
    drain.changeItem(connection).setValue(Jira.LAST_LINK_TYPE, bytes.toNativeArray());
  }
}
