package com.almworks.items.gui.edit.editors.text;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.DataVerification;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.FieldEditor;
import com.almworks.util.datetime.TimeIntervalUtil;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class TimeIntervalValue extends ScalarValueKey.Converting<Integer> {
  @Nullable
  private final String myDefaultUnitSuffix;

  public TimeIntervalValue(String debugName, @Nullable String defaultUnitSuffix, boolean mandatory) {
    super(debugName, mandatory ? "missing value" : null);
    myDefaultUnitSuffix = defaultUnitSuffix;
  }

  @Override
  protected Integer fromText(String text) {
    return fromText(text, null);
  }

  private Integer fromText(String text, @Nullable ArrayList<String> errors) {
    Integer value = TimeIntervalUtil.parseText(text);
    if (value != null && value < 0) value = null;
    if (value != null) return value;
    if (myDefaultUnitSuffix != null) {
      try {
        int intValue = Integer.parseInt(text);
        if (intValue > 0) text = intValue + myDefaultUnitSuffix;
      } catch (NumberFormatException e) {
        // ignore
      }
    }
    value = TimeIntervalUtil.parseText(text, errors);
    if (value != null && value < 0) value = null;
    return value;
  }

  @Override
  protected String toText(Integer value) {
    return TimeIntervalUtil.toTextDuration(value);
  }

  @Override
  protected void verifyText(DataVerification verifyContext, FieldEditor editor, @NotNull String text)
  {
    ArrayList<String> error = Collections15.arrayList();
    fromText(text, error);
    if (!error.isEmpty()) verifyContext.addError(editor, "Has problems: " + TextUtil.separate(error, "; "));
  }

  @Override
  public void commitValue(CommitContext context, DBAttribute<Integer> attribute) {
    context.getCreator().setValue(attribute, getValue(context.getModel()));
  }

  @Override
  public boolean hasValue(EditModelState model) {
    String text = Util.NN(getText(model)).trim();
    return text.length() > 0;
  }
}
