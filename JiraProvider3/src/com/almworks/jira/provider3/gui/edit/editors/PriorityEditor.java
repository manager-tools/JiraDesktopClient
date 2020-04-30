package com.almworks.jira.provider3.gui.edit.editors;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.composition.SingleEnumDelegatingEditor;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEnumEditor;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncState;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Priority;
import com.almworks.util.LogHelper;
import com.almworks.util.components.AComboBox;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.detach.Lifespan;

public class PriorityEditor extends SingleEnumDelegatingEditor<DropdownEnumEditor> {
  private static final DropdownEnumEditor EDIT = buildPriorityEditor().setAppendNull(false).createFixed();
  private static final DropdownEnumEditor CREATE = buildPriorityEditor().createFixed();

  public PriorityEditor() {
    super(EDIT.getAttribute(), EDIT.getVariants());
  }

  @Override
  protected DropdownEnumEditor getDelegate(VersionSource source, EditModelState model) {
    if (model.isNewItem()) return CREATE;
    LongList items = model.getEditingItems();
    for (ItemVersion itemVersion : source.readItems(items)) if (itemVersion.getSyncState() == SyncState.NEW) return CREATE;
    return EDIT;
  }

  private static DropdownEditorBuilder buildPriorityEditor() {
    return new DropdownEditorBuilder().setStaticVariants(Priority.ENUM_TYPE, MetaSchema.CONFIG_PRIORITY)
      .setAttribute(Issue.PRIORITY)
      .setNullPresentation("<Default Priority>")
      .setLabelText(NameMnemonic.parseString("Pri&ority"));
  }

  public void attachCombo(Lifespan life, EditItemModel model, AComboBox<ItemKey> comboBox) {
    ModelWrapper<DropdownEnumEditor> wrapper = getWrapperModel(model);
    if (wrapper == null) {
      LogHelper.error("Missing wrapper model");
      return;
    }
    wrapper.getEditor().attachCombo(life, wrapper, comboBox);
  }
}
