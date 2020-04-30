package com.almworks.jira.provider3.gui.edit.editors.move;

import com.almworks.api.application.ItemKey;
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
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

class IssueTypeEditor extends SingleEnumDelegatingEditor<DropdownEnumEditor> {
  private static final CanvasRenderer<ItemKey> RENDERER = new CanvasRenderer<ItemKey>() {
    @Override
    public void renderStateOn(CellState state, Canvas canvas, ItemKey item) {
      if (item == null) return;
      if (IssueType.isSubtask(item, true)) canvas.emptySection().setFontStyle(Font.ITALIC);
      item.renderOn(canvas, state);
      canvas.setIcon(item.getIcon());
    }
  };
  private static final DropdownEnumEditor DROPDOWN =
    new DropdownEditorBuilder().setVariants(new IssueTypeVariants())
      .setAttribute(Issue.ISSUE_TYPE)
      .setDefaultItem(DefaultItemSelector.ANY)
      .setLabelText(NameMnemonic.parseString("Issue &Type"))
      .setVerify(true)
      .overrideRenderer(RENDERER)
      .createFixed();

  private final MoveParentEditor myParentEditor;

  IssueTypeEditor(boolean editParent) {
    super(DROPDOWN.getAttribute(), DROPDOWN.getVariants());
    myParentEditor = editParent ? new MoveParentEditor(NameMnemonic.rawText("Parent")) : null;
  }

  @Nullable
  @Override
  protected DropdownEnumEditor getDelegate(VersionSource source, EditModelState model) {
    return DROPDOWN;
  }

  @Override
  protected void prepareWrapper(VersionSource source, ModelWrapper<DropdownEnumEditor> wrapper, EditPrepare editPrepare) {
    EditItemModel unwrappedModel = wrapper.getOriginalModel();
    MoveController controller = MoveController.ensureLoaded(source, unwrappedModel);
    controller.setTypeEditor(this);
    super.prepareWrapper(source, wrapper, editPrepare);
    if (myParentEditor == null) return;
    Boolean allSubtasks = areAllSubtasks(source, unwrappedModel);
    boolean editParent;
    if (allSubtasks == null) editParent = true;
    else if (!allSubtasks) {
      if (isSameProject(source, unwrappedModel) && !anyHasSubtasks(source, unwrappedModel)) {
        controller.setCurrentMode(unwrappedModel, MoveController.MODE_ALL);
        editParent = true;
      } else {
        controller.setCurrentMode(unwrappedModel, MoveController.MODE_GENERIC);
        editParent = false;
      }
    } else if (isSameProject(source, unwrappedModel)) { // allSubtasks from same project
      controller.setCurrentMode(unwrappedModel, MoveController.MODE_ALL);
      editParent = true;
    } else { // Subtask from different projects
      return;
    }
    if (editParent) {
      myParentEditor.prepareModel(source, unwrappedModel, editPrepare);
      controller.setParentEditor(myParentEditor);
    }
  }

  public static boolean isSameProject(VersionSource source, EditItemModel model) {
    List<ItemVersion> issues = source.readItems(model.getEditingItems());
    if (issues.isEmpty()) return true;
    Long commonProject = null;
    for (ItemVersion issue : issues) {
      Long project = issue.getValue(Issue.PROJECT);
      if (commonProject == null) commonProject = project;
      else if (project != null && !commonProject.equals(project)) return false;
    }
    return true;
  }

  private boolean anyHasSubtasks(VersionSource source, EditItemModel model) {
    List<ItemVersion> issues = source.readItems(model.getEditingItems());
    for (ItemVersion issue : issues) if (!Issue.getSubtasks(issue).isEmpty()) return true;
    return false;
  }

