package com.almworks.util.properties;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.util.Date;

/**
 * @author : Dyoma
 */
public abstract class PropertyKey <M,V> extends TypedKey<M> {
  public static final ChangeState NOT_CHANGED = new ChangeState(false, "notChanged");
  public static final ChangeState CHANGED = new ChangeState(true, "changed");
  public static final ChangeState NEW = new ChangeState(true, "new");

  private final TypedKey<V> myValueKey;

  public PropertyKey(@NotNull String name) {
    this(name, PropertyKey.<V>create(name + "#valueKey"));
  }

  public PropertyKey(@NotNull String name, @NotNull TypedKey<V> valueKey) {
    super(name, null, null);
    myValueKey = valueKey;
  }

  public abstract V getModelValue(PropertyModelMap properties);

  public abstract void setModelValue(PropertyModelMap properties, V value);

  public abstract void installModel(ChangeSupport changeSupport, PropertyModelMap propertyMap);

  public TypedKey<V> getValueKey() {
    return myValueKey;
  }

  public String getDisplayableName() {
    return getName();
  }

  public void tryCopyValue(PropertyModelMap to, PropertyMap copyFrom) {
    if (!copyFrom.containsKey(getValueKey()))
      return;
    copyValue(to, copyFrom);
  }

  public void copyValue(PropertyModelMap to, PropertyMap from) {
    V value = from.get(getValueKey());
    setModelValue(to, value);
  }

  public PropertyKey<M, V> getModelKey() {
    return this;
  }

  public abstract ChangeState isChanged(PropertyModelMap models, PropertyMap originalValues);

  public abstract void setInitialValue(PropertyMap values, V value);

  public static EnablingKey<Document, String> createEnablingText(final String name,
    final PropertyKey<Document, String> parent, final BooleanPropertyKey enableKey)
  {
    final PropertyKey<Document, String> enabledState = createText(name + "#enabled");
    return createEnablingProperty(name, enabledState, parent, enableKey);
  }

  private static <M, V> EnablingKey<M, V> createEnablingProperty(final String name,
    final PropertyKey<M, V> enabledState, final PropertyKey<M, V> parent, final BooleanPropertyKey enableKey)
  {
    return new EnablingKey<M, V>(name, enabledState, parent, enableKey);
  }

  public static PropertyKey<Document, String> createText(String name) {
    return new PropertyKey<Document, String>(name) {
      public void installModel(final ChangeSupport changeSupport, PropertyModelMap propertyMap) {
        final PlainDocument document = new PlainDocument();
        document.addDocumentListener(new DocumentAdapter() {
          protected void documentChanged(DocumentEvent e) {
            changeSupport.fireChanged(getValueKey(), null, DocumentUtil.getDocumentText(document));
          }
        });
        propertyMap.put(this, document);
      }

      public String getModelValue(PropertyModelMap properties) {
        return DocumentUtil.getDocumentText(properties.get(this));
      }

      public void setModelValue(PropertyModelMap properties, String value) {
        if (!value.equals(getModelValue(properties)))
          DocumentUtil.changeDocumentText(properties.get(this), value);
      }

      public ChangeState isChanged(PropertyModelMap models, PropertyMap originalValues) {
        return ChangeState.choose(this, originalValues, getModelValue(models));
      }

      public void setInitialValue(PropertyMap values, String value) {
        assert !(values instanceof PropertyModelMap) : values.toString();
        values.put(getValueKey(), value);
      }
    };
  }
  
  public static PropertyKey<ValueModel<Date>, Date> createDate(String name) {
    return new PropertyKey<ValueModel<Date>, Date>(name) {
      public void installModel(final ChangeSupport changeSupport, PropertyModelMap propertyMap) {
        final ValueModel<Date> model = ValueModel.create();
        model.addAWTChangeListener(new ChangeListener() {
          public void onChange() {
            changeSupport.fireChanged(getValueKey(), null, model.getValue());
          }
        });
        propertyMap.put(this, model);
      }

      public Date getModelValue(PropertyModelMap properties) {
        ValueModel<Date> valueModel = properties.get(this);
        return valueModel == null ? null : valueModel.getValue();
      }

      public void setModelValue(PropertyModelMap properties, Date value) {
        ValueModel<Date> valueModel = properties.get(this);
        if (valueModel == null)
          return;
        if (!Util.equals(value, valueModel.getValue()))
          valueModel.setValue(value);
      }

      public ChangeState isChanged(PropertyModelMap models, PropertyMap originalValues) {
        return ChangeState.choose(this, originalValues, getModelValue(models));
      }

      public void setInitialValue(PropertyMap values, Date value) {
        assert !(values instanceof PropertyModelMap) : values.toString();
        values.put(getValueKey(), value);
      }
    };
  }

