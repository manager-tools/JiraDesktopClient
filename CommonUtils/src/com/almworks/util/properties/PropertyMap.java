package com.almworks.util.properties;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.exec.ThreadGate;
import gnu.trove.THashMap;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Dyoma
 */
public class PropertyMap implements Modifiable {
  private final ChangeSupport myChangeSupport;
  private Map<TypedKey<?>, Object> myValues;
  private TypedKey<?> mySingleKey;
  private Object mySingleObject;
  private PropertyMapValueDecorator myDecorator;

  public PropertyMap() {
    this(null);
  }

  public PropertyMap(Object sourceBean) {
    this(sourceBean, null);
  }


  public PropertyMap(Object sourceBean, PropertyMap copyFrom) {
    this(sourceBean, copyFrom, null);
  }

  public PropertyMap(PropertyMap copyFrom) {
    this(null, copyFrom);
  }

  public PropertyMap(Object sourceBean, PropertyMap copyFrom, Map<TypedKey<?>, Object> mapImpl) {
    myChangeSupport = ChangeSupport.create(sourceBean);
    myValues = mapImpl;
    if (copyFrom != null) {
      Set<? extends TypedKey> set = copyFrom.keySet();
      if (myValues == null && set.size() == 1) {
        mySingleKey = set.iterator().next();
        mySingleObject = copyFrom.getInternal(mySingleKey);
      } else {
        if (myValues == null)
          myValues = createMap();
        else
          myValues.clear();
        for (TypedKey key : set) {
          myValues.put(key, copyFrom.getInternal(key));
        }
      }
      myDecorator = copyFrom.myDecorator;
    }
  }

  public PropertyMapValueDecorator getDecorator() {
    return myDecorator;
  }

  public void setDecorator(PropertyMapValueDecorator decorator) {
    myDecorator = decorator;
  }

  public <T> T put(TypedKey<T> key, T value) {
    T oldValue = getInternal(key);
    if (!Util.equals(oldValue, value)) {
      doPut(key, value);
      myChangeSupport.fireChanged(key, decorate(key, oldValue), decorate(key, value));
    }
    return oldValue;
  }

  public <T> T replace(TypedKey<T> key, T value) {
    T oldValue = getInternal(key);
    if(oldValue != value) {
      doPut(key, value);
      myChangeSupport.fireChanged(key, decorate(key, oldValue), decorate(key, value));
    }
    return oldValue;
  }

  private <T> T doPut(TypedKey<T> key, T value) {
    Object oldValue = null;
    if (key.equals(mySingleKey)) {
      oldValue = mySingleObject;
      mySingleObject = value;
    } else if (myValues != null) {
      oldValue = key.putTo(myValues, value);
    } else if (mySingleKey == null) {
      mySingleKey = key;
      mySingleObject = value;
    } else {
      myValues = createMap();
      key.putTo(myValues, value);
    }
    return key.cast(oldValue);
  }

  private static Map<TypedKey<?>, Object> createMap() {
    return new THashMap(2);
  }

  @Nullable
  private <T> T getInternal(TypedKey<T> key) {
    if (key.equals(mySingleKey))
      return (T) mySingleObject;
    else if (myValues != null)
      return key.getFrom(myValues);
    else
      return null;
  }

  @Nullable
  public <T> T get(TypedKey<T> key) {
    assert key != null;
    T v = getInternal(key);
    return decorate(key, v);
  }

  public <T> void initValue(TypedKey<T> key, T value) {
    if (containsKey(key))
      throw new RuntimeException(key.getName());
    doPut(key, value);
  }

  public <T> Detach addPropertyChangeListener(TypedKey<? extends T> key,
    final PropertyChangeListener<? super T> listener)
  {
    return myChangeSupport.addListener(key, listener);
  }

  protected ChangeSupport getChangeSupport() {
    return myChangeSupport;
  }

  public boolean containsKey(TypedKey<?> key) {
    return key != null && (key.equals(mySingleKey) || (myValues != null && myValues.containsKey(key)));
  }

