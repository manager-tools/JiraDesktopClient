package com.almworks.jira.provider3.links.actions;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.ModelKey;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.links.LoadedLink;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Convertors;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ATable;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.models.TableColumnBuilder;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CreateLinksOutboundForm {
  private static final TableColumnAccessor<Map<TypedKey<?>, ?>, String> KEY_COLUMN = createColumn(LoadedLink.KEY);
  private static final TableColumnAccessor<Map<TypedKey<?>, ?>, String> SUMMARY_COLUMN = createColumn(LoadedLink.SUMMARY);
    // todo add STP icons column

  private ATable<Map<TypedKey<?>, ?>> mySourceIssue;
  private JTextArea myOppositeIssues;
  private AComboBox<DirectionalLinkType> myLinkType;
  private JPanel myWholePanel;
  private final GuiFeaturesManager myFeatures;

  public CreateLinksOutboundForm(GuiFeaturesManager features) {
    myFeatures = features;
    myLinkType.setCanvasRenderer(DirectionalLinkType.RENDERER);
    setupTable(mySourceIssue);
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);
    mySourceIssue.setColumnModel(FixedListModel.create(KEY_COLUMN, SUMMARY_COLUMN));
  }

  private static TableColumnAccessor<Map<TypedKey<?>, ?>, String> createColumn(TypedKey<String> key) {
    return new TableColumnBuilder<Map<TypedKey<?>, ?>, String>()
      .setId(key.getName())
      .setValueCanvasRenderer(Renderers.defaultCanvasRenderer())
      .setConvertor(Convertors.getFromMap(key))
      .createColumn();
  }

  private void setupTable(ATable<?> table) {
    table.setGridHidden();
    final JComponent jtable = table.getSwingComponent();
    jtable.setEnabled(false);
    jtable.setFocusable(false);
  }

  public void setSourceIssues(List<LoadedItem> issues) {
    ArrayList<Map<TypedKey<?>, ?>> maps = Collections15.arrayList();
    ModelKey<String> key = MetaSchema.issueKey(myFeatures);
    ModelKey<String> summary = MetaSchema.issueSummary(myFeatures);
    for (LoadedItem issue : issues) {
      if (issue == null) continue;
      HashMap<TypedKey<?>, Object> map = Collections15.hashMap();
      copyValue(issue, key, map, LoadedLink.KEY);
      copyValue(issue, summary, map, LoadedLink.SUMMARY);
      maps.add(map);
    }
    mySourceIssue.setCollectionModel(FixedListModel.create(maps));
  }

  public void setSourceKeys(List<String> keys) {
    ArrayList<Map<TypedKey<?>, ?>> maps = Collections15.arrayList();
    for (String key : keys) {
      HashMap<TypedKey<?>, Object> map = Collections15.hashMap();
      LoadedLink.KEY.putTo(map, key);
      maps.add(map);
    }
    mySourceIssue.setCollectionModel(FixedListModel.create(maps));
  }

  private void copyValue(LoadedItem source, ModelKey<String> modelKey, HashMap<TypedKey<?>, Object> target,
    TypedKey<String> key)
  {
    if (modelKey == null) return;
    key.putTo(target, source.getModelKeyValue(modelKey));
  }

  public AComboBox<DirectionalLinkType> getLinkType() {
    return myLinkType;
  }

  public JTextArea getOppositeIssues() {
    return myOppositeIssues;
  }

  public JPanel getWholePanel() {
    return myWholePanel;
  }

  private void createUIComponents() {
    myWholePanel = new JPanel() {
      private boolean myWidthsUpdate = false;

      @Override
      public void reshape(int x, int y, int w, int h) {
        boolean widthChanged = myWidthsUpdate || (w != getWidth());
        super.reshape(x, y, w, h);
        if (widthChanged) {
          mySourceIssue.forcePreferredColumnWidths();
          myWidthsUpdate = true;
        }
      }
    };
  }
}
