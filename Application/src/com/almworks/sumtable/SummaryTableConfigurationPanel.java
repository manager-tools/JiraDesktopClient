package com.almworks.sumtable;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.explorer.ExplorerDistributionTable;
import com.almworks.util.L;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Equality;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.ConfigGetter;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.i18n.text.util.NamePattern;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.ScrollBarPolicy;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class SummaryTableConfigurationPanel {
  public static final LocalizedAccessor I18N = CurrentLocale.createAccessor(SummaryTableConfigurationPanel.class.getClassLoader(), "com/almworks/sumtable/message");

  private final SelectionAccessor<Configuration> myListAccessor;
  private final JPanel myWholePanel;
  private final JTextField myDisplayNameField;
  private final ExplorerDistributionTable myParentTable;
  private final OrderListModel<Configuration> myModel;
  private final Configuration myTableConfigs;

  private @NotNull Configuration myCurrent;
  private WindowController myController = null;

  private static final Equality<Configuration> TABLE_CONFIGS_EQUALITY = new Equality<Configuration>() {
    @Override
    public boolean areEqual(Configuration o1, Configuration o2) {
      return o1.getName().equals(o2.getName());
    }
  };
  private static final String DISPLAY_NAME_SETTING = "displayName";
  private static final String AUTONAME_SETTING = "isAutoname";

  public SummaryTableConfigurationPanel(ExplorerDistributionTable parentTable, Configuration config) {
    myParentTable = parentTable;
    myTableConfigs = config;
    myCurrent = myTableConfigs.getOrCreateSubset(myTableConfigs.getOrSetDefault("currentId", "1"));

    myModel = OrderListModel.create();

    AList<Configuration> list = new AList<Configuration>();
    list.setCollectionModel(myModel);
    UIUtil.keepSelectionOnRemove(list);
    myListAccessor = list.getSelectionAccessor();
    final JScrollPane configsScrollPane = new JScrollPane(list);
    ScrollBarPolicy.setPolicies(configsScrollPane, ScrollBarPolicy.NEVER, ScrollBarPolicy.AS_NEEDED);
    list.setCanvasRenderer(new CanvasRenderer<Configuration>() {
      @Override
      public void renderStateOn(CellState state, Canvas canvas, Configuration item) {
        canvas.appendText(item.getSetting(DISPLAY_NAME_SETTING, "Unnamed"));
      }
    });

    list.addDoubleClickListener(Lifespan.FOREVER, new CollectionCommandListener<Configuration>() {
      @Override
      public void onCollectionCommand(ACollectionComponent<Configuration> configurationACollectionComponent, int index, Configuration element) {
        myController.close();
        myController = null;
      }
    });

    myDisplayNameField = new JTextField();
    myWholePanel = new JPanel(new BorderLayout());
    myWholePanel.add(createNorthToolbar(), BorderLayout.NORTH);
    myWholePanel.add(configsScrollPane, BorderLayout.CENTER);
    myWholePanel.add(createSouthContainer(), BorderLayout.SOUTH);
  }

  private Component createSouthContainer() {
//    FieldWithMoreButton<JTextField> nameFieldWrap = new FieldWithMoreButton<JTextField>();
//    nameFieldWrap.setKeystrokeEnabled(false);
//    nameFieldWrap.setField(myDisplayNameField);
//    nameFieldWrap.setAction(new EnabledAction() {
//      protected void doPerform(ActionContext context) throws CantPerformException {
//        setDefaultName();
//      }
//    });
//    nameFieldWrap.setActionName("Set name according to axis fields");
//    nameFieldWrap.setIcon(Icons.TRANSPOSE_SUMMARY_TABLE_ACTION_SMALL);
    JPanel result = new JPanel(new BorderLayout());
    Box b = new Box(BoxLayout.Y_AXIS);
    b.add(Box.createVerticalStrut(12));
    b.add(new JLabel("Current configuration name:"));
    b.add(Box.createVerticalStrut(4));
    result.add(b, BorderLayout.NORTH);
    result.add(myDisplayNameField /*nameFieldWrap*/, BorderLayout.CENTER);
    return result;
  }

  private void setDefaultName() {
    String newName = calcUniqueDisplayName(myCurrent, calcDefaultName());
    myModel.updateElement(myCurrent);
    updateNameField(newName);
  }

  private String calcDefaultName() {
    SummaryTableConfiguration summaryTableConfig = myParentTable.getSummaryTableConfig();
    AxisDefinition axisDefinition = summaryTableConfig.getRowsConfiguration().getAxisDefinition();
    String rowName = axisDefinition == null ? "<Empty>" : axisDefinition.getName();
    axisDefinition = summaryTableConfig.getColumnsConfiguration().getAxisDefinition();
    String columnName = axisDefinition == null ? "<Empty>" : axisDefinition.getName();
    return rowName + " - " + columnName;
  }

  private Component createNorthToolbar() {
    ToolbarBuilder builder = ToolbarBuilder.smallVisibleButtons();

    builder.addAction(new EnabledAction("Add configuration", Icons.ACTION_GENERIC_ADD) {
      protected void doPerform(ActionContext context) throws CantPerformException {
        addConfig(false);
      }
    });

    builder.addAction(new EnabledAction("Copy current configuration", Icons.ACTION_COPY) {
      protected void doPerform(ActionContext context) throws CantPerformException {
        addConfig(true);
      }
    });

    builder.addAction(new SimpleAction("Remove selected", Icons.ACTION_GENERIC_REMOVE) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnChange(myListAccessor);
        int selectedAmount = myListAccessor.getSelectedItems().size();
        int totalAmount = myModel.getSize();
        context.setEnabled(selectedAmount > 0 && selectedAmount < totalAmount);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        removeConfigs();
      }
    });

    return builder.createHorizontalToolbar();
  }

  private void addConfig(boolean makeCopy) {
    int newId = 1;
    while (myTableConfigs.isSet(String.valueOf(newId)))
      newId++;
    Configuration newConf = myTableConfigs.createSubset(String.valueOf(newId));
    if (makeCopy) {
      ConfigurationUtil.copyTo(myCurrent, newConf);
      calcUniqueDisplayName(newConf, newConf.getSetting(DISPLAY_NAME_SETTING, ""));
    }
    myModel.addElement(newConf);
    myListAccessor.setSelected(newConf);
  }

  private static final LocalizedAccessor.MessageIntStr M_PATTERN = I18N.messageIntStr("sumtable.configurations.duplicateName.name");
  private static final LocalizedAccessor.Value M_APPLIED = I18N.getFactory("sumtable.configurations.duplicateName.applied");
  private static final NamePattern PATTERN = new NamePattern(M_PATTERN, M_APPLIED);
  private static final ConfigGetter GET_DISPLAY_NAME = new ConfigGetter(DISPLAY_NAME_SETTING, "");
  private String calcUniqueDisplayName(Configuration config, String displayName) {
    String name = PATTERN.generateName(GET_DISPLAY_NAME.collectSet(myTableConfigs.getAllSubsets()), displayName);
    config.setSetting(DISPLAY_NAME_SETTING, name);
    return name;
  }

  private void removeConfigs() {
    java.util.List<Configuration> selectedConfigs = myListAccessor.getSelectedItems();
    assert selectedConfigs.size() < myModel.getSize() : selectedConfigs.size() + " " + myModel.getSize();

    myModel.removeAll(selectedConfigs);
    for (Configuration conf : selectedConfigs) conf.removeMe();
  }

  public void attach(final Lifespan lifespan) {
    myModel.clear();
    myModel.addAll(myTableConfigs.getAllSubsets());

    for (Configuration conf : myModel)
      if (TABLE_CONFIGS_EQUALITY.areEqual(myCurrent, conf)) {
        myCurrent = conf;
        break;
      }

    myListAccessor.setSelected(myCurrent);
    updateName();
    myListAccessor.addAWTChangeListener(lifespan, new ChangeListener() {
      public void onChange() {
        Configuration sel = myListAccessor.getFirstSelectedItem();
        if (sel != null && sel != myCurrent) {
          myCurrent = sel;
          myParentTable.changeCurrentTableConfig(myCurrent);
          myTableConfigs.setSetting("currentId", myCurrent.getName());
          updateName();
        }
      }
    });

    lifespan.add(UIUtil.addFocusListener(myDisplayNameField, new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        String name = myDisplayNameField.getText().trim();
        if (name.length() == 0) myCurrent.setSetting(AUTONAME_SETTING, true);
        else if (!name.equals(myCurrent.getSetting(DISPLAY_NAME_SETTING, ""))) {
          myCurrent.setSetting(AUTONAME_SETTING, false);
        }
        myCurrent.setSetting(DISPLAY_NAME_SETTING, name);
        updateName();
        myModel.updateElement(myCurrent);
      }
    }));

    SummaryTableConfiguration summaryTableConfig = myParentTable.getSummaryTableConfig();
    listenAxis(lifespan, summaryTableConfig.getRowsConfiguration());
    listenAxis(lifespan, summaryTableConfig.getColumnsConfiguration());
  }

  private void listenAxis(Lifespan lifespan, AxisConfiguration axis) {
    axis.getDefinitionModifiable().addAWTChangeListener(lifespan, new ChangeListener() {
      @Override
      public void onChange() {
        if (isCurrentAutoname()) {
          String displayName = GET_DISPLAY_NAME.convert(myCurrent);
          if (displayName.isEmpty()) setDefaultName();
          else {
            Integer index = PATTERN.getNameIndex(displayName);
            String name = calcDefaultName();
            myCurrent.setSetting(DISPLAY_NAME_SETTING, index != null ? PATTERN.generateName(name, index) : name);
          }
        }
      }
    });
  }

  private void updateName() {
    String res = myCurrent.getSetting(DISPLAY_NAME_SETTING, "");
    if (res.length() == 0) setDefaultName();
    else updateNameField(res);
  }

  private void updateNameField(String name) {
    myDisplayNameField.setText(name);
    Color bg = UIManager.getColor(isCurrentAutoname() ? "ToolTip.background" : "TextField.background");
    myDisplayNameField.setBackground(bg);
  }

  private boolean isCurrentAutoname() {
    return myCurrent.getBooleanSetting(AUTONAME_SETTING, true);
  }

  public void show(DialogManager dm) {
    if (myController == null) {
      final DialogBuilder builder = dm.createBuilder("SummaryTableConfigsWindow");
      builder.setTitle(L.dialog("Tabular Distribution Configurations"));
      builder.setContent(myWholePanel);
      builder.setCancelAction(new SimpleAction(L.actionName("Close Window")) {
        {
          setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.CTRL_ENTER);
        }

        protected void customUpdate(UpdateContext context) throws CantPerformException {}

        protected void doPerform(ActionContext context) throws CantPerformException {
          myController = null;
        }
      });
      builder.setPreferredSize(new Dimension(250, 280));
      builder.setBottomBevel(false);
      builder.showWindow();
      myController = builder.getWindowContainer().getActor(WindowController.ROLE);
    } else {
      if (!myController.isVisible())
        myController.show();
      myController.toFront();
    }
  }

  public Configuration getCurrentTableConfig() {
    return myCurrent;
  }
}
