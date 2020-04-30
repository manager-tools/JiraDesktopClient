package com.almworks.jira.provider3.gui.edit.fields;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.jira.provider3.sync.download2.rest.JRField;
import com.almworks.jira.provider3.sync.schema.ServerIssueType;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class JiraFieldsInfo implements Procedure<EntityWriter> {
  private static final TypedKey<JiraFieldsInfo> KEY = TypedKey.create("jiraFieldsInfo");
  private static final int VERSION = 1;
  private static final DBAttribute<byte[]> FIELDS_INFO = ServerJira.toScalarAttribute(EntityKey.bytes("connection.field.info", null));

  private final Map<Pair<Integer, Integer>, Map<String, Pair<String, Boolean>>> myInfo = Collections15.hashMap();

  private JiraFieldsInfo() {
  }

  @NotNull
  public static JiraFieldsInfo getInstance(EntityTransaction transaction) {
    UserDataHolder userData = transaction.getUserData();
    JiraFieldsInfo info = userData.getUserData(KEY);
    if (info == null) {
      info = new JiraFieldsInfo();
      if (userData.putIfAbsent(KEY, info)) transaction.addPostWriteProcedure(info);
      else {
        info = userData.getUserData(KEY);
        if (info == null) throw new RuntimeException();
      }
    }
    return info;
  }

  public void addField(int project, int type, String fieldId, String fieldName, boolean mandatory) {
    Pair<Integer, Integer> scope = Pair.create(project, type);
    Map<String, Pair<String, Boolean>> fields = myInfo.get(scope);
    if (fields == null) {
      fields = Collections15.hashMap();
      myInfo.put(scope, fields);
    }
    Pair<String, Boolean> fieldInfo = fields.get(fieldId);
    if (fieldInfo == null) fields.put(fieldId, Pair.create(fieldName, mandatory));
    else {
      if (!Util.equals(fieldInfo.getFirst(), fieldName) || !Util.equals(fieldInfo.getSecond(), mandatory))
        LogHelper.error("Field info redefined", scope, fieldInfo, fieldId, fieldName, mandatory);
    }
  }

  public void processField(int projectId, int typeId, String fieldId, JSONObject field) {
    Boolean required = JRField.REQUIRED.getValue(field);
    String name = JRField.NAME.getValue(field);
    if (required == null || name == null) LogHelper.warning("Missing name/required", field);
    else addField(projectId, typeId, fieldId, name, required);
  }

  @Override
  public void invoke(EntityWriter writer) {
    Map<Integer,EntityHolder> projects = EntityUtils.collectById(writer.getTransaction(), ServerProject.TYPE, ServerProject.ID);
    Map<Integer,EntityHolder> types = EntityUtils.collectById(writer.getTransaction(), ServerIssueType.TYPE, ServerIssueType.ID);
    Map<String, ResolvedField> fields = ResolvedField.loadAllById(writer.getConnection());
    HashMap<Pair<Long, Long>, FieldInfoSet> current = loadCurrent(writer.getConnection());
    ByteArray dbFields = new ByteArray();
    dbFields.addInt(VERSION);
    for (Map.Entry<Pair<Integer, Integer>, Map<String, Pair<String, Boolean>>> entry : myInfo.entrySet()) {
      long project = writer.getItem(projects.get(entry.getKey().getFirst()));
      long type = writer.getItem(types.get(entry.getKey().getSecond()));
      if (project <= 0 || type <= 0) {
        LogHelper.error("Missing project/type", project, type, entry);
        continue;
      }
      current.remove(Pair.create(project, type));
      Map<ResolvedField, Pair<String, Boolean>> resolvedFields = Collections15.hashMap();
      for (Map.Entry<String, Pair<String, Boolean>> fieldEntry : entry.getValue().entrySet()) {
        ResolvedField resolvedField = fields.get(fieldEntry.getKey());
        if (resolvedField == null) continue;
        resolvedFields.put(resolvedField, fieldEntry.getValue());
      }
      dbFields.addLong(project);
      dbFields.addLong(type);
      dbFields.addInt(resolvedFields.size());
      for (Map.Entry<ResolvedField, Pair<String, Boolean>> fieldEntry : resolvedFields.entrySet()) {
        dbFields.addLong(fieldEntry.getKey().getItem());
        Pair<String, Boolean> info = fieldEntry.getValue();
        dbFields.addUTF8(info.getFirst());
        dbFields.addBoolean(info.getSecond());
      }
    }
    for (Map.Entry<Pair<Long, Long>, FieldInfoSet> entry : current.entrySet()) {
      Pair<Long, Long> key = entry.getKey();
      dbFields.addLong(key.getFirst());
      dbFields.addLong(key.getSecond());
      FieldInfoSet infos = entry.getValue();
      infos.storeTo(dbFields);
    }
    writer.getDrain().changeItem(writer.getConnectionItem()).setValue(FIELDS_INFO, dbFields.toNativeArray());
  }

  @NotNull
  static HashMap<Pair<Long, Long>, FieldInfoSet> loadCurrent(ItemVersion connection) {
    HashMap<Pair<Long,Long>, FieldInfoSet> result = Collections15.hashMap();
    byte[] info = connection.getValue(FIELDS_INFO);
    if (info == null || info.length == 0) return result;
    ByteArray.Stream stream = new ByteArray.Stream(info);
    if (VERSION != stream.nextInt()) return result;
    while (!stream.isAtEnd()) {
      long project = stream.nextLong();
      long type = stream.nextLong();
      FieldInfoSet fields = FieldInfoSet.load(connection, stream);
      if (fields == null) return result;
      result.put(Pair.create(project, type), fields);
    }
    if (!stream.isSuccessfullyAtEnd()) {
      LogHelper.error("Failed to field info", stream);
      return Collections15.hashMap();
    }
    return result;
  }
}
