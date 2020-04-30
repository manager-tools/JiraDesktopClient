package com.almworks.jira.provider3.custom.fieldtypes;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.BadUtil;
import com.almworks.jira.provider3.schema.CustomField;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.util.LogHelper;
import org.almworks.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;

public class JqlSearchInfo<T> {
  private final DBAttribute<T> myAttribute;
  private final String myId;
  private final String myDisplayName;

  private JqlSearchInfo(DBAttribute<T> attribute, String id, String displayName) {
    //To change body of created methods use File | Settings | File Templates.
    myAttribute = attribute;
    myId = id;
    myDisplayName = displayName;
  }

  @Nullable
  public static JqlSearchInfo<?> load(ItemVersion field, String ... possibleIds) {
    DBAttribute<?> attribute = CustomField.ATTRIBUTE2.getValue(field);
    String strId = field.getValue(CustomField.ID);
    String displayName = field.getValue(CustomField.NAME);
    if (ArrayUtil.indexOf(possibleIds, strId) < 0) {
      Matcher m = ServerCustomField.ID_PATTERN.matcher(strId);
      if (!m.matches()) {
        LogHelper.error("Unexpected custom field id", strId);
        return null;
      }
      int intId;
      try {
        intId = Integer.parseInt(m.group(1));
        strId = ServerCustomField.jqlId(intId);
      } catch (NumberFormatException e) {
        LogHelper.error(e, strId);
        return null;
      }
    }
    //noinspection unchecked
    return attribute != null && strId != null ? new JqlSearchInfo<Object>((DBAttribute<Object>) attribute, strId, displayName) : null;
  }

  public static <T> JqlSearchInfo<T> loadScalar(ItemVersion field, Class<T> scalar) {
    JqlSearchInfo<?> info = load(field);
    if (info == null) return null;
    DBAttribute<?> attribute = info.getAttribute();
    DBAttribute<T> scalarAttribute = BadUtil.castScalar(scalar, attribute);
    if (scalarAttribute == null) {
      LogHelper.error("Wrong attribute", scalar, attribute);
      return null;
    }
    //noinspection unchecked
    return (JqlSearchInfo<T>) info;
  }

  @NotNull
  public String getJqlName() {
    return myId;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  @NotNull
  public DBAttribute<T> getAttribute() {
    return myAttribute;
  }
}
