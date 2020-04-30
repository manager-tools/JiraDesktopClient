package com.almworks.jira.provider3.gui.edit.editors.move;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.SingleEnumDelegatingEditor;
import com.almworks.items.gui.edit.editors.enums.DefaultItemSelector;
import com.almworks.items.gui.edit.editors.enums.single.BaseSingleEnumEditor;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEnumEditor;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import java.util.List;

class ProjectEditor extends SingleEnumDelegatingEditor<DropdownEnumEditor> {
  private static final DropdownEnumEditor DROPDOWN =
    new DropdownEditorBuilder().setStaticVariants(Project.ENUM_TYPE, "project")
      .setAttribute(Issue.PROJECT)
      .setDefaultItem(DefaultItemSelector.ANY)
      .setLabelText(NameMnemonic.parseString("&Project"))
      .setVerify(true)
      .createFixed();

  public static final ProjectEditor INSTANCE = new ProjectEditor();

  private ProjectEditor() {
    super(DROPDOWN.getAttribute(), DROPDOWN.getVariants());
  }

  @Override
  protected DropdownEnumEditor getDelegate(VersionSource source, EditModelState model) {
    return DROPDOWN;
  }

  @Override
  protected void prepareWrapper(VersionSource source, ModelWrapper<DropdownEnumEditor> wrapper, EditPrepare editPrepare) {
    MoveController move = MoveController.ensureLoaded(source, wrapper);
    move.setProjectEditor(this);
    DropdownEnumEditor editor = wrapper.getEditor();
    if (move.isGenericOnly()) editor.prepareModel(source, wrapper, editPrepare); // Edit model issues themselves
    else {
      LongList parents = move.getAllParents();
      boolean hasDummy = false;
      for (ItemVersion parent : source.readItems(parents)) {
        if (ItemDownloadStage.getValue(parent) == ItemDownloadStage.DUMMY) {
          hasDummy = true;
          break;
        }
      }
      // If has subtasks the project editor edit project of parent issues, not the edited issues
      editor.prepareModel(source, wrapper, hasDummy ? wrapper.getEditingItems() : parents);
    }
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    ModelWrapper<DropdownEnumEditor> wrapper = getWrapperModel(model);
    if (wrapper == null) return Collections15.emptyList();
    List<? extends ComponentControl> components = wrapper.getEditor().createComponents(life, wrapper);
    MoveController controller = MoveController.getInstance(wrapper);
    if (controller == null || controller.isGenericOnly()) return components;
    return ComponentControl.EnableWrapper.disableAll(components); // Do not allow edit project if some issues are subtasks
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    MoveController.performCommit(context);
  }

  public void updateDefaults(CommitContext context) throws CancelCommitException {
    BaseSingleEnumEditor.wrapperUpdateDefaults(context, this);
  }
}