  public static <T> PropertyMap create(TypedKey<T> key, T value) {
    PropertyMap map = new PropertyMap();
    map.put(key, value);
    return map;
  }

  public void addChangeListener(Lifespan life, final ChangeListener listener) {
    life.add(myChangeSupport.addListener(listener));
  }

  public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    life.add(myChangeSupport.addListener(listener, gate));
  }

  public Detach addAWTChangeListener(ChangeListener listener) {
    return myChangeSupport.addListener(listener, ThreadGate.AWT);
  }

  public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    life.add(addAWTChangeListener(listener));
  }

  public void fireChanged() {
    myChangeSupport.fireChanged();
  }

  public <T> T remove(TypedKey<T> key) {
    if (key != null && key.equals(mySingleKey)) {
      Object v = mySingleObject;
      mySingleKey = null;
      mySingleObject = null;
      return (T) v;
    } else if (myValues != null) {
      return (T) myValues.remove(key);
    } else {
      return null;
    }
  }

  public void addPropertyChangeListener(PropertyChangeListener.Any listener) {
    myChangeSupport.addListener(listener);
  }

  public Set<? extends TypedKey> keySet() {
    final TypedKey<?> singleKey = mySingleKey;
    final Set<TypedKey<?>> set = myValues == null ? null : myValues.keySet();
    return new AbstractSet<TypedKey>() {
      public Iterator<TypedKey> iterator() {
        return new Iterator<TypedKey>() {
          Iterator<TypedKey> mySetIterator = set == null ? null : (Iterator) set.iterator();
          boolean mySingleReported = false;

          public boolean hasNext() {
            if (!mySingleReported && singleKey != null)
              return true;
            else if (mySetIterator != null)
              return mySetIterator.hasNext();
            else
              return false;
          }

          public TypedKey next() {
            if (!mySingleReported) {
              mySingleReported = true;
              if (singleKey != null)
                return singleKey;
            }
            if (mySetIterator != null)
              return mySetIterator.next();
            else
              throw new NoSuchElementException();
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }

      public int size() {
        int result = 0;
        if (singleKey != null)
          result++;
        if (set != null)
          result += set.size();
        return result;
      }
    };
  }

  public void copyTo(PropertyMap dest) {
    dest.mySingleKey = mySingleKey;
    dest.mySingleObject = mySingleObject;
    if (myValues == null) {
      dest.myValues = null;
    } else {
      if (dest.myValues == null) {
        dest.myValues = createMap();
      } else {
        dest.myValues.clear();
      }
      dest.myValues.putAll(myValues);
    }
  }

  public Map<TypedKey, Object> copyTo(Map<TypedKey, Object> map) {
    map.clear();
    if (mySingleKey != null) {
      map.put((TypedKey) mySingleKey, decorate((TypedKey) mySingleKey, mySingleObject));
    }
    if (myValues != null) {
      if (myDecorator == null) {
        map.putAll(myValues);
      } else {
        for (Map.Entry<TypedKey<?>, Object> e : myValues.entrySet()) {
          map.put(e.getKey(), decorate((TypedKey) e.getKey(), e.getValue()));
        }
      }
    }
    return map;
  }

  private <T> T decorate(TypedKey<T> key, T value) {
    PropertyMapValueDecorator decorator = myDecorator;
    if (decorator == null)
      return value;
    return decorator.decorate(this, key, value);
  }

  public int size() {
    int result = 0;
    if (mySingleKey != null)
      result++;
    if (myValues != null)
      result += myValues.size();
    return result;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("PropertyMap size=");
    buffer.append(size());
    for (TypedKey key : keySet()) {
      buffer.append('\n');
      buffer.append(key);
      buffer.append('=');
      buffer.append(get(key));
    }
    return buffer.toString();
  }

  public void clear() {
    mySingleKey = null;
    mySingleObject = null;
    Map<TypedKey<?>, Object> values = myValues;
    if (values != null) {
      values.clear();
      myValues = null;
    }
  }

  public boolean isEmpty() {
    return mySingleKey == null && (myValues == null || myValues.isEmpty());
  }
}
