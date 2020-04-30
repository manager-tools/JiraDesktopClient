package com.almworks.jira.provider3.issue.editor;

import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.composition.NestedModelEditor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.util.FieldEditorUtil;
import com.almworks.items.sync.EditPrepare;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.gui.edit.editors.move.ParentSupport;
import com.almworks.jira.provider3.gui.edit.fields.LoadedFieldInfo;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class ScreenIssueEditor extends NestedModelEditor {
  private static final String C_CONTROLLER = "controller";
  private static final String C_SCREENS = "screens";
  public static final AnAction CONFIGURE_VISIBLE_FIELDS = new VisibleFieldsAction();

  private static final TypedKey<ScreenController> CONTROLLER = TypedKey.create("screenController");
  private static final TypedKey<ScreenIssueEditor> EDITOR = TypedKey.create("issueEditor");

  private final boolean myCommitInvisible;
  private final ScreenSet myScreenSet;

  /**
   * @param commitInvisible if false commits only editors from selected screen (when commit performed), otherwise commits all editors
   * @param screenSet configures set of screens for the editor
   */
  public ScreenIssueEditor(boolean commitInvisible, ScreenSet screenSet) {
    super(NameMnemonic.rawText("Issue Editor"));
    myCommitInvisible = commitInvisible;
    myScreenSet = screenSet;
  }

  public static EditDescriptor descriptor(final EditDescriptor.Impl descriptor) {
    return new MyDescriptor(descriptor);
  }

  @Nullable
  @Override
  protected Pair<DefaultEditModel.Child, ? extends List<? extends FieldEditor>> createNestedModel(VersionSource source, EditItemModel parent, EditPrepare editPrepare) {
    parent.getRootModel().putHint(EDITOR, this);
    LongList items = parent.getEditingItems();
    JiraConnection3 connectionObject = EngineConsts.getConnection(JiraConnection3.class, parent);
    if  (connectionObject == null) {
      LogHelper.error("Missing connection");
      return null;
    }
    DefaultEditModel.Child child = DefaultEditModel.Child.editItems(parent, items, false);
    EngineConsts.setupNestedModel(parent, child);
    ParentSupport.copyParent(parent, child);
    List<FieldEditor> editors = myScreenSet.install(source, child);
    FieldsFilter filter = FieldsFilter.load(source.forItem(connectionObject.getConnectionItem()));
    LoadedFieldInfo fieldInfo = LoadedFieldInfo.ensureLoaded(source, child);
    if (editors == null || fieldInfo == null) {
      LogHelper.error("Missing editors", myScreenSet);
      return null;
    }
    Prepared.putToModel(child, editors, filter, fieldInfo);
    return Pair.create(child, editors);
  }

  @Override
  public void commit(CommitContext context) throws CancelCommitException {
    DefaultEditModel.Child nested = getNestedModel(context.getModel());
    if (nested == null) return;
    CommitContext nestedCommit = context.subContext(nested);
    for (FieldEditor editor : getEnabledEditors(context.getModel())) if (editor.hasDataToCommit(nested)) editor.commit(nestedCommit);
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return FieldEditorUtil.hasDataToCommit(getNestedModel(model), getEnabledEditors(model));
  }

  @Override
  public boolean isChanged(EditItemModel model) {
    DefaultEditModel.Child nested = getNestedModel(model);
    if (nested == null) return false;
    for (FieldEditor editor : getEnabledEditors(model)) if (editor.isChanged(nested)) return true;
    return false;
  }

  @Override
  public void verifyData(DataVerification verifyContext) {
    EditItemModel nested = getNestedModel(verifyContext.getModel());
    if (nested == null) return;
    DataVerification nestedContext = verifyContext.subContext(nested);
    for (FieldEditor editor : getEnabledEditors(verifyContext.getModel())) editor.verifyData(nestedContext);
    LoadedFieldInfo fieldInfo = LoadedFieldInfo.getInstance(nested);
    if (fieldInfo != null) fieldInfo.checkMandatory(nestedContext, getEnabledEditors(verifyContext.getModel()));
  }

  private List<FieldEditor> getEnabledEditors(EditItemModel model) {
    DefaultEditModel.Child nested = getNestedModel(model);
    if (nested == null) return Collections.emptyList();
    List<FieldEditor> enabledEditors = nested.getEnabledEditors();
    if (myCommitInvisible) return enabledEditors;
    ScreenController controller = model.getValue(CONTROLLER);
    if (controller == null) {
      LogHelper.error("Missing screen controller");
      return enabledEditors;
    }
    Collection<FieldEditor> controlledEditors = ResolvedField.getEditorsMap(nested).values();
    List<FieldEditor> visibleEditors = controller.getVisibleEditors();
    ArrayList<FieldEditor> result = Collections15.arrayList(enabledEditors);
    for (Iterator<FieldEditor> it = result.iterator(); it.hasNext(); ) {
      FieldEditor editor = it.next();
      if (!visibleEditors.contains(editor) && controlledEditors.contains(editor)) it.remove();
    }
    return result;
  }

  /**
   * Should not be called. Use {@link #createComponent(org.almworks.util.detach.Lifespan, com.almworks.items.gui.edit.EditItemModel, com.almworks.util.config.Configuration)} instead
   */
  @NotNull
  @Override
  public List<? extends ComponentControl> createComponents(Lifespan life, EditItemModel model) {
    LogHelper.error("Should not happen");
    return Collections.emptyList();
  }

  @Nullable
  public JComponent createComponent(Lifespan life, final EditItemModel model, Configuration config) {
    EditItemModel nested = getNestedModel(model);
    Prepared prepared = Prepared.getInstance(nested);
    if (nested == null || prepared == null) {
      return null;
    }
    ControlledEditors editors = ControlledEditors.create(life, nested, prepared.getEditors());
    ScreenController controller = ScreenController.install(nested, prepared.getFilter(), prepared.getFieldInfo(), config.getOrCreateSubset(C_CONTROLLER), editors);
    model.putHint(CONTROLLER, controller);
    if (!myScreenSet.attach(life, controller, config.getOrCreateSubset(C_SCREENS))) return null;
    return controller.getComponent();
  }

  @Nullable
  public JComponent getDefaultFocusOwner(EditItemModel model) {
    ScreenController controller = model.getValue(CONTROLLER);
    if (controller == null) return null;
    DefaultEditModel.Child nested = getNestedModel(model);
    LoadedFieldInfo fieldInfo = LoadedFieldInfo.getInstance(nested);
    if (nested != null && fieldInfo != null) {
      DataVerification context = new DataVerification(nested, DataVerification.Purpose.ANY_ERROR);
      fieldInfo.checkMandatory(context, getEnabledEditors(model));
      List<DataVerification.Problem> errors = context.getErrors();
      if (!errors.isEmpty()) {
        JComponent focusable = controller.findFocusable(errors.get(0).getEditor());
        if (focusable != null) return focusable;
      }
    }
    return controller.getDefaultFocusOwner();
  }

  private static class Prepared {
    private static final TypedKey<Prepared> KEY = TypedKey.create("preparedIssueEditor");

    private final FieldsFilter myFilter;
    private final List<FieldEditor> myEditors;
    private final LoadedFieldInfo myFieldInfo;

    private Prepared(FieldsFilter filter, List<FieldEditor> editors, LoadedFieldInfo fieldInfo) {
      myFilter = filter;
      myEditors = editors;
      myFieldInfo = fieldInfo;
    }

    public static void putToModel(@NotNull EditItemModel model, @NotNull List<FieldEditor> editors, FieldsFilter filter, @NotNull LoadedFieldInfo fieldInfo) {
      Prepared prepared = new Prepared(filter, editors, fieldInfo);
      model.putHint(KEY, prepared);
    }

    @Nullable
    public static Prepared getInstance(EditItemModel model) {
      return model != null ? model.getValue(KEY) : null;
    }

    @NotNull
    public List<FieldEditor> getEditors() {
      return myEditors;
    }

    @NotNull
    public LoadedFieldInfo getFieldInfo() {
      return myFieldInfo;
    }

    public FieldsFilter getFilter() {
      return myFilter;
    }
  }

  private static class MyDescriptor extends EditDescriptor.Wrapper {
    private final Impl myDescriptor;

    public MyDescriptor(Impl descriptor) {
      myDescriptor = descriptor;
    }

    @NotNull
    @Override
    protected EditDescriptor getDelegate() {
      return myDescriptor;
    }

    @Override
    public JComponent createToolbarPanel(DefaultEditModel.Root model) {
      JPanel left = myDescriptor.createLeftToolbar();
      JPanel right = createRightActions();
      JComponent selector = createSelector(model);
      return layoutToolbar(left, selector, right);
    }

    private JComponent layoutToolbar(JPanel left, JComponent selector, JPanel right) {
      JPanel whole = new JPanel(new BorderLayout(5, 0));
      whole.setOpaque(false);
      whole.add(left, BorderLayout.WEST);
      whole.add(right, BorderLayout.EAST);

      Box box = new Box(BoxLayout.X_AXIS);
      box.setOpaque(false);
      if (selector != null) {
        box.add(Box.createHorizontalStrut(20));
        box.add(Box.createGlue());
        JPanel selectorPanel = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, 0, false));
        selectorPanel.setOpaque(false);
        selectorPanel.add(selector);
        box.add(selectorPanel);
        box.setMinimumSize(new Dimension(120, 10));
      }
      whole.add(box, BorderLayout.CENTER);
      return whole;
    }

    private JComponent createSelector(DefaultEditModel.Root model) {
      ScreenIssueEditor issueEditor = model.getValue(EDITOR);
      ScreenSet screens = issueEditor != null ? issueEditor.myScreenSet : null;
      return screens != null ? screens.getScreenSelector(model) : null;
    }

    private JPanel createRightActions() {
      ToolbarBuilder toolbar = new ToolbarBuilder();
      toolbar.addAction(FilterScreenAction.INSTANCE);
      toolbar.addAction(ConfigureVisibleFieldsAction.INSTANCE);
      toolbar.setCommonPresentation(PresentationMapping.NONAME);
      JPanel panel = toolbar.createHorizontalPanel();
      panel.setOpaque(false);
      return panel;
    }
  }
}
