package com.almworks.items.gui.edit.editors.text;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.StringUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.text.JTextComponent;
import java.math.BigDecimal;

public abstract class ScalarValueKey<T> {

  public abstract void setValue(EditModelState model, T value);

  public abstract void setText(EditModelState model, String newText);

  @NotNull
  public abstract String getText(EditModelState model);

  public abstract boolean isChanged(EditItemModel model);

  public abstract T getValue(EditModelState model);
  
  public abstract T getInitialValue(EditModelState model);

  public abstract void verifyData(DataVerification verifyContext, ScalarFieldEditor<T> editor);

  public abstract void commitValue(CommitContext context, DBAttribute<T> attribute);

  public abstract boolean hasValue(EditModelState model);

  public boolean[] listenTextComponent(Lifespan life, final EditModelState model, final JTextComponent textComponent) {
    final boolean[] duringUpdate = {false};
    UIUtil.addTextListener(life, textComponent, new ChangeListener() {
      @Override
      public void onChange() {
        if (duringUpdate[0]) return;
        duringUpdate[0] = true;
        try {
          setText(model, textComponent.getText());
        } finally {
          duringUpdate[0] = false;
        }
      }
    });
    return duringUpdate;
  }

  protected abstract T fromText(String text);

  protected abstract String toText(T value);

  public static class Text extends ScalarValueKey<String> {
    private final TypedKey<String> myKey;
    private final boolean myAllowEmpty;

    public Text(String debugName, boolean allowEmpty) {
      myAllowEmpty = allowEmpty;
      myKey = TypedKey.create(debugName);
    }

    @Override
    protected String fromText(String text) {
      return Util.NN(text).trim();
    }

    @Override
    protected String toText(String value) {
      return Util.NN(value).trim();
    }

    @Override
    public void setValue(EditModelState model, String value) {
      value = normalizeText(value);
      model.putValue(myKey, value);
    }

    @Override
    public void setText(EditModelState model, String newText) {
      String value = normalizeText(newText);
      String currentText = model.getValue(myKey);
      if (Util.equals(currentText, value)) return;
      setValue(model, value);
    }

    public static String normalizeText(String newText) {
      String value = Util.NN(newText).trim();
      // http://snow:10430/browse/JC-131
      value = value.replaceAll(StringUtil.LOCAL_LINE_SEPARATOR, "\n");
      if (value.isEmpty()) value = null;
      return value;
    }

    @NotNull
    @Override
    public String getText(EditModelState model) {
      return Util.NN(model.getValue(myKey));
    }

    @Override
    public boolean isChanged(EditItemModel model) {
      return !model.isEqualValue(myKey);
    }

    @Override
    public String getInitialValue(EditModelState model) {
      return model.getInitialValue(myKey);
    }

    @Override
    public String getValue(EditModelState model) {
      return normalizeText(getText(model));
    }

    @Override
    public void verifyData(DataVerification verifyContext, ScalarFieldEditor<String> editor) {
      if (myAllowEmpty || verifyContext.getPurpose() == DataVerification.Purpose.EDIT_WARNING) return;
      if (getValue(verifyContext.getModel()) == null) verifyContext.addError(editor, "Should be not empty");
    }

    @Override
    public void commitValue(CommitContext context, DBAttribute<String> attribute) {
      context.getCreator().setValue(attribute, getValue(context.getModel()));
    }

    @Override
    public boolean hasValue(EditModelState model) {
      return getValue(model) != null;
    }
  }

  public abstract static class Converting<T> extends ScalarValueKey<T> {
    private final TypedKey<T> myKey;
    private final TypedKey<String> myTextKey;
    @Nullable
    private final String myNullMessage;

    public Converting(String debugName, @Nullable String nullMessage) {
      myNullMessage = nullMessage;
      myKey = TypedKey.create(debugName + "/val");
      myTextKey = TypedKey.create(debugName + "/text");
    }

    @Override
    protected String toText(T value) {
      return value != null ? value.toString() : null;
    }

    protected abstract void verifyText(DataVerification verifyContext, FieldEditor editor, @NotNull String text);

    @Override
    public void setValue(EditModelState model, T value) {
      if (value != null) model.putValues(myKey, value, myTextKey, toText(value));
      else model.putValues(myKey, null, myTextKey, null);
    }

    @Override
    public void setText(EditModelState model, String newText) {
      newText = Text.normalizeText(newText);
      if (Util.equals(model.getValue(myTextKey), newText)) return;
      T value = fromText(newText);
      if (!Util.equals(model.getValue(myKey), value)) model.putValues(myTextKey, newText, myKey, value);
      else model.putValue(myTextKey, newText);
    }

    @NotNull
    @Override
    public String getText(EditModelState model) {
      String text = model.getValue(myTextKey);
      if (text == null) {
        T value = getValue(model);
        text = value != null ? toText(value) : null;
      }
      return Util.NN(text);
    }

    @Override
    public boolean isChanged(EditItemModel model) {
      return !model.isEqualValue(myKey);
    }

    @Override
    public T getInitialValue(EditModelState model) {
      return model.getInitialValue(myKey);
    }

    @Override
    public T getValue(EditModelState model) {
      return model.getValue(myKey);
    }

    @Override
    public void verifyData(DataVerification verifyContext, ScalarFieldEditor<T> editor) {
      EditModelState model = verifyContext.getModel();
      String text = model.getValue(myTextKey);
      if (text != null) verifyText(verifyContext, editor, text);
      else if (myNullMessage != null) verifyContext.addError(editor, myNullMessage);
    }
  }

  public static class Decimal extends Converting<BigDecimal> {
    public Decimal(String debugName) {
      super(debugName, null);
    }

    @Override
    protected BigDecimal fromText(String text) {
      try {
        return text != null ? new BigDecimal(text) : null;
      } catch (NumberFormatException e) {
        return null;
      }
    }

    protected void verifyText(DataVerification verifyContext, FieldEditor editor, @NotNull String text) {
      try {
        new BigDecimal(text);
      } catch (NumberFormatException e) {
        verifyContext.addError(editor, "Illegal value '" + text + "'");
      }
    }

    @Override
    public void commitValue(CommitContext context, DBAttribute<BigDecimal> attribute) {
      context.getCreator().setValue(attribute, getValue(context.getModel()));
    }

    @Override
    public boolean hasValue(EditModelState model) {
      return getValue(model) != null;
    }
  }
}
