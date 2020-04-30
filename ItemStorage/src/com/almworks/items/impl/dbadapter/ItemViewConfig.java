package com.almworks.items.impl.dbadapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static org.almworks.util.Collections15.arrayList;
import static org.almworks.util.Collections15.unmodifiableListCopy;

public class ItemViewConfig {
  public static final ItemViewConfig SIMPLEST = new ItemViewConfig(null, null, null);

  @NotNull
  private final List<SortingElement> mySorting;

  @Nullable
  private final SparseGraphTreeInfo myTreeInfo;

  @NotNull
  private final List<Grouping> myGroupings;

  private ItemViewConfig(List<SortingElement> sorting, SparseGraphTreeInfo treeInfo, List<Grouping> groupings) {
    myTreeInfo = treeInfo;
    mySorting = unmodifiableListCopy(sorting);
    myGroupings = unmodifiableListCopy(groupings);
  }

  @NotNull
  public List<SortingElement> getSorting() {
    return mySorting;
  }

  @Nullable
  public SparseGraphTreeInfo getTreeInfo() {
    return myTreeInfo;
  }

  @NotNull
  public List<Grouping> getGroupings() {
    return myGroupings;
  }

  public static class SortingElement {
    private final DBTable myTable;
    private final DBColumn<?> myColumn;
    private final boolean myDescending;

    public SortingElement(@NotNull DBTable table, @NotNull DBColumn<?> column, boolean descending) {
      myTable = table;
      myColumn = column;
      myDescending = descending;
    }

    public DBTable getTable() {
      return myTable;
    }

    public DBColumn<?> getColumn() {
      return myColumn;
    }

    public boolean isDescending() {
      return myDescending;
    }
  }

  public static class SparseGraphTreeInfo {
    private final DBTable myTable;
    private final DBIntColumn myItemLeftColumn;
    private final DBIntColumn myItemRightColumn;

    public SparseGraphTreeInfo(DBTable table, DBIntColumn itemLeftColumn, DBIntColumn itemRightColumn) {
      myTable = table;
      myItemLeftColumn = itemLeftColumn;
      myItemRightColumn = itemRightColumn;
    }

    public DBTable getTable() {
      return myTable;
    }

    public DBIntColumn getItemLeftColumn() {
      return myItemLeftColumn;
    }

    public DBIntColumn getItemRightColumn() {
      return myItemRightColumn;
    }
  }

  /**
   * Defines grouping by some attribute in the terms of the path to the column that defines the attribute (attribute path).
   * The values in this column will be used as group IDs.
   * Note that the last step in the attribute path is treated specially, as the whole path may consist (minimum) of this step only.
   * @see AttributeStep
   */
  public static class Grouping {
    private final List<AttributeStep> mySteps;

    public Grouping(List<AttributeStep> steps, AttributeStep groupingAttribute) {
      mySteps = arrayList(steps);
      mySteps.add(groupingAttribute);
    }

    public Grouping(AttributeStep groupingAttribute) {
      this(Collections.EMPTY_LIST, groupingAttribute);
    }

    /**
     * Returns all steps in the attribute path, including the last.
     * @return
     */
    public List<AttributeStep> getAllSteps() {
      return mySteps;
    }

    public AttributeStep getAttribute() {
      assert !mySteps.isEmpty();
      return mySteps.get(mySteps.size() - 1);
    }
  }

  public static class Builder {
    private final List<SortingElement> mySorting = arrayList();
    private SparseGraphTreeInfo myTreeInfo;
    private List<Grouping> myGroupings = arrayList();

    public Builder sort(DBTable table, DBColumn<?> column, boolean descending) {
      if (table == null || column == null) {
        assert false;
      } else {
        mySorting.add(new SortingElement(table, column, descending));
      }
/*
      // todo[sank] delete
      // !!! for the example to succeed, tables must be inserted into the DB: see Grouping (Email - TwoSteps)

      Grouping grouping1 = new Grouping(
        arrayList(
          new AttributeStep("MyTestTable", "jira_issue_assignee", "id", "iid"),
          new AttributeStep("jira_issue_assignee", "AssigneeEmail", "value", "iid"),
          new AttributeStep("AssigneeEmail", "EmailValues", "MailId", "iid")),
        "EmailValues", "iid", false
      );
      Grouping grouping2 = new Grouping(
        arrayList(
          new AttributeStep("MyTestTable", "jira_issue_prj", "id", "iid")),
        "jira_issue_prj", "value", false
      );
      group(grouping1);
      group(grouping2);
*/
      return this;
    }

    public Builder group(Grouping groupingDefinition) {
      myGroupings.add(groupingDefinition);
      return this;
    }

    public Builder buildTree(DBTable table, DBIntColumn left, DBIntColumn right) {
      myTreeInfo = new SparseGraphTreeInfo(table, left, right);
      return this;
    }

    public Builder clear() {
      mySorting.clear();
      myTreeInfo = null;
      return this;
    }

    public ItemViewConfig create() {
      return new ItemViewConfig(mySorting, myTreeInfo, myGroupings);
    }
  }
}
