package com.almworks.jira.provider3.issue.editor;

import com.almworks.integers.LongList;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class FieldsFilter {
  private final Set<String> myHiddenFields = Collections15.hashSet();
  private final List<ResolvedField> myAllFields;
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private boolean myOn;

  public FieldsFilter(List<ResolvedField> allFields) {
    myAllFields = allFields;
  }

  public boolean canHide(Collection<String> fieldIds) {
    for (String fieldId : fieldIds) if (myHiddenFields.contains(fieldId)) return true;
    return false;
  }

  private static void chooseHidden(Collection<ResolvedField> allFields, LongList hidden, Collection<String> target) {
    for (ResolvedField field : allFields) if (hidden.contains(field.getItem())) target.add(field.getJiraId());
  }

  public static FieldsFilter load(ItemVersion connection) {
    LongList hidden = Jira.HIDDEN_EDITORS.getValue(connection);
    List<ResolvedField> allFields = ResolvedField.loadAll(connection);
    FieldsFilter filter = new FieldsFilter(allFields);
    chooseHidden(allFields, hidden, filter.myHiddenFields);
    return filter;
  }

  public Modifiable getModifiable() {
    return myModifiable;
  }

  public boolean isOn() {
    return myOn;
  }

  public void setOn(boolean on) {
    if (on == myOn) return;
    myOn = on;
    myModifiable.fireChanged();
  }

  public void toggle() {
    setOn(!myOn);
  }

  public void setHidden(LongList hidden) {
    myHiddenFields.clear();
    chooseHidden(myAllFields, hidden, myHiddenFields);
    myModifiable.fireChanged();
  }

  public List<IssueScreen.Tab> filter(List<IssueScreen.Tab> tabs) {
    if (!myOn) return tabs;
    ArrayList<IssueScreen.Tab> result = Collections15.arrayList();
    for (IssueScreen.Tab tab : tabs) {
      List<String> filteredFields = Collections15.arrayList();
      for (String fieldId : tab.getFieldIds()) if (!myHiddenFields.contains(fieldId)) filteredFields.add(fieldId);
      result.add(new IssueScreen.Tab(tab.getName(), filteredFields));
    }
    return result;
  }
}
