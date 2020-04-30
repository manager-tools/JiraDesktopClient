package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.integers.IntArray;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.sync.download2.details.CustomFieldsSchema;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.jira.provider3.sync.schema.ServerIssueType;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.json.simple.JSONObject;

import java.util.Collections;
import java.util.List;

class FieldCollector {
  private final FieldScopes myFieldScopes = new FieldScopes();
  private final CustomFieldOptionsCollector myFieldOptions;

  FieldCollector(CustomFieldsComponent fieldsComponent) {
    myFieldOptions = new CustomFieldOptionsCollector(fieldsComponent);
  }

  public void processField(String fieldId, JSONObject field, int prjId, int typeId) {
    String atlassianClass = CustomFieldOptionsCollector.getFieldCustomClass(field);
    if (atlassianClass == null) {
      LogHelper.assertError(!CustomFieldsSchema.isCustomField(fieldId), "Missing field data", fieldId, field);
      return;
    }
    myFieldScopes.addScope(fieldId, prjId, typeId);
    myFieldOptions.addFieldOptions(fieldId, field);
  }

  /**
   * Marks all fields applicable in the scope
   */
  public void addAllScope(int prjId, int typeId) {
    myFieldScopes.addAllScope(prjId, typeId);
  }

  public void postProcess(final EntityTransaction transaction, boolean fullSet) {
    myFieldScopes.postProcess(transaction);
    myFieldOptions.postProcess(transaction, fullSet);
  }

  private static class FieldScopes {
    private final List<String> myFieldIds = Collections15.arrayList();
    private final List<IntArray> myProjectScope = Collections15.arrayList();
    private final List<IntArray> myTypeScope = Collections15.arrayList();
    private final IntArray myAllProject = new IntArray();
    private final IntArray myAllType = new IntArray();

    public void addAllScope(int prjId, int typeId) {
      for (int i = 0; i < myFieldIds.size(); i++) {
        myProjectScope.get(i).addSorted(prjId);
        myTypeScope.get(i).addSorted(typeId);
      }
      myAllProject.addSorted(prjId);
      myAllType.addSorted(typeId);
    }

    public void addScope(String fieldId, int prjId, int typeId) {
      IntArray projects;
      IntArray types;
      int index = Collections.binarySearch(myFieldIds, fieldId);
      if (index < 0) {
        index = -index - 1;
        myFieldIds.add(index, fieldId);
        projects = IntArray.copy(myAllProject);
        types = IntArray.copy(myAllType);
        myProjectScope.add(index, projects);
        myTypeScope.add(index, types);
      } else {
        projects = myProjectScope.get(index);
        types = myTypeScope.get(index);
      }
      projects.addSorted(prjId);
      types.addSorted(typeId);
    }

    public void postProcess(EntityTransaction transaction) {
      final EntityBag2 allFields = transaction.addBag(ServerCustomField.TYPE);
      allFields.changeValue(ServerCustomField.ONLY_IN_PROJECTS, null);
      allFields.changeValue(ServerCustomField.ONLY_IN_ISSUE_TYPES, null);
      for (int i = 0; i < myFieldIds.size(); i++) {
        String fieldId = myFieldIds.get(i);
        EntityHolder field = ServerCustomField.getField(transaction, fieldId);
        EntityUtils.setRefCollectionByIds(field, ServerCustomField.ONLY_IN_PROJECTS, myProjectScope.get(i), ServerProject.TYPE, ServerProject.ID);
        EntityUtils.setRefCollectionByIds(field, ServerCustomField.ONLY_IN_ISSUE_TYPES, myTypeScope.get(i), ServerIssueType.TYPE, ServerIssueType.ID);
        allFields.exclude(field);
      }
    }
  }
}
