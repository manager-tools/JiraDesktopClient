package com.almworks.explorer.tree;

import com.almworks.api.application.tree.DistributionFolderNode;
import com.almworks.api.application.tree.DistributionParameters;
import com.almworks.api.application.tree.DistributionQueryNode;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.util.L;
import com.almworks.util.i18n.Local;
import com.almworks.util.sfs.StringFilter;
import com.almworks.util.sfs.StringFilterSet;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ExcludeDisributionNodeAction extends SimpleAction {
  public ExcludeDisributionNodeAction() {
    super(Local.parse("&Exclude from Distribution"));
    setDefaultText(PresentationKey.SHORT_DESCRIPTION, L.tooltip("Exclude selected options from distribution"));
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    watchRole(GenericNode.NAVIGATION_NODE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    List<GenericNode> nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
    DistributionFolderNode parent = getCommonParent(nodes);
    context.setEnabled(parent != null);
  }

  private DistributionFolderNode getCommonParent(List<GenericNode> nodes) throws CantPerformException {
    DistributionFolderNode parent = null;
    for (GenericNode node : nodes) {
      if (!(node instanceof DistributionQueryNode)) {
        throw new CantPerformException();
      }
      GenericNode p = node.getParent();
      if (p != null && !(p instanceof DistributionFolderNode))
        p = p.getParent();
      if (p == null || !(p instanceof DistributionFolderNode))
        throw new CantPerformException();
      if (parent == null)
        parent = (DistributionFolderNode) p;
      else if (parent != p)
        throw new CantPerformException();
    }
    return parent;
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    List<GenericNode> nodes = context.getSourceCollection(GenericNode.NAVIGATION_NODE);
    DistributionFolderNode parent = getCommonParent(nodes);
    if (parent == null)
      return;
    final Set<String> names = Collections15.linkedHashSet();
    for (GenericNode node : nodes) {
      names.add(((DistributionQueryNode) node).getPresentation().getText());
    }
    DistributionParameters oldparams = parent.getParameters();
    StringFilterSet oldFilters = oldparams.getValuesFilter();
    StringFilterSet.Kind kind = oldFilters == null ? StringFilterSet.Kind.EXCLUSIVE : oldFilters.getKind();
    if (kind == StringFilterSet.Kind.ALL)
      kind = StringFilterSet.Kind.EXCLUSIVE;
    List<StringFilter> oldlist = oldFilters == null ? Collections15.<StringFilter>emptyList() : oldFilters.getFilters();
    List<StringFilter> newlist = Collections15.arrayList(oldlist);
    if (kind == StringFilterSet.Kind.INCLUSIVE) {
      for (Iterator<StringFilter> ii = newlist.iterator(); ii.hasNext();) {
        StringFilter sf = ii.next();
        if (matchesAnyExactly(sf, names)) {
          ii.remove();
        }
      }
    } else {
      for (String name : names) {
        newlist.add(new StringFilter(StringFilter.MatchType.EXACT, name, false));
      }
    }
    StringFilterSet newFilter = new StringFilterSet(kind, newlist);    
    DistributionParameters params = new DistributionParameters(oldparams.getGroupingName(),
      oldparams.isArrangeInGroups(), newFilter, oldparams.getGroupsFilter(), oldparams.isHideEmptyQueries());
    parent.setParameters(parent.getDescriptor(), params);
  }

  private boolean matchesAnyExactly(StringFilter sf, Set<String> names) {
    if (sf.getMatchType() != StringFilter.MatchType.EXACT)
      return false;
    for (String name : names) {
      if (sf.isAccepted(name))
        return true;
    }
    return false;
  }
}
