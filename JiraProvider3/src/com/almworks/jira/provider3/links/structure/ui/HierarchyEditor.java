package com.almworks.jira.provider3.links.structure.ui;

import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.explorer.TableController;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.AList;
import com.almworks.util.components.AToolbarButton;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.i18n.text.util.NamePattern;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.DialogEditor;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

class HierarchyEditor implements DialogEditor {
  private static final DataRole<HierarchyEditor> EDITOR = DataRole.createRole(HierarchyEditor.class);
  private static final DataRole<AList<HierarchyEditState>> HIERARCHIES = (DataRole)DataRole.createRole(AList.class);
  private static final DataRole<OrderListModel<HierarchyEditState>> MODEL = (DataRole)DataRole.createRole(OrderListModel.class);
  private static final DataRole<HierarchyEditState> SELECTION = DataRole.createRole(HierarchyEditState.class);

  private JPanel myWholePanel;
  private JTextField myCurrentName;
  private AList<HierarchyEditState> myHierarchies;
  private AToolbarButton myPlus;
  private AToolbarButton myMinus;
  private JScrollPane myCurrentScrollPane;
  private AToolbarButton myDuplicate;

  private final OrderListModel<HierarchyEditState> myModel = OrderListModel.create();
  private final Lifecycle mySelectionCycle = new Lifecycle();
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final StructuresController myStructures;
  private final JiraComplexHierarchies myComplexHierarchies;
  private final String mySelectionId;
  private final List<HierarchyEditState> myInitialStates = Collections15.arrayList();

  private HierarchyEditor(GuiFeaturesManager features, JiraComplexHierarchies complexHierarchies, String selectionId) {
    myComplexHierarchies = complexHierarchies;
    mySelectionId = selectionId;
    myInitialStates.addAll(complexHierarchies.getEditStates());
    Collections.sort(myInitialStates, HierarchyEditState.ORDER_BY_NAME);
    myStructures = new StructuresController(features, complexHierarchies.getConnection());
    myHierarchies.getSelectionAccessor().addAWTChangeListener(new ChangeListener() {
      private HierarchyEditState myLastSelection = null;

      @Override
      public void onChange() {
        List<HierarchyEditState> selection = myHierarchies.getSelectionAccessor().getSelectedItems();
        HierarchyEditState hierarchy = selection.size() == 1 ? selection.get(0) : null;
        if (myLastSelection != hierarchy) {
          myLastSelection = hierarchy;
          setSelection(hierarchy);
        }
      }
    });
    myHierarchies.setCanvasRenderer(HierarchyEditState.RENDERER);
    myHierarchies.setCollectionModel(myModel);
    myHierarchies.setDataRoles(SELECTION);
    myHierarchies.addGlobalRoles(SELECTION);
    myModel.setElements(myInitialStates);
    myModel.addAWTChangeListener(myModifiable);
    UIUtil.keepSelectionOnRemove(myHierarchies);
    ConstProvider.addRoleValue(myWholePanel, HIERARCHIES, myHierarchies);
    ConstProvider.addRoleValue(myWholePanel, MODEL, myModel);
    ConstProvider.addRoleValue(myWholePanel, EDITOR, this);
    myPlus.overridePresentation(PresentationMapping.VISIBLE_NONAME);
    myPlus.setAnAction(new AddAction());
    myMinus.overridePresentation(PresentationMapping.VISIBLE_NONAME);
    myMinus.setAnAction(new RemoveAction());
    myDuplicate.overridePresentation(PresentationMapping.VISIBLE_NONAME);
    myDuplicate.setAnAction(new DuplicateAction());
  }

  private void setSelection(final HierarchyEditState hierarchy) {
    myStructures.applyCurrent();
    mySelectionCycle.cycle();
    if (hierarchy == null) {
      myCurrentName.setEnabled(false);
      myCurrentName.setText("");
      myCurrentScrollPane.setViewportView(null);
      myStructures.setHierarchy(Lifespan.NEVER, myModifiable, null);
      return;
    }
    myCurrentName.setEnabled(true);
    myCurrentName.setText(hierarchy.getDisplayName());
    myStructures.setHierarchy(mySelectionCycle.lifespan(), myModifiable, hierarchy);
    myCurrentScrollPane.setViewportView(myStructures.getScrollable());
    UIUtil.addTextListener(mySelectionCycle.lifespan(), myCurrentName, new ChangeListener() {
      @Override
      public void onChange() {
        hierarchy.setDisplayName(myCurrentName.getText());
        myModel.forceUpdateAt(myModel.indexOf(hierarchy));
        myModifiable.fireChanged();
      }
    });
  }

  public static void showWindow(DialogManager manager, JiraComplexHierarchies hierarchies, GuiFeaturesManager features, ItemsTreeLayout layout, TableController table) {
    DialogEditorBuilder builder = manager.createEditor("customHierarchies.editor");
    builder.setTitle(JiraLinks.I18N.getFactory("hierarchy.editor.title").create());
    String initialSelection = CustomHierarchy.getCustomHierarchyId(layout != null ? layout.getId() : null);
    final HierarchyEditor editor = new HierarchyEditor(features, hierarchies, initialSelection);
    builder.setContent(editor);
    builder.hideApplyButton();
    builder.addGlobalDataRoot();
    builder.setModal(true);
    StateListener stateListener = new StateListener(editor);
    builder.addStateListener(stateListener);
    builder.showWindow();
    stateListener.updateSelection(table);
  }

