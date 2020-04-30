package com.almworks.util.config;



import org.almworks.util.Collections15;

import java.util.*;

/**
 * @author dyoma
 */
class CachingMedium implements Medium {
  private final Medium myMedium;
  private final String myName;
  private final MySettings mySettings;
  private MySubsets mySubsets;
  private CachingMedium myParent;

  public CachingMedium(Medium medium) {
    this(medium, null);
  }

  private CachingMedium(Medium medium, CachingMedium parent) {
    assert medium != null;
    myMedium = medium;
    myParent = parent;
    myName = myMedium.getName();
    mySettings = new MySettings();
    mySubsets = new MySubsets();
  }

  public void setSettings(String settingName, List<String> values) {
    myMedium.setSettings(settingName, values);
    mySettings.updateSettings(settingName, values);
  }

  public Medium createSubset(String subsetName) {
    if (mySubsets == null)
      mySubsets = new MySubsets();
    Medium subset = myMedium.createSubset(subsetName);
    return mySubsets.addSubset(subsetName, subset);
  }

  public void removeMe() {
    myMedium.removeMe();
    if (myParent != null) {
      myParent.remove(this);
      assert myParent == null;
    }
  }

  public void clear() {
    myMedium.clear();
    mySettings.clear();
    if (mySubsets != null)
      mySubsets.clear();
  }

  public SubMedium<String> getSettings() {
    return mySettings;
  }

  public MySubsets getSubsets() {
    if (mySubsets == null)
      mySubsets = new MySubsets();
    return mySubsets;
  }

  public String getName() {
    return myName;
  }

  private void remove(CachingMedium child) {
    assert mySubsets != null;
    mySubsets.remove(child);
  }

  private static abstract class SubMediumCache<T> implements SubMedium<T> {
    protected final Map<String, List<T>> myCache = Collections15.hashMap();

    public boolean isSet(String name) {
      return name != null ? myCache.containsKey(name) : !myCache.isEmpty();
    }

    public T get(String name) {
      List<T> strings = myCache.get(name);
      return strings != null && !strings.isEmpty() ? strings.get(0) : null;
    }

    public List<T> getAll() {
      return getAll(null);
    }

    public List<T> getAll(String name) {
      if (name != null) {
        List<T> strings = myCache.get(name);
        return strings != null ? Collections.unmodifiableList(strings) : Collections15.<T>emptyList();
      }
      List<T> result = Collections15.arrayList();
      for (List<T> strings : myCache.values()) {
        result.addAll(strings);
      }
      return Collections.unmodifiableList(result);
    }

    public List<T> getAll(String name, List<T> buffer) {
      return getAll(name);
    }

    public Collection<String> getAllNames() {
      return Collections.unmodifiableCollection(myCache.keySet());
    }

    public void clear() {
      myCache.clear();
    }
  }

  private class MySettings extends SubMediumCache<String> {
    public MySettings() {
      Collection<String> allNames = myMedium.getSettings().getAllNames();
      for (Iterator<String> iterator = allNames.iterator(); iterator.hasNext();) {
        String name = iterator.next();
        update(name);
      }
    }

    public void update(String settingName) {
      SubMedium<String> settings = myMedium.getSettings();
      if (settings.isSet(settingName))
        myCache.put(settingName, Collections15.arrayList(settings.getAll(settingName)));
      else
        myCache.remove(settingName);
    }

    public void updateSettings(String settingName, List<String> values) {
      if (values != null && !values.isEmpty())
        myCache.put(settingName, Collections15.arrayList(values));
      else
        myCache.remove(settingName);
    }
  }

  private class MySubsets extends SubMediumCache<CachingMedium> {
    public MySubsets() {
      SubMedium<? extends Medium> subsets = myMedium.getSubsets();
      Collection<String> allNames = subsets.getAllNames();
      for (Iterator<String> iterator = allNames.iterator(); iterator.hasNext();) {
        String name = iterator.next();
        List<? extends Medium> all = subsets.getAll(name);
        List<CachingMedium> wrappedList = Collections15.arrayList(all.size());
        for (int i = 0; i < all.size(); i++) {
          Medium medium = all.get(i);
          CachingMedium wrapped = wrap(medium);
          assert name.equals(wrapped.getName());
          wrappedList.add(wrapped);
        }
        myCache.put(name, wrappedList);
      }
    }

    public CachingMedium get(String name) {
      CachingMedium result = super.get(name);
      assert result.myParent == CachingMedium.this : result.myParent;
      return result;
    }

    public List<CachingMedium> getAll(String name) {
      List<CachingMedium> result = super.getAll(name);
      for (int i = 0; i < result.size(); i++) {
        CachingMedium medium = result.get(i);
        assert medium.myParent == CachingMedium.this : medium.myParent;
      }
      return result;
    }

    private CachingMedium wrap(Medium medium) {
      return new CachingMedium(medium, CachingMedium.this);
    }

    public CachingMedium addSubset(String name, Medium subset) {
      List<CachingMedium> list = myCache.get(name);
      if (list == null) {
        list = Collections15.arrayList(4);
        myCache.put(name, list);
      }
      CachingMedium wrapped = wrap(subset);
      list.add(wrapped);
      return wrapped;
    }

    public void remove(CachingMedium child) {
      assert child != null;
      String name = child.getName();
      List<CachingMedium> list = myCache.get(name);
      int index = list.indexOf(child);
      assert index != -1 : child;
      list.remove(index);
      if (list.isEmpty())
        myCache.remove(name);
      child.myParent = null;
    }

    public void clear() {
      for (Iterator<List<CachingMedium>> iterator = myCache.values().iterator(); iterator.hasNext();) {
        List<CachingMedium> list = iterator.next();
        for (int i = 0; i < list.size(); i++) {
          CachingMedium medium = list.get(i);
          medium.myParent = null;
        }
      }
      super.clear();
    }
  }
}
