package com.almworks.util.properties;

import com.almworks.util.collections.BooleanModel;

public class BooleanPropertyKey extends PropertyKey<BooleanModel, Boolean> {
  private final boolean myInitialValue;

  public BooleanPropertyKey(String name, boolean initialValue) {
    super(name);
    myInitialValue = initialValue;
  }

  public Boolean getModelValue(PropertyModelMap properties) {
    BooleanModel model = properties.get(this);
    assert model != null : this;
    return model.getBooleanValue();
  }

  public void installModel(ChangeSupport changeSupport, PropertyModelMap propertyMap) {
    BooleanModel model = new BooleanModel(myInitialValue);
    propertyMap.put(this, model);
    changeSupport.fireChangedOn(this, model);
  }

  public ChangeState isChanged(PropertyModelMap models, PropertyMap originalValues) {
    return ChangeState.choose(this, originalValues, getModelValue(models));
  }

  public void setInitialValue(PropertyMap values, Boolean value) {
    values.put(getValueKey(), value);
  }

  public void setModelValue(PropertyModelMap properties, Boolean value) {
    BooleanModel model = properties.get(this);
    assert model != null : this;
    model.setValue(value);
  }

  public static BooleanPropertyKey createKey(String name, boolean initialValue) {
    return new BooleanPropertyKey(name, initialValue);
  }
}
