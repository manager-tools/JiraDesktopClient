package com.almworks.gui;

import com.almworks.util.LogHelper;
import com.almworks.util.model.ValueModel;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;

public class WindowTitleSections {
  public static final Role<WindowTitleSections> ROLE = Role.role(WindowTitleSections.class);

  private final ValueModel<String> myWindowTitle = new ValueModel<String>();
  private final List<TypedKey<String>> mySections = Collections15.arrayList();
  private final List<String> myValues = Collections15.arrayList();
  private final List<String> myPreSeparators = Collections15.arrayList();

  public ValueModel<String> getModel() {
    return myWindowTitle;
  }

  @ThreadAWT
  public void appendSection(@Nullable TypedKey<String> section, @Nullable String value) {
    appendSection(section, null, value);
  }

  @ThreadAWT
  public void appendSection(@Nullable TypedKey<String> section, @Nullable String preSeparator, @Nullable String value) {
    if (!EventQueue.isDispatchThread()) {
      LogHelper.error("Wrong thread", Thread.currentThread());
      return;
    }
    value = Util.NN(value).trim();
    if (section == null && value.isEmpty()) return;
    preSeparator = Util.NN(preSeparator);
    mySections.add(section);
    myValues.add(value);
    myPreSeparators.add(preSeparator);
    updateTitle();
  }

  private void updateTitle() {
    StringBuilder builder = new StringBuilder();
    boolean first = true;
    for (int i = 0; i < myValues.size(); i++) {
      String value = myValues.get(i);
      if (value == null) continue;
      if (!first) {
        String separator = myPreSeparators.get(i);
        if (separator.isEmpty()) separator = " ";
        builder.append(separator);
      }
      builder.append(value);
      first = false;
    }
    myWindowTitle.setValue(builder.toString());
  }
}
