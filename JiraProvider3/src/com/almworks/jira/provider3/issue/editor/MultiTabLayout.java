package com.almworks.jira.provider3.issue.editor;

import com.almworks.items.gui.edit.ComponentControl;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.items.gui.edit.util.VerticalLinePlacement;
import com.almworks.jira.provider3.gui.edit.fields.FieldInfoSet;
import com.almworks.util.Pair;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.DocumentFormAugmentor;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class MultiTabLayout {
  private final List<String> myTopFields = Collections15.arrayList();
  private final List<Pair<String, List<String>>> myTabs = Collections15.arrayList();
  private final List<String> myBottomFields = Collections15.arrayList();
  /** These fields are excluded from tabs */
  private final Set<String> myExcludeFields = Collections15.hashSet();
  private final Set<String> myVisibleFields = Collections15.hashSet();

  public void addTab(String tabName, List<String> fieldIds) {
    if (fieldIds != null && !fieldIds.isEmpty()) myTabs.add(Pair.create(tabName, fieldIds));
  }

  public void addTop(String jiraId) {
    addFixedField(jiraId, myTopFields);
  }

  public void addBottom(String jiraId) {
    addFixedField(jiraId, myBottomFields);
  }

  private void addFixedField(String jiraId, List<String> fieldSet) {
    if (myExcludeFields.contains(jiraId)) return;
    fieldSet.add(jiraId);
    myExcludeFields.add(jiraId);
  }

  public Pair<JComponent, List<FieldEditor>> createComponent(ControlledEditors editors, FieldInfoSet fieldInfo) {
    JComponent component = priCreateComponents(editors, fieldInfo);
    List<FieldEditor> visibleEditors = editors.selectEditors(myVisibleFields);
    return Pair.create(component, visibleEditors);
  }

  private static final ComponentProperty<JTabbedPane> TABBED_PANE = ComponentProperty.createProperty("tabbedPane");
  private JComponent priCreateComponents(ControlledEditors editors, FieldInfoSet fieldInfo) {
    List<Tab> tabs = collectTabs(editors);
    if (tabs.size() <= 1) return singleTab(editors, tabs.isEmpty() ? new Tab("") : tabs.get(0), fieldInfo);
    JComponent top = createFixedPanel(editors, myTopFields, fieldInfo);
    JComponent bottom = createFixedPanel(editors, myBottomFields, fieldInfo);
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(DocumentFormAugmentor.backgroundColor());
    if (top != null) panel.add(top, BorderLayout.NORTH);
    if (bottom != null) panel.add(bottom, BorderLayout.SOUTH);
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.setOpaque(false);
    panel.add(tabbedPane, BorderLayout.CENTER);
    TABBED_PANE.putClientValue(panel, tabbedPane);
    for (Tab tab : tabs) tabbedPane.addTab(tab.getName(), tab.buildComponent(editors.lifespan(), editors.getModel(), fieldInfo));
    return panel;
  }

  public static Component getActiveTab(JComponent editor) {
    JTabbedPane tabbedPane = TABBED_PANE.getClientValue(editor);
    if (tabbedPane == null) return editor;
    return tabbedPane.getSelectedComponent();
  }

  /**
   * Creates panel for fields with fixed placement (top or bottom).
   */
  @Nullable
  private JComponent createFixedPanel(ControlledEditors editors, List<String> fieldIds, FieldInfoSet fieldInfo) {
    Tab tab = new Tab("");
    tab.add(0, editors, fieldIds, myVisibleFields, null);
    return tab.buildComponent(editors.lifespan(), editors.getModel(), fieldInfo);
  }

  private List<Tab> collectTabs(ControlledEditors editors) {
    ArrayList<Tab> result = Collections15.arrayList();
    for (Pair<String, List<String>> tab : myTabs) {
      Tab newTab = new Tab(tab.getFirst());
      newTab.add(0, editors, tab.getSecond(), myVisibleFields, myExcludeFields);
      if (newTab.size() > 0) result.add(newTab);
    }
    return result;
  }

  private JComponent singleTab(ControlledEditors editors, Tab tab, FieldInfoSet fieldInfo) {
    tab = tab.copy();
    tab.add(0, editors, myTopFields, myVisibleFields, null);
    tab.add(tab.size(), editors, myBottomFields, myVisibleFields, null);
    return tab.buildComponent(editors.lifespan(), editors.getModel(), fieldInfo);
  }

  private static class Tab {
    private final String myName;
    private final List<Pair<String, List<? extends ComponentControl>>> myComponents;

    private Tab(String name) {
      myName = name;
      myComponents = Collections15.arrayList();
    }

    public Tab copy() {
      Tab copy = new Tab(myName);
      copy.myComponents.addAll(myComponents);
      return copy;
    }

    public String getName() {
      return myName;
    }

    public int size() {
      return myComponents.size();
    }

    /**
     * @param index insertion index
     * @param visibleFields collect visible field IDs here
     * @param exclude if not null do not add fields with specified IDs
     */
    public void add(int index, ControlledEditors editors, List<String> fieldIds, Collection<String> visibleFields, @Nullable Collection<String> exclude) {
      for (String fieldId : fieldIds) {
        if (exclude != null && exclude.contains(fieldId)) continue;
        List<? extends ComponentControl> components = editors.getComponents(fieldId);
        if (components != null && !components.isEmpty()) {
          myComponents.add(index, Pair.<String, List<? extends ComponentControl>>create(fieldId, components));
          index++;
          visibleFields.add(fieldId);
        }
      }
    }

    public JComponent buildComponent(Lifespan lifespan, EditItemModel model, FieldInfoSet fieldInfo) {
      VerticalLinePlacement builder = new VerticalLinePlacement(model.getEditingItems().size() > 1);
      for (Pair<String, List<? extends ComponentControl>> pair : myComponents) {
        List<? extends ComponentControl> components = pair.getSecond();
        if (components.isEmpty()) continue;
        String fieldId = pair.getFirst();
//        Pair<NameMnemonic, Boolean> info = fieldInfo.getInfo(fieldId);
//        boolean mandatory = info != null && info.getSecond();
//        NameMnemonic label = info != null ? info.getFirst() : components.get(0).getLabel();
        boolean mandatory = fieldInfo.isSurelyMandatory(fieldId);
        NameMnemonic label = components.get(0).getLabel();
        for (ComponentControl component : components) {
          if (label == null) label = component.getLabel();
          builder.addComponent(component, label, mandatory);
          label = null;
          mandatory = false;
        }
      }
      return builder.finishPanel(lifespan);
    }
  }
}