  private static Boolean areAllSubtasks(VersionSource source, EditItemModel model) {
    List<ItemVersion> issues = source.readItems(model.getEditingItems());
    Boolean hasSubtasks = null;
    Boolean hasGeneric = null;
    for (ItemVersion issue : issues) {
      Boolean subtask = IssueType.getSubtask(issue.getReader(), issue.getValue(Issue.ISSUE_TYPE));
      if (subtask == null) continue;
      boolean isGeneric = !subtask;
      hasSubtasks = MoveController.changeFlag(hasSubtasks, subtask, isGeneric);
      hasGeneric = MoveController.changeFlag(hasGeneric, isGeneric, subtask);
    }
    if (hasSubtasks == null) return hasGeneric != null ? !hasGeneric : null;
    if (hasGeneric == null) return hasSubtasks;
    if (hasSubtasks) return hasGeneric ? null : true;
    return hasGeneric ? false : null;
  }

  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    List<? extends ComponentControl> components = super.createComponents(life, model);
    MoveController controller = MoveController.getInstance(model);
    if (controller != null && controller.getCurrentMode(model) == MoveController.MODE_DISABLED)
      components = ComponentControl.EnableWrapper.disableAll(components);
    if (myParentEditor == null || !model.getAllEditors().contains(myParentEditor)) return components;
    if (components.size() != 1) {
      LogHelper.error("Expected one type editor component", components);
      return components;
    }
    ComponentControl parentControl = myParentEditor.createComponent(life, model);
    if (parentControl == null) return components;
    ComponentControl typeControl = components.get(0);
    Form component = new Form(typeControl, parentControl, getWrapperModel(model));
    component.attach(life, model);
    return Collections.singletonList(component);
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    MoveController.performCommit(context);
  }

  public void updateDefaults(CommitContext context) throws CancelCommitException {
    BaseSingleEnumEditor.wrapperUpdateDefaults(context, this);
  }

  private static class Form implements ComponentControl {
    private final JPanel myWholePanel = new JPanel(UIUtil.createBorderLayout());
    private final JLabel myOfLabel = new JLabel("of");
    private final JPanel myParentPanel;
    private final ComponentControl myTypeControl;
    private final ModelWrapper<DropdownEnumEditor> myTypeWrapper;
    private final ComponentControl myParentControl;

    private Form(ComponentControl typeControl, ComponentControl parentControl, ModelWrapper<DropdownEnumEditor> typeWrapper) {
      myTypeControl = typeControl;
      myTypeWrapper = typeWrapper;
      myParentControl = parentControl;
      InlineLayout layout = InlineLayout.horizontal(5);
      layout.setLastTakesAllSpace(true);
      myParentPanel = new JPanel(layout);
      myParentPanel.add(myOfLabel);
      myParentPanel.add(myParentControl.getComponent());
      myWholePanel.add(myTypeControl.getComponent(), BorderLayout.CENTER);
      myWholePanel.add(myParentPanel, BorderLayout.EAST);
    }

    @NotNull
    @Override
    public JComponent getComponent() {
      return myWholePanel;
    }

    @NotNull
    @Override
    public Dimensions getDimension() {
      return Dimensions.SINGLE_LINE;
    }

    @NotNull
    @Override
    public Enabled getEnabled() {
      return myTypeControl.getEnabled();
    }

    @Override
    public void setEnabled(boolean enable) {
      myTypeControl.setEnabled(enable);
      Enabled enabled = myParentControl.getEnabled();
      if (enabled == Enabled.ENABLED || enabled == Enabled.DISABLED) {
        myParentControl.setEnabled(enable);
        myOfLabel.setEnabled(enable);
      }
    }

    @Override
    public NameMnemonic getLabel() {
      return myTypeControl.getLabel();
    }

    public void attach(Lifespan life, final EditItemModel model) {
      model.addAWTChangeListener(life, new ChangeListener() {
        @Override
        public void onChange() {
          ItemKey type = myTypeWrapper.getEditor().getCurrentValue(myTypeWrapper);
          boolean visible = !IssueType.isSubtask(type, false);
          myParentPanel.setVisible(visible);
        }
      });
    }
  }
}
