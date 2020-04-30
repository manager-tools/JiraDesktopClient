package com.almworks.jira.provider3.gui.edit.editors.move;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.LogHelper;
import gnu.trove.TLongObjectHashMap;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParentSupport {
  private static final TypedKey<ParentSupport> KEY = TypedKey.create("parentSupport");
  private static final TypedKey<Long> PARENT_ISSUE = TypedKey.create("issueSubtaskMode");
  /** Maps edited issue to it's parent (or does not contain mapping for generic issues) */
  private final TLongObjectHashMap<Long> myParentMap;
  /** Set of all parents of edited issues */
  private final LongList myParents;
  /** At least one edited issue is generic issue */
  private final boolean myHasGeneric;

  private ParentSupport(boolean hasGeneric, TLongObjectHashMap<Long> parentMap, LongList parents) {
    myParents = parents;
    myHasGeneric = hasGeneric;
    myParentMap = parentMap;
  }

  public static long getParentIssue(EditModelState model) {
    LogHelper.assertError(model.isNewItem());
    return Util.NN(model.getValue(PARENT_ISSUE), 0l);
  }

  public static void copyParent(EditItemModel source, EditItemModel target) {
    target.putHint(PARENT_ISSUE, source.getValue(PARENT_ISSUE));
  }

  @NotNull
  public static ParentSupport ensureLoaded(VersionSource source, EditModelState model) {
    ParentSupport parentSupport = model.getValue(KEY);
    if (parentSupport == null) {
      parentSupport = load(source, model);
      model.putHint(KEY, parentSupport);
    }
    return parentSupport;
  }

  private static ParentSupport load(VersionSource source, EditModelState model) {
    LongList parents;
    boolean hasGeneric;
    TLongObjectHashMap<Long> parentMap = new TLongObjectHashMap<>();
    if (model.isNewItem()) {
      long singleParent = getParentIssue(model);
      parents = singleParent > 0 ? LongArray.create(singleParent) : LongList.EMPTY;
      hasGeneric = singleParent <= 0;
    } else {
      hasGeneric = false;
      LongList issues = model.getEditingItems();
      LongArray set = new LongArray();
      for (int i = 0; i < issues.size(); i++) {
        long issue = issues.get(i);
        long parent = Issue.getParent(source.forItem(issue));
        if (parent <= 0) {
          hasGeneric = true;
          parent = 0;
        } else set.add(parent);
        parentMap.put(issue, parent);
      }
      set.sortUnique();
      parents = set;
    }
    return new ParentSupport(hasGeneric, parentMap, parents);
  }

  /**
   * @return true if all edited issues are generic issues
   */
  public boolean isGenericOnly() {
    return myHasGeneric && myParents.isEmpty();
  }

  public boolean isSubtaskOnly() {
    return !myHasGeneric;
  }

  public long getInitialParent(long item) {
    return myParentMap.get(item);
  }

  @Nullable
  public static ParentSupport getInstance(EditModelState model) {
    return model.getValue(KEY);
  }

  public LongList getAllParents() {
    return myParents;
  }

  public static void prepareSubtask(EditModelState model, long parent) {
    model.putHint(PARENT_ISSUE, parent);
  }
}
