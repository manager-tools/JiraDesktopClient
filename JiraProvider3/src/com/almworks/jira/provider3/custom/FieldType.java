package com.almworks.jira.provider3.custom;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.entities.dbwrite.impl.CreateAttributeInfo;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.items.util.BadUtil;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class FieldType {
  private final String myType;
  private final List<TypedKey<?>> myKeys;

  protected FieldType(String type, List<TypedKey<?>> keys) {
    myType = type;
    myKeys = Collections15.unmodifiableListCopy(keys);
  }

  public FieldType(String type, TypedKey<?> ... keys) {
    myType = type;
    myKeys = Collections15.unmodifiableListCopy(keys);
  }

  public String getType() {
    return myType;
  }

  public List<TypedKey<?>> getKeys() {
    return myKeys;
  }

  @NotNull
  public abstract FieldKind createKind(Map<TypedKey<?>, ?> map) throws CreateProblem;

  @Nullable
  public static FieldType find(Collection<? extends FieldType> types, String name) {
    if (name == null || types == null) return null;
    for (FieldType type : types) if (name.equals(type.getType())) return type;
    return null;
  }

  protected static List<TypedKey<?>> unite(List<TypedKey<?>> list, TypedKey<?> ... keys) {
    ArrayList<TypedKey<?>> result = Collections15.arrayList();
    result.addAll(list);
    result.addAll(Arrays.asList(keys));
    return result;
  }

  protected static List<TypedKey<?>> unite(List<TypedKey<?>> list1, List<TypedKey<?>> list2, TypedKey<?> ... keys) {
    ArrayList<TypedKey<?>> result = Collections15.arrayList();
    result.addAll(list1);
    result.addAll(list2);
    result.addAll(Arrays.asList(keys));
    return result;
  }

  @NotNull
  public static Pair<String, String> getConnectionFieldIds(ItemVersion field) throws MigrationProblem {
    if (field == null) throw MigrationProblem.internalError();
    String id = field.getValue(CustomField.ID);
    if (id == null) throw new MigrationProblem("Missing field id: " + field.getValue(CustomField.NAME));
    Long connection = field.getValue(SyncAttributes.CONNECTION);
    if (connection == null || connection < 0) throw new MigrationProblem("Missing connection " + id);
    String connectionId = field.forItem(connection).getValue(SyncAttributes.CONNECTION_ID);
    if (connectionId == null) {
      LogHelper.error("Missing connection id", field.forItem(connection));
      throw MigrationProblem.internalError();
    }
    return Pair.create(connectionId, id);
  }

  public static class CreateProblem extends Exception {
    public CreateProblem(String message) {
      super(message);
    }

    @NotNull
    public static <T> T getFromMap(String key, Map<String, T> map, String failureMessage) throws CreateProblem {
      map = Util.NN(map, Collections.<String, T>emptyMap());
      T value = map.get(key);
      if (value == null) throw new CreateProblem(failureMessage + " '" + key + "'");
      return value;
    }

    public static <T> T getOptional(Map<TypedKey<?>, ?> idMap, TypedKey<String> idKey, Map<String, T> valueMap, String failureMessage) throws CreateProblem {
      String id = idKey.getFrom(idMap);
      return id == null ? null : getFromMap(id, valueMap, failureMessage);
    }
  }


  public static class MigrationProblem extends Exception {
    public MigrationProblem(String message) {
      super(message);
    }

    public static MigrationProblem internalError() {
      return new MigrationProblem("Internal error");
    }

    public MigrationProblem(Throwable cause) {
      super(cause.getMessage(), cause);
    }

    @NotNull
    public static DBAttribute<?> ensureCanMigrateAttribute(ItemVersionCreator field, CreateAttributeInfo info) throws MigrationProblem {
      if (info == null) {
        LogHelper.error("Null info", field);
        throw MigrationProblem.internalError();
      }
      DBReader reader = field.getReader();
      Long attrItem = field.getValue(CustomField.ATTRIBUTE);
      DBAttribute<?> prevAttribute = BadUtil.getAttribute(reader, attrItem);
      if (attrItem == null || prevAttribute == null || !SyncAttributes.isShadowable(reader, attrItem)) return info.createAttribute();
      if (info.ensureSameAttribute(prevAttribute, field.getReader())) return prevAttribute;
      LogHelper.error("Attribute is shadowable", prevAttribute, field, field.getValue(CustomField.NAME), field.getValue(CustomField.ID));
      throw MigrationProblem.internalError();
    }
  }
}
