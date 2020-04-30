package com.almworks.jira.provider3.issue.editor;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.DataVerification;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.jira.provider3.gui.edit.fields.FieldInfoSet;
import com.almworks.jira.provider3.gui.edit.fields.LoadedFieldInfo;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.util.Pair;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.FlatCollectionComponent;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.*;

public class ScreenController {
  public static final DataRole<ScreenController> ROLE = DataRole.createRole(ScreenController.class);

  private static final String S_FILTER_ON = "hideFields";

  private final FieldsFilter myFilter;
  private final PlaceHolder myComponent = new PlaceHolder();
  private final ControlledEditors myEditors;
  private final Lifecycle myScreenLife = new Lifecycle();
  @Nullable
  private final LoadedFieldInfo myFieldInfo;
  private List<FieldEditor> myVisibleEditors = Collections.emptyList();
  private IssueScreen myCurrentScreen = null;
  private Object myScreenState = null;
  @NotNull
  private List<ServerFields.Field> myTopFields = Collections.emptyList();
  @NotNull
  private List<ServerFields.Field> myBottomFields = Collections.emptyList();

  private ScreenController(FieldsFilter filter, @Nullable LoadedFieldInfo fieldInfo, ControlledEditors editors) {
    myFilter = filter;
    myFieldInfo = fieldInfo;
    myEditors = editors;
  }

