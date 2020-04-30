package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.sax.*;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure2;
import org.almworks.util.Util;
import org.json.simple.JSONObject;

abstract class CreateMetaFields {
  public LocationHandler createMetaHandler() {
    final JSONCollector projectId = new JSONCollector(null);
    final JSONCollector issueTypeId = new JSONCollector(null);
    LocationHandler types = PeekArrayElement.entryArray("issuetypes",
      new CompositeHandler(issueTypeId.peekObjectEntry("id"), issueTypeId.resetOnStartTop(), new PeekEntryValue(new Procedure2<String, Object>() {
        @Override
        public void invoke(String name, Object f) {
          JSONObject field = Util.castNullable(JSONObject.class, f);
          if (field == null) {
            LogHelper.error("Expected field object", f);
            return;
          }
          Integer prjId = JSONKey.INTEGER.convert(projectId.getObject());
          Integer typeId = JSONKey.INTEGER.convert(issueTypeId.getObject());
          if (prjId == null || typeId == null) {
            LogHelper.error("Missing data", prjId, typeId);
            return;
          }
          addField(prjId, typeId, name, field);
        }
      }).entryObject("fields")));
    return PeekArrayElement.entryArray("projects", new CompositeHandler(projectId.peekObjectEntry("id"), projectId.resetOnStartTop(), types));
  }

  protected abstract void addField(int projectId, int typeId, String id, JSONObject field);
}
