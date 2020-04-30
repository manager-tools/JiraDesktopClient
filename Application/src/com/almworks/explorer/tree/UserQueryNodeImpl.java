package com.almworks.explorer.tree;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.ItemCollectionContext;
import com.almworks.api.application.ItemSource;
import com.almworks.api.application.qb.FilterEditor;
import com.almworks.api.application.qb.FilterEditorProvider;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.application.qb.QueryBuilderComponent;
import com.almworks.api.application.tree.*;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.gui.DialogEditorBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.items.api.Database;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.TextEditor;
import org.almworks.util.Failure;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 *         The class is final because calling constructor from subclass is not safe
 */
final class UserQueryNodeImpl extends AbstractQueryNode implements UserQueryNode {
  public UserQueryNodeImpl(Database db, QueryPresentation presentation, Configuration config) {
    super(db, presentation, config);
    addAllowedChildType(TreeNodeFactory.NodeType.FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.QUERY);
    addAllowedChildType(TreeNodeFactory.NodeType.DISTRIBUTION_FOLDER);
    addAllowedChildType(TreeNodeFactory.NodeType.LAZY_DISTRIBUTION);
  }

  public DialogEditorBuilder openQueryEditor(String title, final QueryBuilderComponent qb) {
    RootNode rootNode = getRoot();
    assert rootNode != null;
    ComponentContainer rootContainer = rootNode.getContainer();
    DialogManager dialogManager = rootContainer.requireActor(DialogManager.ROLE);
    FilterEditorProvider filterEditorProvider = rootContainer.requireActor(FilterEditorProvider.ROLE);
    final ExplorerComponent explorer = rootContainer.requireActor(ExplorerComponent.ROLE);

    FilterNode root = getFilterStructure();
    assert root != null : this;
    DialogEditorBuilder builder = dialogManager.createEditor("queryBuilder");
    TextEditor nameEditor = new TextEditor();
    MutableComponentContainer container = builder.getWindowContainer();
    GenericNode parent = getParent();
    ItemHypercube hypercube = parent == null ? new ItemHypercubeImpl() : parent.getHypercube(false);
    if (hypercube == null) hypercube = new ItemHypercubeImpl();
    final FilterEditor filterEditor = filterEditorProvider.createFilterEditor(root, hypercube, container);
    builder.setContent(qb.createQueryEditor(nameEditor, filterEditor));
    builder.setTitle(title);
    builder.setPreferredSize(new Dimension(680, 420));
    final JCheckBox runningImmediately = qb.createRunImmediatlyCheckbox();
    builder.setBottomLineComponent(runningImmediately);
    nameEditor.setTarget(getPresentation());
    builder.addStateListener(new Procedure<DialogEditorBuilder.EditingEvent>() {
      public void invoke(DialogEditorBuilder.EditingEvent arg) {
        if (arg.isApplying()) {
          setFilter(filterEditor.getCurrentFilter());
          if (runningImmediately.isSelected()) {
            QueryResult queryResult = UserQueryNodeImpl.this.getQueryResult();
            ItemSource source = queryResult.getItemSource();
            ItemCollectionContext context = queryResult.getCollectionContext();
            if (source == null || context == null)
              throw new Failure();
            context.forceRerun();
            explorer.showItemsInTab(source, context, false);
          }
        }
      }
    });
    return builder;
  }

  public boolean isRenamable() {
    return true;
  }
}
