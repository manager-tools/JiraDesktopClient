package com.almworks.jira.provider3.custom.fieldtypes.enums.multi;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.util.MultiEnumInfo;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.jira.provider3.custom.fieldtypes.enums.EnumTypeKind;
import com.almworks.jira.provider3.custom.loadxml.ConfigKeys;
import com.almworks.jira.provider3.gui.JiraFields;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

class MultiEnumProperties {
  /**
   * Show values in single line on issue viewer.
   */
  private static final TypedKey<Boolean> INLINE_VIEW = TypedKey.create("inlineView", Boolean.class);
  /**
   * When true column comparator is lexicographical
   */
  private static final TypedKey<Boolean> LEXICAL_COLUMN_ORDER = TypedKey.create("lexicalColumnOrder", Boolean.class);
  /**
   * Separator to renderer value.
   */
  private static final TypedKey<String> RENDERER_SEPARATOR = TypedKey.create("rendererSeparator", String.class);

  @SuppressWarnings("unchecked")
  public static final List<TypedKey<?>> KEYS = Collections15.<TypedKey<?>>unmodifiableListCopy(INLINE_VIEW, LEXICAL_COLUMN_ORDER, RENDERER_SEPARATOR);

  private final String myNoneId;
  private final String myNoneDisplayName;
  private final boolean myInlineView;
  private final String myRenderingSeparator;
  private final boolean myLexicalColumnOrder;

  public MultiEnumProperties(String noneId, String noneDisplayName, boolean inlineView, String renderingSeparator, boolean lexicalColumnOrder) {
    myNoneId = noneId;
    myNoneDisplayName = noneDisplayName;
    myInlineView = inlineView;
    myRenderingSeparator = renderingSeparator;
    myLexicalColumnOrder = lexicalColumnOrder;
  }

  public static MultiEnumProperties create(Map<TypedKey<?>, ?> map, @NotNull String noneId, @NotNull String defaultNoneName) {
    String noneName = Util.NN(ConfigKeys.NONE_NAME.getFrom(map), defaultNoneName);
    boolean inlineView = Util.NN(INLINE_VIEW.getFrom(map), false);
    String separator = Util.NN(RENDERER_SEPARATOR.getFrom(map), ", ");
    boolean lexicalColumnOrder = Util.NN(LEXICAL_COLUMN_ORDER.getFrom(map), true);
    return new MultiEnumProperties(noneId, noneName, inlineView, separator, lexicalColumnOrder);
  }

  public MultiEnumInfo createFieldInfo(DBAttribute<Set<Long>> attribute, DBIdentity connection, String id, String name, String connectionIdPrefix, EnumTypeKind typeKind,
    DBItemType type) {
    MultiEnumInfo info = JiraFields.multiEnum(ItemDownloadStage.QUICK, myLexicalColumnOrder)
      .setAttribute(attribute)
      .setOwner(connection)
      .setId(id)
      .setConstraintId(name != null ? name : id)
      .setColumnId(connectionIdPrefix + id)
      .setDisplayName(name)
      .setHideEmptyLeftField(true)
      .setNullableEnum(myNoneId, myNoneDisplayName);
    if (myInlineView) info.setInlineLeftField();
    else info.setMultiLineTextField();
    typeKind.setEnumType(info, type);
    info.setRendererSeparator(myRenderingSeparator);
    return info;
  }
}
