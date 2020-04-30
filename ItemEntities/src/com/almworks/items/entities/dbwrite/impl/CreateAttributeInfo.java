package com.almworks.items.entities.dbwrite.impl;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.util.EntityKeyProperties;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;

public class CreateAttributeInfo {
  private final String myId;
  private final String myName;
  private final Class<?> myScalarClass;
  private final DBAttribute.ScalarComposition myComposition;
  private final boolean myPropagatingChange;
  private final AttributeMap myAttributeMap;

  private CreateAttributeInfo(String id, String name, Class<?> scalarClass, DBAttribute.ScalarComposition composition, boolean propagatingChange, AttributeMap attributeMap) {
    myId = id;
    myName = name;
    myScalarClass = scalarClass;
    myComposition = composition;
    myPropagatingChange = propagatingChange;
    myAttributeMap = attributeMap;
  }

  public static CreateAttributeInfo create(Entity key, String id, String name) {
    if (!Entity.isKeyType(key.getType())) return null;
    KeyDataKind<?, ?> behaviour = DBObjectsCache.chooseDataKind(key);
    if (behaviour == null) return null;
    Builder builder = new Builder(id, name);
    if (!behaviour.createAttribute(builder)) return null;
    if (EntityKeyProperties.isShadowable(key)) builder.setValue(SyncAttributes.SHADOWABLE, true);
    return new CreateAttributeInfo(builder.getId(), builder.getName(), builder.getScalarClass(), builder.getComposition(), builder.isPropagatingChange(), builder.copyAdditional());
  }

  public DBAttribute<?> createAttribute() {
    DBAttribute<?> attribute = DBAttribute.create(myId, myName, myScalarClass, myComposition, myPropagatingChange);
    for (DBAttribute<?> attr : myAttributeMap.keySet()) init(attribute, attr);
    return attribute;
  }

  private <T> void init(DBAttribute<?> attribute, DBAttribute<T> additional) {
    T v = myAttributeMap.get(additional);
    if (v != null) attribute.initialize(additional, v);
  }

  /**
   * Checks that this instance describes the same <b>existing</b> attribute
   * @param attribute the attribute to be checked. It must be materialized in DB.
   * @param reader used to access additional properties of the attribute
   * @return true if the attribute is the same. false if a difference found (and logged), or if the attribute is not materialized
   */
  @SuppressWarnings("NullableProblems")
  public boolean ensureSameAttribute(DBAttribute<?> attribute, DBReader reader) {
    if (!Util.equals(myId, attribute.getId()) || !Util.equals(myName, attribute.getName()) ||
      !Util.equals(myScalarClass, attribute.getScalarClass()) || !Util.equals(myComposition, attribute.getComposition()) || myPropagatingChange != attribute.isPropagatingChange()) {
      LogHelper.error("Expected same attribute: [", myId, myName, myScalarClass, myComposition, myPropagatingChange, "]", attribute);
      return false;
    }
    long item = reader.findMaterialized(attribute);
    if (item <= 0) {
      LogHelper.error("Attribute is not materialized", attribute);
      return false;
    }
    AttributeMap map = reader.getAttributeMap(item);
    map.put(DBAttribute.ID, null);
    map.put(DBAttribute.NAME, null);
    map.put(DBAttribute.SCALAR_CLASS, null);
    map.put(DBAttribute.SCALAR_COMPOSITION, null);
    map.put(DBAttribute.PROPAGATING_CHANGE, null);
    map.put(DBAttribute.TYPE, null);
    map.put(SyncAttributes.EXISTING, null);
    if (myAttributeMap.size() != map.size()) {
      LogHelper.error("Different init values", attribute, map, myAttributeMap);
      return false;
    }
    for (DBAttribute<?> attr : map.keySet()) {
      Object loaded = map.get(attr);
      Object own = myAttributeMap.get(attr);
      if (!Util.equals(loaded, own)) {
        LogHelper.error("Not equal init values", attribute, attr, own, loaded, map, myAttributeMap);
        return false;
      }
    }
    return true;
  }

  public static class Builder {
    private final String myId;
    private final String myName;
    private Class<?> myScalarClass;
    private DBAttribute.ScalarComposition myComposition;
    private boolean myPropagatingChange;
    private final AttributeMap myAdditional = new AttributeMap();

    public Builder(String id, String name) {
      myId = id;
      myName = name;
    }

    public String getId() {
      return myId;
    }

    public String getName() {
      return myName;
    }

    public Class<?> getScalarClass() {
      return myScalarClass;
    }

    public DBAttribute.ScalarComposition getComposition() {
      return myComposition;
    }

    public boolean isPropagatingChange() {
      return myPropagatingChange;
    }

    public AttributeMap copyAdditional() {
      return myAdditional.copy();
    }

    public void setProperties(Class<?> scalarClass, DBAttribute.ScalarComposition composition, boolean propagatingChange) {
      myScalarClass = scalarClass;
      myComposition = composition;
      myPropagatingChange = propagatingChange;
    }

    public <T> void setValue(DBAttribute<T> attr, T value) {
      if (value == null) {
        LogHelper.error("Null value does not make sense", attr, myId, myName);
        return;
      }
      if (myAdditional.containsKey(attr)) {
        LogHelper.error("Attempt to override", attr, myAdditional.get(attr), value, myId, myName);
        return;
      }
      myAdditional.put(attr, value);
    }
  }
}
