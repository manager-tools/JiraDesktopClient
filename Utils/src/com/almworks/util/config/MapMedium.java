package com.almworks.util.config;

import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author : Dyoma
 */
public class MapMedium implements Medium {
  private MapMedium myParent;
  private final MapSubMedium<String> mySettings;
  private final MapSubMedium<MapMedium> mySubsets;
  private final String myName;

  MapMedium(MapMedium parent, String name) {
    myParent = parent;
//    myName = MediumOptimization.optimize(name);
    myName = name == null ? null : name.intern();
    mySettings = MapSubMedium.create();
    mySubsets = MapSubMedium.create();
  }

  public static Configuration createConfig() {
    return Configuration.createWritable(new MapMedium(null, null));
  }

  public static Configuration createConfig(MediumWatcher notificator) {
    return Configuration.createWritable(new MapMedium(null, null), notificator);
  }

  public static Configuration createConfig(MediumWatcher notificator, String name) {
    return Configuration.createWritable(new MapMedium(null, name), notificator);
  }

  public void setSettings(String settingName, List<String> values) {
    Map<String, ?> settings = mySettings.myMap;
    if (values.isEmpty()) {
      if (settings != null)
        settings.remove(settingName);
    } else {
      if (settings == null) {
        settings = createMap();
        mySettings.myMap = settings;
      }
      int count = values.size();
      assert count > 0 : count + " " + this;
//      String key = MediumOptimization.optimize(settingName);
      settingName = settingName == null ? null : settingName.intern();
      if (count == 1) {
        ((Map) settings).put(settingName, values.get(0).intern());
      } else {
        List<String> list = Collections15.arrayList(count);
        for (String value : values) {
          list.add(value.intern());
        }
        ((Map) settings).put(settingName, list);
      }
    }
  }

  private static Map<String, ?> createMap() {
    return Collections15.linkedHashMap();
  }

  public Medium createSubset(String subsetName) {
    Map<String, ?> subsetsMap = mySubsets.myMap;
    if (subsetsMap == null) {
      subsetsMap = createMap();
      mySubsets.myMap = subsetsMap;
    }
//    subsetName = MediumOptimization.optimize(subsetName);
    subsetName = subsetName == null ? null : subsetName.intern();
    MapMedium newMedium = new MapMedium(this, subsetName);
    Object obj = subsetsMap.get(subsetName);
    if (obj == null) {
      ((Map) subsetsMap).put(subsetName, newMedium);
    } else if (obj instanceof List) {
      ((List) obj).add(newMedium);
    } else {
      assert obj instanceof MapMedium : obj + " " + this;
      List<MapMedium> subsets = Collections15.arrayList();
      subsets.add((MapMedium) obj);
      subsets.add(newMedium);
      ((Map) subsetsMap).put(subsetName, subsets);
    }
    return newMedium;
  }

  public void removeMe() {
    if (myParent == null)
      return;
    Map<String, ?> subsetsMap = myParent.mySubsets.myMap;
    assert subsetsMap != null : this;
    if (subsetsMap == null)
      return;
    Object obj = subsetsMap.get(getName());
    if (obj == null) {
//      assert false : this;
      Log.warn(this + ": configuration inconsistency at removal");
      return;
    }
    if (obj instanceof List) {
      List list = ((List) obj);
      // guard against emptyList
      if (list.size() > 0)
        list.remove(this);
      if (list.isEmpty())
        subsetsMap.remove(getName());
    } else {
      assert this == obj : this;
      if (this == obj)
        subsetsMap.remove(getName());
    }
  }

  public void clear() {
    Map<String, ?> settings = mySettings.myMap;
    if (settings != null)
      settings.clear();
    Map<String, ?> subsets = mySubsets.myMap;
    if (subsets != null) {
      for (Object obj : subsets.values()) {
        if (obj instanceof MapMedium) {
          ((MapMedium) obj).myParent = null;
        } else if (obj instanceof List) {
          for (MapMedium m : ((List<MapMedium>) obj)) {
            m.myParent = null;
          }
        } else {
          assert false : obj + " " + this;
        }
      }
      subsets.clear();
    }
  }

  public SubMedium<String> getSettings() {
    return mySettings;
  }

  public SubMedium<? extends Medium> getSubsets() {
    return mySubsets;
  }

  public String getName() {
    return myName;
  }

  private static class MapSubMedium<T> implements SubMedium<T> {
    private Map<String, ?> myMap;

    public static <T> MapSubMedium<T> create() {
      return new MapSubMedium<T>();
    }

    public boolean isSet(String name) {
      if (myMap == null)
        return false;
      return name != null ? myMap.containsKey(name) : !myMap.isEmpty();
    }

    public T get(String name) {
      if (myMap == null)
        return null;
      Object obj = myMap.get(name);
      if (obj instanceof List)
        return (T) ((List) obj).get(0);
      else
        return (T) obj;
    }

    public List<T> getAll() {
      return getAll(null, null);
    }

    public List<T> getAll(String name) {
      return getAll(name, null);
    }

    public List<T> getAll(String name, List<T> buffer) {
      if (buffer != null)
        buffer.clear();
      if (myMap != null) {
        if (name != null) {
          Object obj = myMap.get(name);
          if (obj instanceof List) {
            if (buffer == null) {
              buffer = Collections15.unmodifiableListCopy((List<? extends T>) obj);
            } else {
              buffer.addAll((List<? extends T>) obj);
            }
          } else if (obj != null) {
            if (buffer == null) {
              return Collections.singletonList((T) obj);
            } else {
              buffer.add((T) obj);
            }
          }
        } else {
          for (Map.Entry<String, ?> entry : myMap.entrySet()) {
            if (buffer == null)
              buffer = Collections15.arrayList();
            Object obj = entry.getValue();
            if (obj instanceof List) {
              buffer.addAll((List<T>) obj);
            } else if (obj != null) {
              buffer.add((T) obj);
            }
          }
        }
      }
      return buffer == null ? Collections15.<T>emptyList() : buffer;
    }

    public Collection<String> getAllNames() {
      if (myMap == null)
        return Collections15.emptyList();
      else
        return Collections.unmodifiableCollection(myMap.keySet());
    }
  }

  public String toString() {
    StringBuilder r = new StringBuilder();
    makeString(r);
    return r.toString();
  }

  private void makeString(StringBuilder r) {
    MapMedium parent = myParent;
    if (parent != null) {
      myParent.makeString(r);
      r.append('.');
    }
    r.append(myName);
  }
}
