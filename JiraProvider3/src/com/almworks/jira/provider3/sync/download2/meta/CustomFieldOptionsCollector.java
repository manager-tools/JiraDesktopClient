package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.sync.download2.details.CustomFieldsSchema;
import com.almworks.jira.provider3.sync.download2.rest.JRField;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public class CustomFieldOptionsCollector {
  private final Map<String, Pair<OptionsLoader<Object>, Object>> myFieldOptions = Collections15.hashMap();
  private final CustomFieldsComponent myFieldsComponent;

  public CustomFieldOptionsCollector(CustomFieldsComponent fieldsComponent) {
    myFieldsComponent = fieldsComponent;
  }

  public static String getFieldCustomClass(JSONObject field) {
    return CustomFieldsSchema.getFieldCustomClass(JRField.SCHEMA.getValue(field));
  }

  public void addFieldOptions(String fieldId, JSONObject field) {
    String atlassianClass = getFieldCustomClass(field);
    FieldKind kind = myFieldsComponent.getFieldKind(atlassianClass);
    if (kind == null) return;
    //noinspection unchecked
    OptionsLoader<Object> loader = (OptionsLoader<Object>) kind.getExtension(OptionsLoader.CREATE_META);
    if (loader == null) return;
    List<JSONObject> options = JRField.ALLOWED_VALUES.list(field);
    Pair<OptionsLoader<Object>, Object> prevCall = myFieldOptions.get(fieldId);
    Object prev = prevCall != null ? prevCall.getSecond() : null;
    Object result = loader.loadOptions(prev, options);
    myFieldOptions.put(fieldId, Pair.create(loader, result));
  }

  public void postProcess(EntityTransaction transaction, boolean fullSet) {
    for (Map.Entry<String, Pair<OptionsLoader<Object>, Object>> entry : myFieldOptions.entrySet()) {
      String fieldId = entry.getKey();
      Pair<OptionsLoader<Object>, Object> loaderResult = entry.getValue();
      EntityHolder field = ServerCustomField.getField(transaction, fieldId);
      if (field == null) continue;
      loaderResult.getFirst().postProcess(field, loaderResult.getSecond(), fullSet);
    }
  }
}
