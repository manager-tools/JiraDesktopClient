package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.download2.rest.JRGeneric;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public class CommonEnumOptions implements OptionsLoader<Map<String, CommonEnumOptions.Option>> {
  public static final CommonEnumOptions ORDERED = new CommonEnumOptions(true, boolAlways(true));
  public static final CommonEnumOptions ORDERED_NO_REMOVE = new CommonEnumOptions(true, boolAlways(false));
  public static final CommonEnumOptions UNORDERED = new CommonEnumOptions(false, boolAlways(true));
  public static final CommonEnumOptions UNORDERED_NO_REMOVE = new CommonEnumOptions(false, boolAlways(false));

  private final boolean myOrdered;
  private final BooleanSupplier myRemoveOthers;

  private CommonEnumOptions(boolean ordered, BooleanSupplier removeOthers) {
    myOrdered = ordered;
    myRemoveOthers = removeOthers;
  }

  public static CommonEnumOptions removeByProperty(boolean ordered, String removeProperty, boolean propertyDefault) {
    return new CommonEnumOptions(ordered, () -> Env.getBoolean(removeProperty, propertyDefault));
  }

  public boolean isOrdered() {
    return myOrdered;
  }

  private static BooleanSupplier boolAlways(boolean value) {
    return () -> value;
  }

  @Override
  public Map<String, CommonEnumOptions.Option> loadOptions(@Nullable Map<String, Option> prevResult,
    List<JSONObject> options)
  {
    if (prevResult == null) prevResult = Collections15.hashMap();
    for (JSONObject option : options) {
      String id = JRGeneric.ID_STR.getValue(option);
      String name = JRGeneric.VALUE.getValue(option);
      if (id == null || name == null) {
        LogHelper.error("Missing enum option data", id, name);
        continue;
      }
      if (prevResult.containsKey(id)) continue;
      prevResult.put(id, new Option(id, name, prevResult.size()));
    }
    return prevResult;
  }

  @Override
  public void postProcess(EntityHolder field, @Nullable Map<String, Option> loadResult, boolean fullSet) {
    Entity type = ServerCustomField.createEnumType(field);
    if (type == null || loadResult == null) return;
    EntityTransaction transaction = field.getTransaction();
    EntityBag2.Optional allOptions = myRemoveOthers.getAsBoolean() && fullSet ? transaction.addBag(type).delete().toOptional() : EntityBag2.Optional.MOCK;
    for (Option option : loadResult.values()) {
      EntityHolder holder = transaction.addEntity(type, ServerCustomField.ENUM_STRING_ID, option.myId);
      if (holder == null) continue;
      holder.setNNValue(ServerCustomField.ENUM_DISPLAY_NAME, option.myName);
      if (myOrdered) holder.setValue(ServerCustomField.ENUM_ORDER, option.myOrder);
      allOptions.exclude(holder);
    }
  }

  static class Option {
    private final String myId;
    private final String myName;
    private final int myOrder;

    Option(String id, String name, int order) {
      myId = id;
      myName = name;
      myOrder = order;
    }
  }
}
