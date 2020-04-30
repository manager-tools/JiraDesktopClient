package com.almworks.jira.provider3.gui.edit.fields;

import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.ByteArray;
import com.almworks.util.text.NameMnemonic;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class FieldInfoSet {
  public static final FieldInfoSet EMPTY = new FieldInfoSet(Collections.<ResolvedField, Pair<NameMnemonic, Boolean>>emptyMap());

  private final Map<ResolvedField, Pair<NameMnemonic, Boolean>> myFields;

  FieldInfoSet(Map<ResolvedField, Pair<NameMnemonic, Boolean>> fields) {
    myFields = fields;
  }

  public static FieldInfoSet merge(Collection<FieldInfoSet> fields) {
    FieldInfoSetBuilder result = new FieldInfoSetBuilder();
    for (FieldInfoSet info : fields) result.merge(info, false);
    return result.build();
  }

  void storeTo(ByteArray array) {
    array.addInt(myFields.size());
    for (Map.Entry<ResolvedField, Pair<NameMnemonic, Boolean>> infoEntry : myFields.entrySet()) {
      array.addLong(infoEntry.getKey().getItem());
      Pair<NameMnemonic, Boolean> fieldInfo = infoEntry.getValue();
      array.addUTF8(fieldInfo.getFirst().getTextWithMnemonic());
      array.addBoolean(fieldInfo.getSecond());
    }
  }

  @Nullable("When error occurred")
  static FieldInfoSet load(ItemVersion connection, ByteArray.Stream stream) {
    int fieldCount = stream.nextInt();
    FieldInfoSetBuilder fields = new FieldInfoSetBuilder();
    for (int j = 0; j < fieldCount; j++) {
      if (stream.isErrorOccurred()) {
        LogHelper.error("Failed to load field info", stream);
        return null;
      }
      ResolvedField field = ResolvedField.load(connection.forItem(stream.nextLong()));
      if (field == null) continue;
      NameMnemonic fieldName = NameMnemonic.rawText(stream.nextUTF8());
      boolean mandatory = stream.nextBoolean();
      fields.add(field, fieldName, mandatory);
    }
    return fields.build();
  }

  Map<ResolvedField, Pair<NameMnemonic, Boolean>> getInfos() {
    return myFields;
  }

  public Pair<NameMnemonic, Boolean> getInfo(ResolvedField field) {
    return myFields.get(field);
  }

  @Nullable
  public Pair<NameMnemonic, Boolean> getInfo(String fieldId) {
    for (Map.Entry<ResolvedField, Pair<NameMnemonic, Boolean>> entry : myFields.entrySet()) if (entry.getKey().getJiraId().equals(fieldId)) return entry.getValue();
    return null;
  }

  public boolean isSurelyMandatory(String fieldId) {
    Pair<NameMnemonic, Boolean> info = getInfo(fieldId);
    return info != null && info.getSecond();
  }

  public Set<ResolvedField> getFields() {
    return myFields.keySet();
  }
}
