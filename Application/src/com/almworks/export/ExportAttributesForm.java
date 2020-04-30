package com.almworks.export;

import com.almworks.api.application.util.ItemExport;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.util.AppBook;
import com.almworks.util.LogHelper;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.SubsetModel;
import com.almworks.util.components.SubsetEditor;
import com.almworks.util.i18n.Local;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

class ExportAttributesForm {
  private JButton mySameAsInBugsTable;
  private JPanel mySubsetEditorPlace;
  private JPanel myWholePanel;
  private JLabel myBanner;
  private final DialogManager myDialogManager;
  private final SubsetModel<ItemExport> myAttributesModel;
  private final List<String> myTableColumns;

  private SubsetEditor<ItemExport> mySubsetEditor = null;

  public ExportAttributesForm(DialogManager dialogManager, SubsetModel<ItemExport> attributesModel,
    List<String> tableColumns)
  {
    myDialogManager = dialogManager;
    myAttributesModel = attributesModel;
    myTableColumns = tableColumns;
    AppBook.replaceText("ExportAttributesForm", myWholePanel);
    setupSameButton();
    setupVisual();
  }

  private void setupVisual() {
    UIUtil.adjustFont(myBanner, -1, Font.BOLD, false);
  }

  private void setupSameButton() {
    mySameAsInBugsTable.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setupFieldsFromTable(myAttributesModel, myTableColumns);
      }
    });
    NameMnemonic
      .parseString(Local.parse("&Same as in " + Terms.ref_Artifacts + " Table"))
      .setToButton(mySameAsInBugsTable);
  }

  static void setupFieldsFromTable(SubsetModel<ItemExport> attributesModel, List<String> tableColumns) {
    List<ItemExport> exports = Collections15.arrayList();
    List<ItemExport> all = attributesModel.getFullSet().toList();
    for (String column : tableColumns) {
      if (column == null) continue;
      ItemExport export = null;
      for (ItemExport itemExport : all) {
        if (column.equals(itemExport.getId())) {
          export = itemExport;
          break;
        }
      }
      if (export == null) LogHelper.warning("Export not found", column);
      else exports.add(export);
    }
    attributesModel.setSubset(exports);
  }

  public void show() {
    maybeInitializeSubsetEditor();
    DialogBuilder builder = myDialogManager.createBuilder("exportAttributesForm");
    builder.setTitle("Select Attributes for Export");
    builder.setModal(true);
    builder.setCancelAction("Close Window");
    builder.setContent(myWholePanel);
    builder.showWindow();
  }

  private void maybeInitializeSubsetEditor() {
    if (mySubsetEditor != null)
      return;
    mySubsetEditor = SubsetEditor.create(myAttributesModel, ItemExport.DISPLAY_NAME_COMPARATOR, "&Selected Attributes:",
      "A&vailable Attributes:", true);
    mySubsetEditor.setCanvasRenderer(ItemExport.DISPLAY_NAME_RENDERER);
    mySubsetEditorPlace.setLayout(new SingleChildLayout(SingleChildLayout.CONTAINER));
    mySubsetEditorPlace.add(mySubsetEditor.getComponent());
  }
}
