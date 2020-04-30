package com.almworks.sumtable;

import com.almworks.api.application.qb.CannotSuggestNameException;
import com.almworks.api.application.qb.FilterEditor;
import com.almworks.api.application.qb.FilterEditorProvider;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.application.tree.UserQueryNode;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.events.DetachableValue;
import com.almworks.util.exec.Context;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.ConfiguredSplitPane;
import com.almworks.util.ui.ScrollBarPolicy;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CountersConfigurationPanel {
  public static final DataRole<STFilter> COUNTER_ROLE = DataRole.createRole(STFilter.class);
  private static final String SINGLE_SELECTION = "1";
  private static final String ZERO_SELECTION = "0";
  private static final String MULTIPLE_SELECTION = "*";

  private JPanel myCounterPropsPanel;
  private JTextField myCounterName;
  private JTextArea myCounterFormula;
  private JButton myEditFilterButton;
  private JCheckBox myTextCheckBox;
  private JTextField myTextSpecifiedEditor;
  private JRadioButton myIconAutoColor;
  private JRadioButton myIconSelectedColor;
  private JPanel myColorSelectionPlace;
  private JCheckBox myIconCheckBox;
  private JLabel myCounterNameLabel;
  private JLabel myCounterFilterLabel;
  private JRadioButton myIconByFilter;
  private JRadioButton myTextAuto;
  private JPanel myIconConfigPanel;
  private JRadioButton myTextSpecified;
  private JPanel myTextConfigPanel;

  private final JPanel myWholePanel = new JPanel(new BorderLayout());

  private final JPanel myPropsPlace = new JPanel(new CardLayout());

  private final AList<STFilter> myCounterList = new AList<STFilter>();

  private final CounterConfiguration myCounterConfiguration;

  private final Lifecycle mySelectedCounterLife = new Lifecycle();

  private final DetachableValue<QueryResult> myQueryResult = DetachableValue.create();

  private STFilter myLastSelection;
  private static final Map<TypedKey<?>, ?> NAME_SUGGESTION_HINTS;
  static {
    HashMap<TypedKey<?>, ?> map = Collections15.hashMap();
    UserQueryNode.SINGLE_ENUM_PLEASE.putTo(map, Boolean.TRUE);
    UserQueryNode.MAX_NAME_LENGTH.putTo(map, 30);
    NAME_SUGGESTION_HINTS = Collections.unmodifiableMap(map);
  }

  public CountersConfigurationPanel(Configuration uiConfig, CounterConfiguration counterConfig) {
    myCounterConfiguration = counterConfig;

    final JScrollPane countersScrollPane = new JScrollPane(myCounterList);
    ScrollBarPolicy.setPolicies(countersScrollPane, ScrollBarPolicy.NEVER, ScrollBarPolicy.AS_NEEDED);

    final JSplitPane myContentPanel = ConfiguredSplitPane.createLeftRight(
      countersScrollPane, new ScrollPaneBorder(myPropsPlace), uiConfig, 0.35f);

    myPropsPlace.add(myCounterPropsPanel, SINGLE_SELECTION);
    myPropsPlace.add(createMessage("No counters selected"), ZERO_SELECTION);
    myPropsPlace.add(createMessage("Multiple counters selected"), MULTIPLE_SELECTION);
    myPropsPlace.setBorder(UIUtil.BORDER_5);

    myWholePanel.add(createToolbar(), BorderLayout.NORTH);
    myWholePanel.add(myContentPanel, BorderLayout.CENTER);

    myCounterList.setCanvasRenderer(Renderers.defaultCanvasRenderer());
    myCounterList.setDataRoles(COUNTER_ROLE);
    myCounterList.setPrototypeCellValue(new STFilter(BoolExpr.<DP>TRUE(), "this is a prototype", null, "proto"));
    myCounterList.setVisibleRowCount(4);

    myCounterFilterLabel.setLabelFor(myCounterFormula);
    myCounterNameLabel.setLabelFor(myCounterName);

    UIUtil.setupConditionalEnabled(myIconCheckBox, false, myIconConfigPanel);
    UIUtil.setupConditionalEnabled(myIconSelectedColor, false, myColorSelectionPlace);
    UIUtil.setupConditionalEnabled(myTextCheckBox, false, myTextConfigPanel);
    UIUtil.setupConditionalEnabled(myTextSpecified, false, myTextSpecifiedEditor);

    UIUtil.createButtonGroup(myIconAutoColor, myIconSelectedColor, myIconByFilter);
    UIUtil.createButtonGroup(myTextAuto, myTextSpecified);

    EditFilterListener editListener = new EditFilterListener();
    myEditFilterButton.addActionListener(editListener);
    myCounterList.addDoubleClickListener(Lifespan.FOREVER, editListener);
  }

  private JLabel createMessage(String message) {
    JLabel label = new JLabel(message);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalAlignment(SwingConstants.CENTER);
    return label;
  }

  private Component createToolbar() {
    ToolbarBuilder builder = ToolbarBuilder.smallVisibleButtons();
    builder.setContextComponent(myCounterList);
    builder.addAction(new EnabledAction("Add Counter", Icons.ACTION_GENERIC_ADD) {
      protected void doPerform(ActionContext context) throws CantPerformException {
        addCounter();
      }
    });
    builder.addAction(new RemoveCountersAction(myCounterConfiguration));
    builder.addSeparator();
    builder.addAction(new MoveCounterAction(myCounterConfiguration, -1, "Move Counter Up", Icons.ARROW_UP));
    builder.addAction(new MoveCounterAction(myCounterConfiguration, 1, "Move Counter Down", Icons.ARROW_DOWN));
    return builder.createHorizontalToolbar();
  }

  private void addCounter() {
    FilterNode newFilter = editFilterNode(FilterNode.ALL_ITEMS);
    if (newFilter == null)
      return;
    String name = suggestNameFromFilter(newFilter);
    STFilter counter = myLastSelection;
    int index = counter == null ? -1 : myCounterConfiguration.getCounterModel().indexOf(counter);
    if (name == null) {
      if (index < 0) {
        name = "New Counter";
      } else {
        name = "Counter #" + (index + 2);
      }
    }
    STFilter newCounter = myCounterConfiguration.addNewCounter(name, newFilter, index);
    myCounterList.getSelectionAccessor().setSelected(newCounter);
  }

  @Nullable
  private String suggestNameFromFilter(FilterNode filter) {
    try {
      return filter.getSuggestedName(NAME_SUGGESTION_HINTS);
    } catch (CannotSuggestNameException e) {
      return null;
    }
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void attach(final Lifespan lifespan, QueryResult queryResult) {
    myQueryResult.set(lifespan, queryResult);
    lifespan.add(mySelectedCounterLife.getAnyCycleDetach());
    lifespan.add(myCounterList.setCollectionModel(myCounterConfiguration.getCounterModel()));
    SelectionAccessor<STFilter> accessor = myCounterList.getSelectionAccessor();
    accessor.addAWTChangeListener(lifespan, new ChangeListener() {
      public void onChange() {
        if (!lifespan.isEnded()) {
          onCounterSelectionChanged();
        }
      }
    });
    accessor.ensureSelectionExists();
  }

  private void onCounterSelectionChanged() {
    SelectionAccessor<STFilter> accessor = myCounterList.getSelectionAccessor();
    int selectedCount = accessor.getSelectedCount();
    String card = selectedCount == 0 ? ZERO_SELECTION : (selectedCount == 1 ? SINGLE_SELECTION : MULTIPLE_SELECTION);
    ((CardLayout) myPropsPlace.getLayout()).show(myPropsPlace, card);

    STFilter selection = accessor.getSelection();
    if (selection != myLastSelection) {
      myLastSelection = selection;
      mySelectedCounterLife.cycle();
      if (selection != null) {
        attachCounterProperties(mySelectedCounterLife.lifespan(), selection);
      }
    }
  }

  private void attachCounterProperties(Lifespan lifespan, STFilter counter) {
    myCounterConfiguration.configureCounterName(lifespan, counter, myCounterName);
    myCounterConfiguration.configureCounterLabel(lifespan, counter, myIconCheckBox, myTextCheckBox, myIconAutoColor,
      myIconSelectedColor, myIconByFilter, myTextAuto, myTextSpecified, myTextSpecifiedEditor);
    displayFilterFormula(counter);
  }

  private void displayFilterFormula(STFilter counter) {
    String formula = counter.getFormula();
    myCounterFormula.setText(formula);
    if (formula.length() > 0) {
      try {
        Rectangle r = myCounterFormula.modelToView(0);
        if (r != null) {
          myCounterFormula.scrollRectToVisible(r);
        }
      } catch (BadLocationException e) {
        // ignore
      }
    }
  }

  private void editFilter() {
    if (myLastSelection == null)
      return;
    QueryResult queryResult = myQueryResult.get();
    if (queryResult == null)
      return;
    STFilter counter = myLastSelection;
    FilterNode filterNode = counter.getFilterNode();
    if (filterNode == null)
      filterNode = FilterNode.ALL_ITEMS;
    FilterNode newNode = editFilterNode(filterNode);
    if (newNode != null) {
      counter.setFilterNode(newNode, queryResult.getEncompassingHypercube());
      if (counter == myLastSelection) {
        displayFilterFormula(counter);
      }
      myCounterConfiguration.updatedCounter(counter);
    }
  }

  private FilterNode editFilterNode(FilterNode filterNode) {
    QueryResult queryResult = myQueryResult.get();
    if (queryResult == null)
      return filterNode;

    FilterEditorProvider provider = Context.require(FilterEditorProvider.ROLE);
    DialogManager dialogManager = Context.require(DialogManager.ROLE);
    DialogBuilder builder = dialogManager.createBuilder("sumtableEditFilter");
    ItemHypercube hypercube = queryResult.getEncompassingHypercube();
    final FilterEditor editor = provider.createFilterEditor(filterNode, hypercube, builder.getWindowContainer());
    builder.setContent(editor);
    builder.setTitle("Edit Filter");
    builder.setPreferredSize(new Dimension(680, 320));
    builder.setEmptyCancelAction();
    builder.setModal(true);
    final FilterNode[] newNode = new FilterNode[] {null};
    editor.reset();
    builder.setOkAction(new SimpleAction("OK") {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(editor.getModifiable());
        context.setEnabled(editor.getCurrentFilter() != null);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        editor.apply();
        newNode[0] = editor.getCurrentFilter();
      }
    });
    builder.showWindow();
    editor.dispose();
    return newNode[0];
  }


  private static class RemoveCountersAction extends SimpleAction {
    private final CounterConfiguration myCounterConfiguration;

    public RemoveCountersAction(CounterConfiguration counterConfiguration) {
      super("Remove Counter", Icons.ACTION_GENERIC_REMOVE);
      myCounterConfiguration = counterConfiguration;
      watchRole(CountersConfigurationPanel.COUNTER_ROLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      List<STFilter> counters = context.getSourceCollection(COUNTER_ROLE);
      context.setEnabled(counters.size() > 0);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      List<STFilter> counters = context.getSourceCollection(COUNTER_ROLE);
      myCounterConfiguration.remove(counters);
    }
  }


  private class EditFilterListener implements ActionListener, CollectionCommandListener<STFilter> {
    public void actionPerformed(ActionEvent e) {
      editFilter();
    }

    public void onCollectionCommand(ACollectionComponent<STFilter> aCollectionComponent, int index, STFilter element) {
      myCounterList.getSelectionAccessor().setSelectedIndex(index);
      editFilter();
    }
  }
}

