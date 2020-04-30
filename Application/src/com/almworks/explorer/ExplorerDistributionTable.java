package com.almworks.explorer;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.gui.DialogManager;
import com.almworks.items.api.Database;
import com.almworks.sumtable.*;
import com.almworks.util.components.AToolbar;
import com.almworks.util.components.ATreeNode;
import com.almworks.util.components.SelectionAccessor;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.MapMedium;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class ExplorerDistributionTable implements SummaryTableQueryExecutor {
  public static final TypedKey<ExplorerDistributionTable> ROLE = DataRole.createRole(ExplorerDistributionTable.class);
  
  private static final String NO_NODE_CARD = "nonode";
  private static final String CONTENT_CARD = "content";

  private final JPanel myMainPanel = new JPanel(new BorderLayout());
  private final JPanel myHeaderPanel = new JPanel(new BorderLayout());
  private final JPanel myWholePanel = new JPanel(new CardLayout());
  private final JLabel myWarningLabel = new JLabel();
  private final SummaryTableMainPanel myTable;
  private final ExplorerComponent myExplorer;
  private final SelectionAccessor<ATreeNode<GenericNode>> myTreeSelection;
  private final Configuration myConfig;
  private final Lifecycle myTableAttachLife = new Lifecycle();
  private final SummaryTableConfiguration myTableConfiguration;
  private final SummaryTableConfigurationPanel myTableConfigPanel;

  private final Map<String, SummaryTableFrame> myNodeFrames = Collections15.hashMap();

  private final RowColumnConfig myRowColumnConfig;

  private final Bottleneck myAttachTableBottleneck = new Bottleneck(200, ThreadGate.AWT, new Runnable() {
    public void run() {
      attachTable();
    }
  });

  public ExplorerDistributionTable(ExplorerComponent explorer, SelectionAccessor<ATreeNode<GenericNode>> treeSelection, Configuration config) {
    myExplorer = explorer;
    myTreeSelection = treeSelection;
    myConfig = config;
    myTableConfigPanel = new SummaryTableConfigurationPanel(this, myConfig.getOrCreateSubset("tableConfigs"));

    myTableConfiguration = new SummaryTableConfiguration(myTableConfigPanel.getCurrentTableConfig());
    myRowColumnConfig = new RowColumnConfig(myTableConfiguration);
    myTable = new SummaryTableMainPanel(config, myTableConfiguration, false, this);

    myHeaderPanel.add(myWarningLabel, BorderLayout.NORTH);
    myHeaderPanel.add(createToolbar(), BorderLayout.CENTER);

    myMainPanel.add(myTable.getComponent(), BorderLayout.CENTER);
    myMainPanel.add(myHeaderPanel, BorderLayout.NORTH);

    myTable.installSummaryDataProvider(myMainPanel);

    myWholePanel.add(myMainPanel, CONTENT_CARD);
    myWholePanel.add(createMessage(), NO_NODE_CARD);
    ConstProvider.addGlobalValue(myWholePanel, ROLE, this);
  }

  public void changeCurrentTableConfig(Configuration selected) {
    myTableConfiguration.changeConfig(selected);
    attachTable();
  }
  
  public SummaryTableConfiguration getSummaryTableConfig() {
    return myTableConfiguration;
  }

  private JComponent createMessage() {
    final JComponent panel = UIUtil.createMessagePanel(
      "<html><body>Please select a context node above",
      true, true, UIUtil.BORDER_9);
    Aqua.cleanScrollPaneBorder(panel);
    return panel;
  }

  public AToolbar createToolbar() {
    ToolbarBuilder builder = new ToolbarBuilder();
    builder.addAction(new OpenWindowAction());
    builder.addAction(new ConfigureSummaryTableAction(myTable));
    builder.addAction(new ExportToCSVAction());
    builder.addAction(new ManageTableConfigsAction());
    builder.addSeparator();
    myRowColumnConfig.buildToolbar(builder);
    return builder.createHorizontalToolbar();
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void attach(Lifespan lifespan) {
    if (lifespan.isEnded())
      return;
    myTableConfigPanel.attach(lifespan);
    myTreeSelection.addChangeListener(lifespan, myAttachTableBottleneck);
    attachTable();
    lifespan.add(myTableAttachLife.getAnyCycleDetach());
  }

  private void attachTable() {
    myTableAttachLife.cycle();
    GenericNode node = getCurrentNode();
    if (node != null && isValid(node.getQueryResult())) {
      if (!myMainPanel.isVisible()) {
        ((CardLayout) myWholePanel.getLayout()).show(myWholePanel, CONTENT_CARD);
      }
      Lifespan life = myTableAttachLife.lifespan();
      myTable.attach(life, node);
      myRowColumnConfig.attach(life, node.getQueryResult());
    } else {
      if (myMainPanel.isVisible()) {
        ((CardLayout) myWholePanel.getLayout()).show(myWholePanel, NO_NODE_CARD);
      }
    }
  }

  private boolean isValid(QueryResult result) {
    return result != null && result != QueryResult.NO_RESULT;
  }

  public QueryResult getCurrentQueryResult() {
    GenericNode node = getCurrentNode();
    return node == null ? null : node.getQueryResult();
  }

  public GenericNode getCurrentNode() {
    List<ATreeNode<GenericNode>> selection = myTreeSelection.getSelectedItems();
    return selection.size() == 1 ? selection.get(0).getUserObject() : null;
  }

  public void runQuery(QueryResult queryResult, STFilter counter, STFilter column, STFilter row, Integer count,
    boolean newTab)
  {
    ATreeNode<GenericNode> treeNode = myTreeSelection.getSelection();
    if (treeNode == null) {
      assert false : queryResult;
      return;
    }
    SummaryTableFrame.executeQuery(myExplorer, treeNode.getUserObject(), queryResult, counter, column, row, count, newTab);
  }

  @Override
  public Database getDatabase() {
    return myExplorer.getEngine().getDatabase();
  }

  private class OpenWindowAction extends SimpleAction {
    public OpenWindowAction() {
      super("", Icons.OPEN_TABLE_IN_A_WINDOW);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
        "Open a window with a copy of this tabular distribution, pinned to the currently selected node");
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.updateOnChange(myTreeSelection);
      QueryResult result = getCurrentQueryResult();
      context.setEnabled(isValid(result));
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      ATreeNode<GenericNode> treeNode = myTreeSelection.getSelection();
      if (treeNode == null)
        return;
      GenericNode node = treeNode.getUserObject();
      final String nodeId = node.getNodeId();
      SummaryTableFrame frame = myNodeFrames.get(nodeId);
      if (frame == null) {
        Configuration tempConfig = MapMedium.createConfig();
        ConfigurationUtil.copyTo(myTableConfigPanel.getCurrentTableConfig(), tempConfig);
        SummaryTableConfiguration tableConfig = new SummaryTableConfiguration(tempConfig);
        ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
        frame = SummaryTableFrame.create(explorer, tableConfig, node, nodeId, new Detach() {
          protected void doDetach() {
            myNodeFrames.remove(nodeId);
          }
        });
        myNodeFrames.put(nodeId, frame);
      }
      frame.show();
    }
  }

  private class ManageTableConfigsAction extends SimpleAction {
    public ManageTableConfigsAction() {
      super("", Icons.ACTION_EDIT_QUERY);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
          "Manage saved tabular distribution configurations");
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      DialogManager dm = context.getSourceObject(DialogManager.ROLE);
      myTableConfigPanel.show(dm);
    }
  }
}