  @Override
  public boolean isModified() {
    myStructures.applyCurrent();
    for (HierarchyEditState state : myInitialStates) if (!myModel.contains(state)) return true;
    for (HierarchyEditState hierarchy : myModel) if (hierarchy.isModified()) return true;
    return false;
  }

  @Override
  public void apply() throws CantPerformExceptionExplained {
    List<HierarchyEditState> updated = myComplexHierarchies.setNewHierarchies(myModel.toList());
    myInitialStates.clear();
    myInitialStates.addAll(updated);
    Collections.sort(myInitialStates, HierarchyEditState.ORDER_BY_NAME);
    reset();
  }

  @Override
  public void reset() {
    SelectionAccessor<HierarchyEditState> selectionAccessor = myHierarchies.getSelectionAccessor();
    int[] selection = selectionAccessor.getSelectedIndexes();
    for (HierarchyEditState state : myInitialStates) state.reset();
    myModel.setElements(myInitialStates);
    if (selection.length == 0) {
      HierarchyEditState selected = null;
      if (mySelectionId != null) {
        for (HierarchyEditState state : myModel) {
          if (mySelectionId.equals(state.getId())) {
            selected = state;
            break;
          }
        }
      }
      if (selected != null) selectionAccessor.setSelected(selected);
      else selectionAccessor.ensureSelectionExists();
    } else selectionAccessor.setSelectedIndexes(selection);
    myStructures.reset();
  }

  @Override
  public Modifiable getModifiable() {
    return myModifiable;
  }

  @Override
  public JComponent getComponent() {
    return myWholePanel;
  }

  @Override
  public void dispose() {
    mySelectionCycle.dispose();
  }

  private static void createNewHierarchy(ActionContext context, HierarchyEditState hierarchy) throws CantPerformException {
    context.getSourceObject(MODEL).addElement(hierarchy);
    context.getSourceObject(HIERARCHIES).getSelectionAccessor().setSelected(hierarchy);
    JTextField nameField = context.getSourceObject(EDITOR).myCurrentName;
    nameField.selectAll();
    nameField.requestFocusInWindow();
  }

  private static class AddAction extends SimpleAction {
    private static final LocalizedAccessor.MessageInt M_NEW_HIERARCHY = JiraLinks.I18N.messageInt("hierarchy.editor.newHierarchy.name");

    private AddAction() {
      super(JiraLinks.I18N.getFactory("hierarchy.editor.action.add.name"), Icons.ACTION_GENERIC_ADD);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      OrderListModel<HierarchyEditState> model = context.getSourceObject(MODEL);
      String dupName = NamePattern.nextName(HierarchyEditState.GET_NAME.collectSet(model.toList()), M_NEW_HIERARCHY);
      createNewHierarchy(context, new HierarchyEditState(dupName, Collections.<String>emptyList(), null));
    }
  }

  private static class RemoveAction extends SimpleAction {
    private RemoveAction() {
      super(JiraLinks.I18N.getFactory("hierarchy.editor.action.remove.name"), Icons.ACTION_GENERIC_REMOVE);
      watchRole(SELECTION);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      CantPerformException.ensureNotEmpty(context.getSourceCollection(SELECTION));
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      context.getSourceObject(MODEL).removeAll(context.getSourceCollection(SELECTION));
    }
  }

  private static class DuplicateAction extends SimpleAction {
    public static final LocalizedAccessor.MessageIntStr M_DUPLICATE_NAME = JiraLinks.I18N.messageIntStr("hierarchy.editor.action.duplicate.duplicateName.name");
    public static final LocalizedAccessor.Value M_DUPLICATE_APPLIED = JiraLinks.I18N.getFactory("hierarchy.editor.action.duplicate.duplicateName.applied");
    private static final NamePattern DUP_NAME = new NamePattern(M_DUPLICATE_NAME, M_DUPLICATE_APPLIED);

    private DuplicateAction() {
      super(JiraLinks.I18N.getFactory("hierarchy.editor.action.duplicate.name"), Icons.ACTION_GENERIC_COPY);
      watchRole(SELECTION);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(SELECTION);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      HierarchyEditState source = context.getSourceObject(SELECTION);
      OrderListModel<HierarchyEditState> model = context.getSourceObject(MODEL);
      String dupName = DUP_NAME.generateName(HierarchyEditState.GET_NAME.collectSet(model.toList()), source.getDisplayName());
      createNewHierarchy(context, source.duplicate(dupName));
    }
  }

  private static class StateListener implements Procedure<DialogEditorBuilder.EditingEvent> {
    private final HierarchyEditor myEditor;
    private String mySelectedId;

    public StateListener(HierarchyEditor editor) {
      myEditor = editor;
      mySelectedId = null;
    }

    @Override
    public void invoke(DialogEditorBuilder.EditingEvent arg) {
      if (arg != DialogEditorBuilder.OK_EVENT) return;
      List<HierarchyEditState> selection = myEditor.myHierarchies.getSelectionAccessor().getSelectedItems();
      if (selection.size() == 1) {
        HierarchyEditState state = selection.get(0);
        if (!state.isEmpty()) mySelectedId = state.getId();
      }
    }

    public void updateSelection(TableController table) {
      if (table == null || mySelectedId == null) return;
      table.setTreeLayoutById(CustomHierarchy.getLayoutId(mySelectedId));
    }
  }
}
