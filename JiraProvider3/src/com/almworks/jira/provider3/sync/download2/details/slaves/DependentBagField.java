package com.almworks.jira.provider3.sync.download2.details.slaves;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.JsonIssueField;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Adapts {@link SlaveLoader} to JsonIssueField interface.<br>
 * Loads array of dependent issues then writes it into transaction and supply a bag of all loaded entities.
 * @param <B> type of bag holder (single bag or a kind of a collection of bags)
 */
public class DependentBagField<B> implements JsonIssueField {
  private final SlaveLoader<B> mySlave;
  private final boolean myNullAsEmpty;

  private DependentBagField(SlaveLoader<B> slave, boolean nullAsEmpty) {
    mySlave = slave;
    myNullAsEmpty = nullAsEmpty;
  }

  public static <B> JsonIssueField create(SlaveLoader<B> slave, boolean nullAsEmpty) {
    return new DependentBagField<B>(slave, nullAsEmpty);
  }

  @Override
  public Collection<? extends ParsedValue> loadValue(@Nullable Object jsonValue) {
    final List<SlaveLoader.Parsed<B>> fullBag;
    if (jsonValue == null) fullBag = Collections.emptyList();
    else {
      JSONArray array = Util.castNullable(JSONArray.class, jsonValue);
      if (array == null) {
        LogHelper.error("Expected array", jsonValue.getClass());
        return null;
      }
      fullBag = loadEntities(array, mySlave);
    }
    return createBagValueCollection(fullBag, mySlave);
  }

  public static <B> List<SlaveLoader.Parsed<B>> loadEntities(JSONArray array, SlaveLoader<B> slaveLoader) {
    List<SlaveLoader.Parsed<B>> fullBag;
    fullBag = Collections15.arrayList();
    for (int i = 0; i < array.size(); i++) {
      Object element = array.get(i);
      Collection<? extends SlaveLoader.Parsed<B>> values = slaveLoader.loadValue(element, i);
      if (values != null)
        fullBag.addAll(values);
    }
    return fullBag;
  }

  @Override
  public Collection<? extends ParsedValue> loadNull() {
    if (myNullAsEmpty) return createBagValueCollection(Collections15.<SlaveLoader.Parsed<B>>emptyList(), mySlave);
    else {
      LogHelper.error("Unexpected null", mySlave);
      return null;
    }
  }

  public static <B> Collection<? extends ParsedValue> createBagValueCollection(List<SlaveLoader.Parsed<B>> fullBag, SlaveLoader<B> slave) {
    return Collections.singleton(createBagValue(fullBag, slave));
  }

  public static <B> ParsedValue createBagValue(List<SlaveLoader.Parsed<B>> fullBag, SlaveLoader<B> slave) {
    return new MyParsedValue<B>(fullBag, slave);
  }

  private static class MyParsedValue<B> implements ParsedValue {
    private final List<SlaveLoader.Parsed<B>> myFullBag;
    private final SlaveLoader<B> mySlave;

    public MyParsedValue(List<SlaveLoader.Parsed<B>> fullBag, SlaveLoader<B> slave) {
      myFullBag = fullBag;
      mySlave = slave;
    }

    @Override
    public boolean addTo(EntityHolder master) {
      B bags = mySlave.createBags(master);
      for (SlaveLoader.Parsed<B> slave : myFullBag) {
        slave.addTo(master, bags);
      }
      return true;
    }

    @Override
    public String toString() {
      return mySlave + " <- " + myFullBag;
    }
  }
}
