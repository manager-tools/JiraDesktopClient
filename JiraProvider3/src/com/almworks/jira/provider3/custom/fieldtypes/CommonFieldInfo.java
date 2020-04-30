package com.almworks.jira.provider3.custom.fieldtypes;

import com.almworks.items.gui.meta.util.FieldInfo;
import com.almworks.items.gui.meta.util.ScalarFieldInfo;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

import java.util.List;
import java.util.Map;

public class CommonFieldInfo {
  /**
   * If false no column is created for the field
   */
  private static final TypedKey<Boolean> HAS_COLUMN = TypedKey.create("hasColumn", Boolean.class);
  /**
   * If false no reorder by field is allowed
   */
  private static final TypedKey<Boolean> HAS_REORDER = TypedKey.create("hasReorder", Boolean.class);
  /**
   * If false no constraint descriptor is generated (search not allowed by the field)
   */
  private static final TypedKey<Boolean> HAS_SEARCH = TypedKey.create("hasSearch", Boolean.class);
  /**
   * If false value is not visible on viewer (otherwise viewer is defined by other values)
   */
  private static final TypedKey<Boolean> HAS_VIEWER = TypedKey.create("hasViewerField", Boolean.class);

  @SuppressWarnings("unchecked")
  public static final List<TypedKey<?>> KEYS = Collections15.<TypedKey<?>>unmodifiableListCopy(HAS_COLUMN, HAS_SEARCH, HAS_REORDER, HAS_VIEWER);

  private final boolean myNoColumn;
  private final boolean myNoSearch;
  private final boolean myNoReorder;
  private final boolean myNoViewerField;

  public CommonFieldInfo(boolean noColumn, boolean noSearch, boolean noReorder, boolean noViewerField) {
    myNoColumn = noColumn;
    myNoSearch = noSearch;
    myNoReorder = noReorder;
    myNoViewerField = noViewerField;
  }

  public static CommonFieldInfo create(Map<TypedKey<?>, ?> fieldMap) {
    boolean noColumn = !Util.NN(HAS_COLUMN.getFrom(fieldMap), true);
    boolean noSearch = !Util.NN(HAS_SEARCH.getFrom(fieldMap), true);
    boolean noReorder = !Util.NN(HAS_REORDER.getFrom(fieldMap), true);
    boolean noViewerField = !Util.NN(HAS_VIEWER.getFrom(fieldMap), true);
    return new CommonFieldInfo(noColumn, noSearch, noReorder, noViewerField);
  }

  public FieldInfo wrapFieldInfo(FieldInfo info) {
    if (info == null) return null;
    return new FieldInfo.Wrapper(info, myNoColumn, myNoSearch, myNoReorder, myNoViewerField);
  }

  public CommonFieldInfo noReorder() {
    if (myNoReorder) return this;
    return new CommonFieldInfo(myNoColumn, myNoSearch, true, myNoViewerField);
  }

  public void update(ScalarFieldInfo<?> info) {
    if (!myNoReorder) info.allowReorder();
  }
}