  public static class ChangeState {
    private final boolean myChanged;
    private final String myDebugName;

    public ChangeState(boolean changed, String debugName) {
      myChanged = changed;
      myDebugName = debugName;
    }

    public boolean isChanged() {
      return myChanged;
    }

    public String toString() {
      return myDebugName;
    }

    public static <V> ChangeState choose(PropertyKey<?, V> key, PropertyMap values, V value) {
      TypedKey<V> valueKey = key.getValueKey();
      if (!values.containsKey(valueKey))
        return PropertyKey.NEW;
      return Util.equals(values.get(valueKey), value) ? PropertyKey.NOT_CHANGED : PropertyKey.CHANGED;
    }
  }

  public static class EnablingKey<M, V> extends PropertyKey<M, V> {
    private final PropertyKey<M, V> myEnabledState;
    private final PropertyKey<M, V> myParent;
    private final BooleanPropertyKey myEnableKey;

    public EnablingKey(String name, PropertyKey<M, V> enabledState, PropertyKey<M, V> parent,
      BooleanPropertyKey enableKey)
    {
      super(name);
      assert enabledState != null;
      assert parent != null;
      assert enableKey != null;
      myEnabledState = enabledState;
      myParent = parent;
      myEnableKey = enableKey;
    }

    public PropertyKey<M, V> getModelKey() {
      return myEnabledState;
    }

    public ChangeState isChanged(PropertyModelMap models, PropertyMap originalValues) {
      ChangeState enableChange = myEnableKey.isChanged(models, originalValues);
      if (enableChange == CHANGED)
        return CHANGED;
      if (!isEnabled(models))
        return NOT_CHANGED;
      return myEnabledState.isChanged(models, originalValues);
    }

    public void setInitialValue(PropertyMap values, V value) {
      myEnableKey.setInitialValue(values, Boolean.TRUE);
      myParent.setInitialValue(values, value);
      myEnabledState.setInitialValue(values, value);
    }

    public void tryCopyValue(PropertyModelMap to, PropertyMap copyFrom) {
      myEnableKey.tryCopyValue(to, copyFrom);
      boolean enabled = myEnableKey.getModelValue(to);
      if (enabled) {
        myParent.tryCopyValue(to, copyFrom);
        myEnabledState.tryCopyValue(to, copyFrom);
      }
    }

    public V getModelValue(PropertyModelMap properties) {
      return (isEnabled(properties) ? myEnabledState : myParent).getModelValue(properties);
    }

    public void setModelValue(PropertyModelMap properties, V value) {
      myEnabledState.setModelValue(properties, value);
    }

    public BooleanPropertyKey getEnableKey() {
      return myEnableKey;
    }

    public void installModel(ChangeSupport changeSupport, final PropertyModelMap propertyMap) {
      propertyMap.ensureInstalled(myEnableKey);
      propertyMap.ensureInstalled(myParent);
      myEnabledState.installModel(changeSupport, propertyMap);
      final PropertyChangeListener<V> parentListener = new PropertyChangeListener<V>() {
        public void propertyChanged(TypedKey<V> key, Object bean, V oldValue, V newValue) {
          if (!isEnabled(propertyMap))
            setModelValue(propertyMap, myParent.getModelValue(propertyMap));
        }
      };
      propertyMap.addPropertyChangeListener(myParent.getValueKey(), parentListener);
      PropertyChangeListener<Object> enableListener = new PropertyChangeListener<Object>() {
        public void propertyChanged(TypedKey<Object> key, Object bean, Object oldValue, Object newValue) {
          if (isEnabled(propertyMap))
            parentListener.propertyChanged(null, null, null, null);
          else
            myEnabledState.setModelValue(propertyMap, myParent.getModelValue(propertyMap));
        }
      };
      propertyMap.addPropertyChangeListener(myEnableKey.getValueKey(), enableListener);
      propertyMap.addPropertyChangeListener(myEnabledState.getValueKey(), new PropertyChangeListener<V>() {
        public void propertyChanged(TypedKey<V> key, Object bean, V oldValue, V newValue) {
          if (!isEnabled(propertyMap))
            return;
          myParent.setModelValue(propertyMap, myEnabledState.getModelValue(propertyMap));
        }
      });
      enableListener.propertyChanged(null, null, null, null);
      propertyMap.markIntalled(this);
    }

    private boolean isEnabled(PropertyModelMap models) {
      return myEnableKey.getModelValue(models);
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof EnablingKey<?, ?>))
        return false;
      EnablingKey<?, ?> other = (EnablingKey<?, ?>) obj;
      return other.myParent.equals(myParent) && other.myEnableKey.equals(myEnableKey);
    }

    public int hashCode() {
      return myParent.hashCode() ^ myEnableKey.hashCode();
    }
  }
}
