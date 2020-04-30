package com.almworks.jira.provider3.gui;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.ExportContext;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.schema.export.ExportPolicy;
import com.almworks.items.gui.meta.schema.export.ModelKeyValueExport;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.services.IssueUrl;
import com.almworks.util.Pair;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

class IssueKeyExportPolicy implements ExportPolicy {
  private static final Object NO_KEY = "noKey";
  public static final ExportPolicy INSTANCE = new IssueKeyExportPolicy();

  private IssueKeyExportPolicy() {
  }

  @Override
  public Pair<String, ExportValueType> export(PropertyMap values, ExportContext context, GuiFeaturesManager features) {
    ModelKey<String> key = ModelKeyValueExport.getModelKey(context, features, TypedKey.create("issueKeyKey"), MetaSchema.KEY_KEY);
    if (key == null) return null;
    String value = key.hasValue(values) ? Util.NN(key.getValue(values)).trim() : "";
    String url = null;
    if (context.isHtmlAccepted() && value.length() > 0) {
      LoadedItemServices lis = LoadedItemServices.VALUE_KEY.getValue(values);
      if (lis != null) {
        JiraConnection3 conn = lis.getConnection(JiraConnection3.class);
        if (conn != null) {
          String baseUrl = conn.getConfigHolder().getBaseUrl();
          if (baseUrl != null) url = IssueUrl.getIssueUrl(baseUrl, value);
        }
      }
    }

    ExportValueType type = ExportValueType.STRING;
    if (url != null) {
      value = "<a href=\"" + url + "\">" + value + "</a>";
      type = ExportValueType.STRING_HTML;
    }

    return Pair.create(value, type);
  }
}
