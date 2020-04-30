package com.almworks.jira.provider3.issue.features.edit;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.QueryNode;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.helper.EditDescriptor;
import com.almworks.items.gui.edit.util.DefaultValues;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.gui.edit.editors.JiraEditUtils;
import com.almworks.jira.provider3.issue.features.BaseEditIssueFeature;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.util.Pair;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateRequest;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;

class NewIssueHereFeature extends BaseEditIssueFeature {
  NewIssueHereFeature() {
    super(CreateIssueFeature.EDITOR);
  }

  @NotNull
  @Override
  public EditDescriptor.Impl doCheckContext(ActionContext context, UpdateRequest updateRequest) throws CantPerformException {
    updateRequest.watchRole(GenericNode.NAVIGATION_NODE);
    Pair<JiraConnection3, ItemHypercube> target = getTarget(context);
    JiraConnection3 connection = target.getFirst();
    CantPerformException.ensure(connection.isUploadAllowed());
    return CreateIssueFeature.newIssueDescriptor(connection);
  }

  private Pair<JiraConnection3, ItemHypercube> getTarget(ActionContext context) throws CantPerformException {
    List<GenericNode> nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
    nodes = CantPerformException.ensureNotNull(nodes);
    JiraConnection3 connection = CantPerformException.ensureNotNull(JiraEditUtils.findConnection(context, nodes));
    ItemHypercubeImpl cube = new ItemHypercubeImpl();
    ItemHypercubeUtils.adjustForConnection(cube, connection);
    for (GenericNode node : nodes) {
      QueryNode queryNode = Util.castNullable(QueryNode.class, node);
      if (queryNode == null) queryNode = node.getAncestorOfType(QueryNode.class);
      ItemHypercube nodeCube = null;
      while (queryNode != null) {
        nodeCube = queryNode.getHypercube(false);
        if (nodeCube != null) break;
        GenericNode parent = queryNode.getParent();
        queryNode = parent == null ? null : parent.getAncestorOfType(QueryNode.class);
      }
      if (nodeCube == null) continue;
      for (DBAttribute<?> attribute : nodeCube.getAxes()) {
        SortedSet<Long> nodeInclude = nodeCube.getIncludedValues(attribute);
        if (nodeInclude == null || nodeInclude.isEmpty()) continue;
        cube.addAxisIncluded(attribute, nodeInclude);
      }
    }
    return Pair.create(connection, (ItemHypercube) cube);
  }

  private Long getSingleValue(ItemHypercube cube, DBAttribute<Long> attribute) {
    SortedSet<Long> values = cube.getIncludedValues(attribute);
    if (values == null || values.size() != 1) return null;
    return values.iterator().next();
  }

  @Override
  public DefaultEditModel.Root setupModel(ActionContext context, WritableLongList itemsToLock) throws CantPerformException {
    Pair<JiraConnection3, ItemHypercube> target = getTarget(context);
    JiraConnection3 connection = target.getFirst();
    ItemHypercube cube = target.getSecond();
    cube = CleanupHypercube.create(context.getSourceObject(GuiFeaturesManager.ROLE)).cleanup(cube);
    DefaultValues defaults = new DefaultValues(connection.getConnectionObj(), Issue.ISSUE_DEFAULTS, CreateIssueFeature.GENERIC_DEFAULTS);
    for (DBAttribute<?> attribute : cube.getAxes()) {
      if (SyncAttributes.CONNECTION.equals(attribute)) continue;
      DBAttribute<Long> singleValue = BadUtil.castScalar(Long.class, attribute);
      if (singleValue != null) {
        Long value = getSingleValue(cube, singleValue);
        if (value != null && Issue.ISSUE_TYPE.equals(singleValue)) {
          GuiFeaturesManager manager = context.getSourceObject(GuiFeaturesManager.ROLE);
          LoadedItemKey type = LoadedIssueUtil.getItemKey(manager, IssueType.ENUM_TYPE, value);
          if (type != null && !IssueType.isSubtask(type, false)) value = null;
        }
        defaults.override(singleValue, value);
      } else {
        DBAttribute<Set<Long>> setAttribute = BadUtil.castSetAttribute(Long.class, attribute);
        SortedSet<Long> values = cube.getIncludedValues(setAttribute);
        defaults.override(setAttribute, values);
      }
    }
    DefaultEditModel.Root model = DefaultEditModel.Root.newItem(CreateIssueFeature.ISSUE_CREATOR);
    EngineConsts.setupConnection(model, connection);
    setDefaults(model, defaults);
    return model;
  }
}