  static ScreenController install(final EditItemModel model, final FieldsFilter filter, @Nullable LoadedFieldInfo fieldInfo, final Configuration config, ControlledEditors editors) {
    final ScreenController controller = new ScreenController(filter, fieldInfo, editors);
    Lifespan life = editors.lifespan();
    life.add(controller.myScreenLife.getDisposeDetach());
    ConstProvider.addGlobalValue(controller.myComponent, ROLE, controller);
    model.addAWTChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        controller.onModelChanged();
      }
    });
    filter.setOn(config.getBooleanSetting(S_FILTER_ON, false));
    filter.getModifiable().addChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        controller.rebuildCurrentScreen();
        config.setSetting(S_FILTER_ON, filter.isOn());
      }
    });
    return controller;
  }

  public void setTopFields(@NotNull List<ServerFields.Field> topFields) {
    myTopFields = Util.NN(topFields, Collections.<ServerFields.Field>emptyList());
  }

  public void setBottomFields(@NotNull List<ServerFields.Field> bottomFields) {
    myBottomFields = Util.NN(bottomFields, Collections.<ServerFields.Field>emptyList());
  }

  public void setScreen(IssueScreen screen) {
    if (myCurrentScreen == screen || screen == null) return;
    myCurrentScreen = screen;
    rebuildCurrentScreen();
  }

  public FieldsFilter getFilter() {
    return myFilter;
  }

  void onModelChanged() {
    if (myCurrentScreen == null || myScreenState == null) return;
    Object newState = myCurrentScreen.checkModelState(myEditors.getModel(), myScreenState);
    if (newState != null) rebuildCurrentScreen();
  }

  public EditItemModel getModel() {
    return myEditors.getModel();
  }

  public void rebuildCurrentScreen() {
    myScreenLife.cycle();
    if (myCurrentScreen == null) {
      myComponent.show((UIComponentWrapper)null);
      myCurrentScreen = null;
      myScreenState = null;
      return;
    }
    EditorComponentState savedState = EditorComponentState.create(myEditors, myComponent);
    MultiTabLayout layout = createScreenLayout();
    Long project = myEditors.getModel().getSingleEnumValue(Issue.PROJECT);
    Long type = myEditors.getModel().getSingleEnumValue(Issue.ISSUE_TYPE);
    FieldInfoSet fieldInfo = myFieldInfo != null ? myFieldInfo.getAllFields(project, type) : FieldInfoSet.EMPTY;
    Pair<JComponent, List<FieldEditor>> pair = layout.createComponent(myEditors, fieldInfo);
    myVisibleEditors = pair.getSecond();
    myComponent.show(pair.getFirst());
    if (savedState != null) savedState.restore(myComponent);
    myScreenState = myCurrentScreen.checkModelState(myEditors.getModel(), null);
  }

  private MultiTabLayout createScreenLayout() {
    MultiTabLayout layout = new MultiTabLayout();
    for (ServerFields.Field field : myTopFields) layout.addTop(field.getJiraId());
    for (ServerFields.Field field : myBottomFields) layout.addBottom(field.getJiraId());
    for (IssueScreen.Tab tab : myFilter.filter(getCurrentTabs())) {
      ArrayList<String> fieldIds = Collections15.arrayList();
      for (String fieldId : tab.getFieldIds()) {
        if (myEditors.getEditor(fieldId) != null) fieldIds.add(fieldId);
      }
      layout.addTab(tab.getName(), fieldIds);
    }
    return layout;
  }

  public List<IssueScreen.Tab> getCurrentTabs() {
    return myCurrentScreen.getTabs(myEditors.getModel());
  }

  /**
   * @return editors visible on selected (built) screen
   */
  List<FieldEditor> getVisibleEditors() {
    return myVisibleEditors;
  }

  JComponent getComponent() {
    return myComponent;
  }

  @Nullable
  public JComponent getDefaultFocusOwner() {
    for (FieldEditor editor : myVisibleEditors) {
      DataVerification context = new DataVerification(getModel(), DataVerification.Purpose.ANY_ERROR);
      editor.verifyData(context);
      if (context.getErrors().isEmpty()) continue;
      JComponent focusable = findFocusable(editor);
      if (focusable != null) return focusable;
    }
    UIComponentWrapper shown = myComponent.getShown();
    if (shown == null) return null;
    JComponent container = Util.castNullable(JComponent.class, MultiTabLayout.getActiveTab(shown.getComponent()));
    if (container == null) return null;
    JComponent focusable = findFocusable(Collections.singleton(container));
    return focusable != null ? focusable : container;
  }

  @Nullable
  public JComponent findFocusable(FieldEditor editor) {
    List<? extends ComponentControl> components = myEditors.getComponents(editor);
    if (components == null) return null;
    return findFocusable(ComponentControl.GET_COMPONENT.collectList(components));
  }

  @Nullable
  private static JComponent findFocusable(Collection<? extends Component> components) {
    for (Component c : components) {
      JComponent parent = Util.castNullable(JComponent.class, c);
      if (toFocusable(parent) != null) return parent;
      for (Component cc : SwingTreeUtil.descendants(parent)) {
        JComponent focusable = toFocusable(cc);
        if (focusable != null) return focusable;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static final List<Class<?>> FOCUSABLE_CLASSES = Arrays.asList(JTextComponent.class, FlatCollectionComponent.class, AComboBox.class);
  private static JComponent toFocusable(Component c) {
    JComponent component = Util.castNullable(JComponent.class, c);
    if (component == null || !component.isFocusable() || !component.isEnabled() || !component.isVisible()) return null;
    for (Class<?> aClass : FOCUSABLE_CLASSES) {
      if (Util.castNullable(aClass, component) != null) return component;
    }
    return null;
  }

  private static class EditorComponentState {
    private final ComponentControl myControl;

    public EditorComponentState(ComponentControl control) {
      myControl = control;
    }

    public static EditorComponentState create(ControlledEditors editors, JComponent ancestor) {
      Window window = SwingTreeUtil.findAncestorOfType(ancestor, Window.class);
      if (window == null) return null;
      Component focusOwner = window.getFocusOwner();
      if (!SwingTreeUtil.isAncestor(ancestor, focusOwner)) return null;
      ComponentControl control = editors.getEditorComponent(focusOwner);
      if (control == null) return null;
      return new EditorComponentState(control);
    }

    public void restore(JComponent ancestor) {
      JComponent component = myControl.getComponent();
      if (SwingTreeUtil.isAncestor(ancestor, component)) component.requestFocusInWindow();
    }
  }
}
