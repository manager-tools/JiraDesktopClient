package com.almworks.items.gui.edit.editors;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.TypedKey;

public abstract class ConstEditor<A, V> extends MockEditor {
  private final DBAttribute<A> myAttribute;
  private final TypedKey<V> myKey;

  public ConstEditor(DBAttribute<A> attribute) {
    super(NameMnemonic.EMPTY);
    myAttribute = attribute;
    myKey = TypedKey.create(attribute.getId() + "/val");
  }

  public static <T> void install(EditItemModel model, DBAttribute<T> attribute, T value) {
    ConstEditor<T, T> editor = createRaw(attribute);
    editor.setValue(model, value);
  }

  public static <T> ConstEditor<T, T> createRaw(final DBAttribute<T> attribute) {
    return new ConstEditor<T, T>(attribute) {
      @Override
      public void commit(CommitContext context) {
        context.getCreator().setValue(getAttribute(), getValue(context.getModel()));
      }
    };
  }

  public void setValue(EditItemModel model, V value) {
    model.registerEditor(this);
    model.putValue(myKey, value);
  }

  public V getValue(EditModelState model) {
    return model.getValue(myKey);
  }

  public DBAttribute<A> getAttribute() {
    return myAttribute;
  }

  @Override
  public boolean hasDataToCommit(EditItemModel model) {
    return true;
  }

  @Override
  public boolean hasValue(EditModelState model) {
    return model.getValue(myKey) != null;
  }
}
