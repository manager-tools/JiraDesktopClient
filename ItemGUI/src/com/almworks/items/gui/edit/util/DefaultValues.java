package com.almworks.items.gui.edit.util;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ItemReference;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.Map;

public class DefaultValues {
  private final TypedKey<Long> myHolderKey = TypedKey.create("defaultsHolder");
  private final DBAttribute<AttributeMap> myAttribute;
  private final ItemReference myDefaultsHolder;
  private final Map<DBAttribute<?>, Object> myOverride = Collections15.hashMap();
  private final Condition<DBAttribute<?>> myWriteFilter;
  private boolean myWriteOnCommit = true;

  public DefaultValues(ItemReference holder, DBAttribute<AttributeMap> attribute, Condition<DBAttribute<?>> writeFilter) {
    myDefaultsHolder = holder;
    myAttribute = attribute;
    myWriteFilter = writeFilter;
  }

  public <T> void override(DBAttribute<T> attribute, T value) {
    myOverride.put(attribute, value);
    myWriteOnCommit = false;
  }

  public void setDefaults(DBReader reader, EditItemModel model) {
    long holder = myDefaultsHolder.findItem(reader);
    AttributeMap defaults = null;
    if (holder > 0) defaults = myAttribute.getValue(holder, reader);
    if (defaults == null) defaults = new AttributeMap();
    for (Map.Entry<DBAttribute<?>, Object> entry : myOverride.entrySet()) {
      DBAttribute<Object> attribute = (DBAttribute<Object>) entry.getKey();
      Object value = entry.getValue();
      defaults.put(attribute, value);
    }
    model.putHint(EditItemModel.DEFAULT_VALUES, defaults);
    model.putHint(myHolderKey, holder);
  }

  public void commitDefaults(CommitContext c) {
    if (!myWriteOnCommit) return;
    c.afterCommit(new Procedure<CommitContext>() {
      @Override
      public void invoke(CommitContext context) {
        final long holder = Util.NN(context.getModel().getValue(myHolderKey), 0l);
        if (holder <= 0) return;
        AttributeMap defaults = context.getModel().getValue(EditItemModel.DEFAULT_VALUES);
        if (defaults == null) return;
        AttributeMap copy = new AttributeMap();
        for (DBAttribute<?> attribute : defaults.keySet()) {
          if (myWriteFilter.isAccepted(attribute)) copy.putFrom(defaults, attribute);
        }
        context.getDrain().changeItem(holder).setValue(myAttribute, copy);
      }
    });
  }
}
