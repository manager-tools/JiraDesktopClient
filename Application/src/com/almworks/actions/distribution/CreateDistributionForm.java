package com.almworks.actions.distribution;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ItemKeyGroup;
import com.almworks.api.application.ItemKeyStub;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.application.qb.EnumGrouping;
import com.almworks.api.application.tree.DistributionFolderNode;
import com.almworks.api.application.tree.DistributionParameters;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryUtil;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.DialogResult;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.advmodel.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Function;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.ConfigAttach;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.sfs.StringFilterSet;
import com.almworks.util.sfs.StringFilterSetEditor;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.dnd.ConvertingToStringSourceTransfer;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class CreateDistributionForm implements Function<JPanel, Boolean> {
  private static final ConvertingToStringSourceTransfer<ItemKey> ITEM_KEY_TRANSFER =
    ConvertingToStringSourceTransfer.create(ItemKey.ITEM_KEY_ROLE, ItemKey.DISPLAY_NAME);

  private static final String GROUPS_CARD = "groups";
  private static final String NOGROUPS_CARD = "nogroups";
  private static final String NOTHING_CARD = "nothing";
  private static final String SELECTED_GROUP_BY_SETTING = "groupBy";
  private static final String ARRANGE_IN_GROUPS_SETTING = "arrange";
  private static final String VALUES_ALL = "None - all values";
  private static final String VALUES_INCLUSIVE = "Inclusive - only these values:";
  private static final String VALUES_EXCLUSIVE = "Exclusive - all values except these:";

  private static final String COLLAPSED_SUBSET = "collapsed";
  private static final String EXPANDED_SUBSET = "expanded";
  private static final String DIVIDER_LOCATION = "dividerLocation";

  private static final ItemKeyStub ITEM_KEY_PROTOTYPE =
    new ItemKeyStub("very very long prototype key name");
  private static final ItemKeyGroup ITEM_KEY_GROUP_PROTOTYPE =
    new ItemKeyGroup("very very long prototype group name");
  private static final ConvertingToStringSourceTransfer<ItemKeyGroup> ITEM_KEY_GROUP_TRANSFER =
    new ConvertingToStringSourceTransfer<ItemKeyGroup>(ItemKeyGroup.ITEM_KEY_GROUP_ROLE,
      ItemKeyGroup.TO_DISPLAY_STRING);

  private Window myWindow;
  private JRootPane myRootPane;
  private final JPanel myWholePanel;
  private final AList<EnumConstraintType> myTypes = new AList<EnumConstraintType>();

  private final boolean myEdit;
  private final ItemHypercube myContextCube;
  private final Configuration myConfiguration;
  private final Lifecycle mySelectionLifecycle = new Lifecycle();

  private EnumConstraintType myCurrentType = null;

  private final JPanel myParametersWithGroups = new JPanel();
  private final AComboBox<EnumGrouping> myGroupBy = new AComboBox<EnumGrouping>(15);
  private final JCheckBox myArrangeInGroups = new JCheckBox();
  private final JCheckBox myHideEmptyQueries = new JCheckBox();
  private final StringFilterSetEditor myValuesFilterEditor;
  private final StringFilterSetEditor myGroupsFilterEditor;
  private final AList<ItemKey> myValuesPreview = new AList<ItemKey>();
  private final AList<ItemKeyGroup> myGroupsPreview = new AList<ItemKeyGroup>();

  private final JPanel myParametersWithoutGroups = new JPanel();
  private final StringFilterSetEditor myValuesWGFilterEditor;
  private final AList<ItemKey> myValuesWGPreview = new AList<ItemKey>();

  private final JPanel myParameters = new JPanel(new CardLayout());
  private final Lifecycle myGroupByLifespan = new Lifecycle();

  private AListModel<ItemKeyGroup> myValueToGroupCachedImage;

  private String myShownCard;

  private ConstraintDescriptor myInitialDescriptor;
  private DistributionParameters myInitialParemeters;
  private StringFilterSet myInitialValuesFilterSet;
  private StringFilterSet myInitialGroupsFilterSet;

  private final JPanel myExpandablePanel;
  private final JScrollPane myAttributes;
  private final JSplitPane mySplitPane;
  private final JButton myExpandButton;
  private Boolean myCollapsed = null;

  private CreateDistributionForm(boolean edit, ItemHypercube contextCube,
    AListModel<EnumConstraintType> attributesModel, Configuration configuration,
    @Nullable ConstraintDescriptor editDescriptor, @Nullable DistributionParameters editParameters)
  {
    myContextCube = contextCube;
    myConfiguration = configuration;
    myEdit = edit;
    myInitialDescriptor = editDescriptor;
    myInitialParemeters = editParameters;
    if (editParameters != null) {
      myInitialValuesFilterSet = editParameters.getValuesFilter();
      myInitialGroupsFilterSet = editParameters.getGroupsFilter();
    }

    myValuesFilterEditor = new StringFilterSetEditor(this, 10);
    myValuesWGFilterEditor = new StringFilterSetEditor(this, 10);
    myGroupsFilterEditor = new StringFilterSetEditor(this, 5);

    initAttributes(attributesModel);
    initParametersWithGroups();
    initParametersWithoutGroups();
    initFilterKindNames();

    myExpandablePanel = new JPanel(new BorderLayout(0, 0));
    myExpandButton = new JButton();
    myExpandButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setCollapsed(!myCollapsed);
      }
    });

    myAttributes = new JScrollPane(myTypes.toComponent());
    myAttributes.setMinimumSize(new Dimension(200, 150));
    myAttributes.setPreferredSize(new Dimension(200, 250));

    myParameters.add(myParametersWithGroups, GROUPS_CARD);
    myParameters.add(myParametersWithoutGroups, NOGROUPS_CARD);
    myParameters.add(UIUtil.createMessage("Please select field for the new distribution"), NOTHING_CARD);

    mySplitPane = UIUtil.createSplitPane(myAttributes, myParameters, true, configuration, DIVIDER_LOCATION, -1);
    mySplitPane.setOneTouchExpandable(false);
    // set to value >0 and observe "jumping" on expansion
    mySplitPane.setResizeWeight(0.0);

    myWholePanel = new JPanel(UIUtil.createBorderLayout());
    myWholePanel.add(myExpandablePanel, BorderLayout.CENTER);

    NameMnemonic.parseString("&Hide empty sub-queries").setToButton(myHideEmptyQueries);
    myHideEmptyQueries.setSelected(myInitialParemeters != null ? myInitialParemeters.isHideEmptyQueries() : true);

    myWholePanel.add(myHideEmptyQueries, BorderLayout.SOUTH);

    myGroupBy.setCanvasRenderer(new CanvasRenderer<EnumGrouping>() {
      public void renderStateOn(CellState state, Canvas canvas, EnumGrouping item) {
        canvas.appendText(item == null ? "No grouping" : item.getDisplayableName());
      }
    });

    myGroupsPreview.setCanvasRenderer(new FilteredGroupRenderer(myGroupsFilterEditor));
    myValuesPreview.setCanvasRenderer(new FilteredKeyRenderer(myValuesFilterEditor, myGroupsFilterEditor));
    myValuesWGPreview.setCanvasRenderer(new FilteredKeyRenderer(myValuesWGFilterEditor, null));
    ListSpeedSearch.install(myGroupsPreview);
    ListSpeedSearch.install(myValuesPreview);
    ListSpeedSearch.install(myValuesWGPreview);

    myGroupsPreview.setPrototypeCellValue(ITEM_KEY_GROUP_PROTOTYPE);
    myValuesPreview.setPrototypeCellValue(ITEM_KEY_PROTOTYPE);
    myValuesWGPreview.setPrototypeCellValue(ITEM_KEY_PROTOTYPE);

    myValuesPreview.setTransfer(ITEM_KEY_TRANSFER);
    myValuesPreview.setDataRoles(ItemKey.ITEM_KEY_ROLE);
    myValuesWGPreview.setTransfer(ITEM_KEY_TRANSFER);
    myValuesWGPreview.setDataRoles(ItemKey.ITEM_KEY_ROLE);
    myGroupsPreview.setTransfer(ITEM_KEY_GROUP_TRANSFER);
    myGroupsPreview.setDataRoles(ItemKeyGroup.ITEM_KEY_GROUP_ROLE);

    // set collapsed state
    boolean filtering =
      StringFilterSet.isFiltering(myInitialValuesFilterSet) || StringFilterSet.isFiltering(myInitialGroupsFilterSet);
    boolean grouping = editParameters != null && (editParameters.isArrangeInGroups() || editParameters.getGroupingName() != null);
    boolean collapsed = !(myEdit && (filtering || grouping));
    setCollapsed(collapsed);

    myWholePanel.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        saveSizeToConfig();
      }
    });
  }

  public static CreateDistributionForm forCreate(ItemHypercube contextCube,
    AListModel<EnumConstraintType> attributesModel, Configuration configuration)
  {
    return new CreateDistributionForm(false, contextCube, attributesModel, configuration, null, null);
  }

  public static CreateDistributionForm forEdit(ItemHypercube contextCube,
    AListModel<EnumConstraintType> attributesModel, Configuration configuration, GenericNode node)
  {
    if (!(node instanceof DistributionFolderNode)) {
      assert false : node;
      return null;
    }
    DistributionFolderNode folder = ((DistributionFolderNode) node);
    return new CreateDistributionForm(true, contextCube, attributesModel, configuration, folder.getDescriptor(),
      folder.getParameters());
  }

  private void initFilterKindNames() {
    myGroupsFilterEditor.setKindPresentation(StringFilterSet.Kind.ALL, "None - values from all groups");
    myGroupsFilterEditor.setKindPresentation(StringFilterSet.Kind.INCLUSIVE,
      "Inclusive - only values from these groups:");
    myGroupsFilterEditor.setKindPresentation(StringFilterSet.Kind.EXCLUSIVE,
      "Exclusive - only values not in these groups:");

    myValuesFilterEditor.setKindPresentation(StringFilterSet.Kind.ALL, VALUES_ALL);
    myValuesFilterEditor.setKindPresentation(StringFilterSet.Kind.INCLUSIVE, VALUES_INCLUSIVE);
    myValuesFilterEditor.setKindPresentation(StringFilterSet.Kind.EXCLUSIVE, VALUES_EXCLUSIVE);

    myValuesWGFilterEditor.setKindPresentation(StringFilterSet.Kind.ALL, VALUES_ALL);
    myValuesWGFilterEditor.setKindPresentation(StringFilterSet.Kind.INCLUSIVE, VALUES_INCLUSIVE);
    myValuesWGFilterEditor.setKindPresentation(StringFilterSet.Kind.EXCLUSIVE, VALUES_EXCLUSIVE);
  }

  private void initParametersWithGroups() {
    FormLayout layout = new FormLayout("fill:d, 4dlu, fill:d:g(1), 26dlu, fill:d:g(4)",
      "d, 16dlu, d, 4dlu, fill:d:g(1), 16dlu, d, 4dlu, fill:d:g(4)");

    DefaultFormBuilder builder = new DefaultFormBuilder(layout, myParametersWithGroups);

    DefaultFormBuilder groupByPanel = new DefaultFormBuilder(new FormLayout("d, 4dlu, d"));
    groupByPanel.append(myGroupBy);
    groupByPanel.append(myArrangeInGroups);
    NameMnemonic.parseString("&Arrange sub-queries in groups").setToButton(myArrangeInGroups);

    builder.append("&Group by:", groupByPanel.getPanel(), 3).setLabelFor(myGroupBy);
    builder.nextLine(2);

    builder.append("G&roup filter:").setLabelFor(myGroupsFilterEditor.getFilterTypeCombobox());
    builder.add(myGroupsFilterEditor.getFilterTypeCombobox(),
      new CellConstraints(builder.getColumn(), builder.getRow(), CellConstraints.LEFT, CellConstraints.DEFAULT));
    builder.nextColumn(2);
    builder.append("Groups preview:");
    builder.nextLine(2);

    builder.nextColumn(2);
    builder.append(myGroupsFilterEditor.getFilterList());
    builder.append(new JScrollPane(myGroupsPreview, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
    builder.nextLine(2);

    builder.append("&Value filter:").setLabelFor(myValuesFilterEditor.getFilterTypeCombobox());
    builder.add(myValuesFilterEditor.getFilterTypeCombobox(),
      new CellConstraints(builder.getColumn(), builder.getRow(), CellConstraints.LEFT, CellConstraints.DEFAULT));

    builder.nextColumn(2);
    builder.append("Values preview:");
    builder.nextLine(2);

    builder.nextColumn(2);
    builder.append(myValuesFilterEditor.getFilterList());
    builder.append(new AScrollPane(myValuesPreview, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

    myParametersWithGroups.setBorder(UIUtil.BORDER_5);
  }

  private void initParametersWithoutGroups() {
    FormLayout layout = new FormLayout("fill:d, 4dlu, fill:d:g(1), 26dlu, fill:d:g(4)", "d, 4dlu, fill:d:g(1)");

    DefaultFormBuilder builder = new DefaultFormBuilder(layout, myParametersWithoutGroups);

    JComponent filterTypeCombo = myValuesWGFilterEditor.getFilterTypeCombobox();
    builder.append("&Value filter:").setLabelFor(filterTypeCombo);
    builder.add(filterTypeCombo,
      new CellConstraints(builder.getColumn(), builder.getRow(), CellConstraints.LEFT, CellConstraints.DEFAULT));
    builder.nextColumn(2);
    builder.append("Values preview:");
    builder.nextLine(2);

    builder.nextColumn(2);
    builder.append(myValuesWGFilterEditor.getFilterList());
    builder.append(new JScrollPane(myValuesWGPreview, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));

    myParametersWithoutGroups.setBorder(UIUtil.BORDER_5);
  }

  private void initAttributes(AListModel<EnumConstraintType> typesModel) {
    myTypes.setCollectionModel(typesModel);
    myTypes.setCanvasRenderer(new CanvasRenderer<EnumConstraintType>() {
      public void renderStateOn(CellState state, Canvas canvas, EnumConstraintType item) {
        item.getDescriptor().getPresentation().renderOn(canvas, state);
      }
    });
    ListSpeedSearch.install(myTypes);
    myTypes.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myTypes.getSelectionAccessor().ensureSelectionExists();
  }

  public void connectValues(Lifespan life, final Runnable okAction) {
    life.add(myTypes.getSelectionAccessor().addListener(new SelectionAccessor.Listener<Object>() {
      public void onSelectionChanged(Object attribute) {
        updateEditor();
      }
    }));
    myTypes.addDoubleClickListener(life, new CollectionCommandListener<EnumConstraintType>() {
      public void onCollectionCommand(ACollectionComponent<EnumConstraintType> coll, int index, EnumConstraintType element) {
        okAction.run();
      }
    });
    updateEditor();
    life.add(mySelectionLifecycle.getAnyCycleDetach());

    if (myEdit) {
      ConstraintDescriptor initialDescriptor = myInitialDescriptor;
      if (initialDescriptor != null) {
        for (EnumConstraintType type : myTypes.getCollectionModel()) {
          if (Util.equals(type.getDescriptor(), initialDescriptor)) {
            myTypes.getSelectionAccessor().setSelected(type);
            break;
          }
        }
      }
    }
  }

  private void updateEditor() {
    if (myCollapsed) return;
    EnumConstraintType type = myTypes.getSelectionAccessor().getSelection();
    if (!Util.equals(type, myCurrentType)) {
      mySelectionLifecycle.cycle();
      myCurrentType = type;
      if (type == null) {
        clearEditor();
      } else {
        Lifespan lifespan = mySelectionLifecycle.lifespan();
        lifespan.add(myGroupByLifespan.getAnyCycleDetach());
        AListModel<ItemKey> valuesModel = type.getEnumModel(lifespan, myContextCube);
//      myEnumForm.setModels(mySelectionLifecycle.lifespan(), fullModel, BasicScalarModel.createConstant(model));
        String id = type.getDescriptor().getId();
        Configuration config = myConfiguration.getOrCreateSubset("distSetup").getOrCreateSubset(id);

        myValuesFilterEditor.setConfiguration(lifespan, config, "addValueFilter");
        myValuesWGFilterEditor.setConfiguration(lifespan, config, "addValueFilter");
        myGroupsFilterEditor.setConfiguration(lifespan, config, "addGroupFilter");

        List<EnumGrouping> groupings = type.getAvailableGroupings();
        boolean showGroups = groupings != null && groupings.size() > 0;
        if (showGroups) {
          showParametersCard(GROUPS_CARD);
          connectGroups(lifespan, config, groupings, valuesModel, type);
        } else {
          showParametersCard(NOGROUPS_CARD);
          connectValues(lifespan, config, type, valuesModel, myValuesWGFilterEditor, myValuesWGPreview, null);
        }
      }
    }
  }

  private void connectValues(Lifespan lifespan, Configuration config, EnumConstraintType type,
    final AListModel<ItemKey> valuesModel, final StringFilterSetEditor filterEditor,
    final AList<ItemKey> preview, final SelectionAccessor<ItemKeyGroup> groupSelection)
  {
    connectValuesPreview(lifespan, preview, valuesModel, groupSelection);
    connectValuesFilter(lifespan, config, filterEditor, preview, type);
  }

  private void connectValuesFilter(Lifespan lifespan, Configuration config, final StringFilterSetEditor filterEditor,
    final AList<ItemKey> preview, final EnumConstraintType type)
  {
    final Configuration valuesConfig = config.getOrCreateSubset("vf");
    StringFilterSet set = null;
    final boolean edit = Util.equals(type.getDescriptor(), myInitialDescriptor);
    if (edit) {
      set = myInitialValuesFilterSet;
    }
    if (set == null) {
      set = StringFilterSet.readFrom(valuesConfig);
    }
    filterEditor.setValue(set);
    filterEditor.getModifiable().addChangeListener(lifespan, new ChangeListener() {
      public void onChange() {
        filterEditor.getCachedValue().writeTo(valuesConfig);
        if (edit) {
          // if we change something, no longer load initial
          myInitialValuesFilterSet = null;
        }
        preview.repaint();
      }
    });
    preview.repaint();
  }

  private void connectValuesPreview(Lifespan lifespan, final AList<ItemKey> preview,
    final AListModel<ItemKey> valuesModel, final SelectionAccessor<ItemKeyGroup> groupSelection)
  {
    if (groupSelection == null) {
      preview.setCollectionModel(valuesModel);
    } else {
      final Lifecycle selectedGroupsLife = new Lifecycle();
      lifespan.add(selectedGroupsLife.getDisposeDetach());
      ChangeListener modelUpdater = new ChangeListener() {
        public void onChange() {
          selectedGroupsLife.cycle();
          List<ItemKeyGroup> groups = groupSelection.getSelectedItems();
          AListModel<ItemKey> model;
          if (groups.size() == 0) {
            model = valuesModel;
          } else {
            final Set<ItemKeyGroup> groupSet = Collections15.hashSet(groups);
            final Lifespan life = selectedGroupsLife.lifespan();
            ImageBasedDecorator2<ItemKey, ItemKey> decorator =
              new ValuesInGroupsDecorator(life, valuesModel, groupSet);
            decorator.attach(life);
            model = decorator.getDecoratedImage();
          }
          preview.setCollectionModel(model, true);
        }
      };
      groupSelection.addChangeListener(lifespan, modelUpdater);
      modelUpdater.onChange();
    }
  }

  private void connectGroups(final Lifespan lifespan, final Configuration config, List<EnumGrouping> groupings,
    final AListModel<ItemKey> valuesModel, final EnumConstraintType type)
  {
    connectGroupByCombobox(lifespan, config, type, groupings, valuesModel);
    connectArrangeCheckbox(lifespan, config, type);
  }

  private void connectArrangeCheckbox(Lifespan lifespan, Configuration config, EnumConstraintType type) {
    boolean edit = Util.equals(type.getDescriptor(), myInitialDescriptor);
    DistributionParameters initialParemeters = myInitialParemeters;
    if (edit && initialParemeters != null) {
      myArrangeInGroups.setSelected(initialParemeters.isArrangeInGroups());
    } else {
      ConfigAttach.attachCheckbox(lifespan, myArrangeInGroups, config, ARRANGE_IN_GROUPS_SETTING, true);
    }
  }

  private void connectGroupByCombobox(Lifespan lifespan, final Configuration config, final EnumConstraintType type,
    List<EnumGrouping> groupings, final AListModel<ItemKey> valuesModel)
  {
    String selected = null;
    final boolean edit = Util.equals(type.getDescriptor(), myInitialDescriptor);
    if (edit) {
      DistributionParameters initialParemeters = myInitialParemeters;
      if (initialParemeters != null) {
        selected = initialParemeters.getGroupingName();
      }
    }
    if (selected == null) {
      selected = config.getSetting(SELECTED_GROUP_BY_SETTING, null);
    }

    final SelectionInListModel<EnumGrouping> model = buildGroupByModel(groupings, selected);
    myGroupBy.setModel(model);

    model.addSelectionChangeListener(lifespan, new ChangeListener() {
      public void onChange() {
        EnumGrouping grouping = model.getSelectedItem();
        config.setSetting(SELECTED_GROUP_BY_SETTING, grouping == null ? "none" : grouping.getDisplayableName());
        if (edit) {
          myInitialParemeters = null;
        }
        onGroupByChanged(valuesModel, config, type);
      }
    });
    onGroupByChanged(valuesModel, config, type);
  }

  private SelectionInListModel<EnumGrouping> buildGroupByModel(List<EnumGrouping> groupings, String selected) {
    List<EnumGrouping> modelValues = Collections15.arrayList();
    modelValues.add(null);
    modelValues.addAll(groupings);
    if (selected == null && groupings.size() > 0) {
      selected = groupings.get(0).getDisplayableName();
    }
    final SelectionInListModel<EnumGrouping> model = SelectionInListModel.create(modelValues, null);
    model.setSelectedItem(null);
    if (selected != null) {
      for (EnumGrouping grouping : groupings) {
        if (selected.equals(grouping.getDisplayableName())) {
          model.setSelectedItem(grouping);
          break;
        }
      }
    }
    return model;
  }

  private void onGroupByChanged(AListModel<ItemKey> valuesModel, Configuration config, EnumConstraintType type) {
    EnumGrouping selected = myGroupBy.getModel().getSelectedItem();
    boolean enabled = selected != null;
    myArrangeInGroups.setEnabled(enabled);
    myGroupsFilterEditor.setEnabled(enabled);
    myGroupsPreview.setEnabled(enabled);
    myGroupByLifespan.cycle();
    Lifespan groupLife = myGroupByLifespan.lifespan();
    if (enabled) {
      AListModel<ItemKeyGroup> groupModel = createGroupModel(groupLife, valuesModel, type, selected);
      myGroupsPreview.setCollectionModel(groupModel);
      connectGroupsFilter(groupLife, config, selected, type);
      SelectionAccessor<ItemKeyGroup> groupSelection = myGroupsPreview.getSelectionAccessor();
      connectValues(groupLife, config, type, valuesModel, myValuesFilterEditor, myValuesPreview, groupSelection);
    } else {
      myGroupsPreview.setCollectionModel(AListModel.EMPTY);
      myGroupsFilterEditor.clear();
      connectValues(groupLife, config, type, valuesModel, myValuesFilterEditor, myValuesPreview, null);
    }
  }

  private void connectGroupsFilter(Lifespan groupLife, Configuration config, EnumGrouping grouping,
    EnumConstraintType type)
  {
    final Configuration groupConfig = config.getOrCreateSubset("gf." + grouping.getDisplayableName());
    final boolean edit = Util.equals(type.getDescriptor(), myInitialDescriptor);
    StringFilterSet set = null;
    if (edit) {
      set = myInitialGroupsFilterSet;
    }
    if (set == null) {
      set = StringFilterSet.readFrom(groupConfig);
    }
    myGroupsFilterEditor.setValue(set);
    myGroupsFilterEditor.getModifiable().addChangeListener(groupLife, new ChangeListener() {
      public void onChange() {
        myGroupsFilterEditor.getCachedValue().writeTo(groupConfig);
        if (edit) {
          // if we change something, no longer load initial
          myInitialGroupsFilterSet = null;
        }
        myGroupsPreview.repaint();
        myValuesPreview.repaint();
      }
    });
  }

  private AListModel<ItemKeyGroup> createGroupModel(final Lifespan lifespan, AListModel<ItemKey> valuesModel,
    final EnumConstraintType type, final EnumGrouping grouping)
  {
    ImageBasedDecorator2<ItemKey, ItemKeyGroup> groups =
      new GroupModelConvertor(lifespan, valuesModel, type, grouping);
    groups.attach(lifespan);
    AListModel<ItemKeyGroup> vtg;
    myValueToGroupCachedImage = vtg = groups.getDecoratedImage();
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        myValueToGroupCachedImage = null;
      }
    });
    AListModel<ItemKeyGroup> unique = UniqueListDecorator.create(lifespan, vtg);
    SortedListDecorator sorted = SortedListDecorator.create(lifespan, unique, grouping.getComparator());
    return sorted;
  }

  private void clearEditor() {
    showParametersCard(NOTHING_CARD);
    myGroupBy.setModel(AComboboxModel.EMPTY_COMBOBOX);
    myGroupsPreview.setCollectionModel(AListModel.EMPTY);
    myValuesPreview.setCollectionModel(AListModel.EMPTY);
    myValuesWGPreview.setCollectionModel(AListModel.EMPTY);
  }

  private void showParametersCard(String card) {
    myShownCard = card;
    ((CardLayout) myParameters.getLayout()).show(myParameters, card);
  }

  private void setCollapsed(boolean collapsed) {
    boolean change = myCollapsed == null || myCollapsed != collapsed;
    myCollapsed = collapsed;
    String text = myCollapsed ? "Filter or group values >>" : "Filter or group values <<";
    myExpandButton.setText(text);
    if (change) {
      onCollapsedChange();
    }
  }

  private void onCollapsedChange() {
    if (myCollapsed) {
      // collapse
      myExpandablePanel.remove(mySplitPane);
      myExpandablePanel.add(myAttributes);
    } else {
      // expand
      mySplitPane.setLeftComponent(myAttributes);
      myExpandablePanel.add(mySplitPane);
    }

    // setup window size
    Dimension size = getSizeSetting().getDimensionOrNull();
    if (size == null) {
      JRootPane rootPane = getRootPane();
      Window window = getWindow();
      if (window != null && rootPane != null) {
        Insets insets = rootPane.getInsets();
        int w = rootPane.getSize().width + insets.left + insets.right;
        int h = window.getHeight();
        size = new Dimension(w, h);
      }
    }
    if (size != null) {
      setWindowSize(size);
    }

    updateEditor();
  }

  private void setWindowSize(@NotNull Dimension newSize) {
    Window window = getWindow();
    JRootPane rootPane = getRootPane();
    if (window != null && rootPane != null) {
      // set min size first
      Insets insets = myWholePanel.getInsets();
      window.setMinimumSize(new Dimension(insets.left + myWholePanel.getMinimumSize().width + insets.right, rootPane.getMinimumSize().height));
      // then window size
      window.setSize(newSize);
    }
  }

  public Dimension getLastSize() {
    return getSizeSetting().getDimensionOrNull();
  }

  private void saveSizeToConfig() {
    Window window = getWindow();
    if (window != null) {
      getSizeSetting().setDimension(window.getSize());
    }
  }

  private ConfigAccessors getSizeSetting() {
    return ConfigAccessors.dimension(curSettings());
  }

  private Configuration curSettings() {
    return myConfiguration.getOrCreateSubset(myCollapsed ? COLLAPSED_SUBSET : EXPANDED_SUBSET);
  }

  @Nullable
  private Window getWindow() {
    if (myWindow == null) {
      myWindow = SwingTreeUtil.findAncestorOfType(getComponent(), Window.class);
    }
    return myWindow;
  }

  @Nullable
  private JRootPane getRootPane() {
    if (myRootPane == null) {
      myRootPane = SwingTreeUtil.findAncestorOfType(getComponent(), JRootPane.class);
    }
    return myRootPane;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public Component getInitialFocusOwner() {
    return myTypes.getScrollable();
  }

  public EnumConstraintType getSelectedEnum() {
    return myTypes.getSelectionAccessor().getSelection();
  }

  public DistributionParameters buildDistributionParameters() {
    EnumGrouping grouping = myGroupBy.getModel().getSelectedItem();
    String groupingName = grouping == null ? null : grouping.getDisplayableName();
    boolean arrange = myGroupBy.isVisible() ? myArrangeInGroups.isSelected() : false;
    StringFilterSet valuesFilter;
    StringFilterSet groupsFilter;
    if (GROUPS_CARD.equals(myShownCard)) {
      groupsFilter = myGroupsFilterEditor.createValue();
      valuesFilter = myValuesFilterEditor.createValue();
    } else {
      groupsFilter = null;
      valuesFilter = myValuesWGFilterEditor.createValue();
    }
    return new DistributionParameters(
      groupingName, arrange, valuesFilter, groupsFilter, myHideEmptyQueries.isSelected());
  }

  public Boolean invoke(JPanel argument) {
    DialogManager dialogManager = Context.get(DialogManager.ROLE);
    if (dialogManager == null) {
      assert false;
      Log.warn("no dialog manager");
      return null;
    }
    DialogBuilder builder = dialogManager.createBuilder("addfilter");
    DialogResult<Boolean> result = new DialogResult<Boolean>(builder);
    builder.setTitle("Add Filter");
    builder.setContent(argument);
    builder.setModal(true);
    builder.setIgnoreStoredSize(true);
    result.setOkResult(Boolean.TRUE);
    result.setCancelResult(Boolean.FALSE);
    return result.showModal();
  }

  public JComponent createBottomLineComponent() {
    FormLayout formLayout = new FormLayout("l:d:none d:grow", "d");
    DefaultFormBuilder b = new DefaultFormBuilder(formLayout);
    b.add(myExpandButton, new CellConstraints());
    return b.getPanel();
  }

  private class GroupModelConvertor extends ImageBasedDecorator2<ItemKey, ItemKeyGroup> {
    private final Lifespan myLifespan;
    private final EnumConstraintType myType;
    private final EnumGrouping myGrouping;

    public GroupModelConvertor(Lifespan lifespan, AListModel<ItemKey> valuesModel, EnumConstraintType type,
      EnumGrouping grouping)
    {
      super(valuesModel);
      myLifespan = lifespan;
      myType = type;
      myGrouping = grouping;
    }

    protected List<? extends ItemKeyGroup> createImage(ItemKey sourceItem, int sourceIndex, boolean update) {
      if (myLifespan.isEnded())
        return null;
      ResolvedItem artifact = QueryUtil.getOneResolvedItem(sourceItem, myType.getDescriptor(), myContextCube);
      ItemKeyGroup group = myGrouping.getGroup(artifact);
      if (group == null)
        group = myGrouping.getNullGroup();
      return Collections.singletonList(group);
    }
  }


  private class FilteredKeyRenderer implements CanvasRenderer<ItemKey> {
    private final StringFilterSetEditor myValuesFilter;
    private final StringFilterSetEditor myGroupsFilter;

    public FilteredKeyRenderer(StringFilterSetEditor valuesFilter, StringFilterSetEditor groupsFilter) {
      myValuesFilter = valuesFilter;
      myGroupsFilter = groupsFilter;
    }

    public void renderStateOn(CellState state, Canvas canvas, ItemKey item) {
      if (item != ITEM_KEY_PROTOTYPE) {
        if (!state.isSelected() && !isAccepted(item, state.getCellRow())) {
          canvas.setForeground(ColorUtil.between(state.getForeground(), state.getDefaultBackground(), 0.7F));
        }
      }
      canvas.setIcon(item.getIcon());
      item.renderOn(canvas, state);
    }

    private boolean isAccepted(ItemKey item, int cellRow) {
      if (!myValuesFilter.getCachedValue().isAccepted(item.getDisplayName()))
        return false;
      if (myGroupsFilter == null)
        return true;
      AListModel<ItemKeyGroup> groupImage = myValueToGroupCachedImage;
      if (groupImage == null) {
        // detached?
        return true;
      }
      if (cellRow < 0 || cellRow >= groupImage.getSize()) {
        assert false;
        return true;
      }
      ItemKeyGroup keyGroup = groupImage.getAt(cellRow);
      return myGroupsFilter.getCachedValue().isAccepted(keyGroup.getDisplayableName());
    }
  }


  private static class FilteredGroupRenderer implements CanvasRenderer<ItemKeyGroup> {
    private final StringFilterSetEditor myFilterEditor;

    public FilteredGroupRenderer(StringFilterSetEditor filterEditor) {
      myFilterEditor = filterEditor;
    }

    public void renderStateOn(CellState state, Canvas canvas, ItemKeyGroup item) {
      if (item != ITEM_KEY_GROUP_PROTOTYPE) {
        StringFilterSet filter = myFilterEditor.getCachedValue();
        if (!state.isSelected() && !filter.isAccepted(item.getDisplayableName())) {
          canvas.setForeground(ColorUtil.between(state.getForeground(), state.getDefaultBackground(), 0.7F));
        }
      }
      canvas.appendText(item.getDisplayableName());
    }
  }


  private class ValuesInGroupsDecorator extends ImageBasedDecorator2<ItemKey, ItemKey> {
    private final Lifespan myLife;
    private final Set<ItemKeyGroup> myGroupSet;

    public ValuesInGroupsDecorator(Lifespan life, AListModel<ItemKey> valuesModel, Set<ItemKeyGroup> groupSet) {
      super(valuesModel);
      myLife = life;
      myGroupSet = groupSet;
    }

    protected List<? extends ItemKey> createImage(ItemKey sourceItem, int sourceIndex, boolean update) {
      if (myLife.isEnded())
        return null;
      AListModel<ItemKeyGroup> groupCache = myValueToGroupCachedImage;
      if (groupCache != null) {
        if (sourceIndex >= 0 && sourceIndex < groupCache.getSize()) {
          ItemKeyGroup group = groupCache.getAt(sourceIndex);
          if (!myGroupSet.contains(group)) {
            return null;
          }
        }
      }
      return Collections.singletonList(sourceItem);
    }
  }
}
