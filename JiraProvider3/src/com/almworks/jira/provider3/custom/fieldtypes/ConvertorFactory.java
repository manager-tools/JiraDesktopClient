package com.almworks.jira.provider3.custom.fieldtypes;

import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.jql.JqlEnum;
import com.almworks.jira.provider3.sync.jql.JqlNumeric;
import com.almworks.jira.provider3.sync.jql.JqlSubStrings;

public interface ConvertorFactory {
  /**
   * @see com.almworks.jira.provider3.custom.FieldKind#getJqlSearch(com.almworks.items.sync.ItemVersion)
   */
  JQLConvertor createJql(ItemVersion field);

  ConvertorFactory SEARCH_FLOAT = field -> {
    JqlSearchInfo<?> info = JqlSearchInfo.load(field);
    return info != null ? new JqlNumeric(info.getJqlName(), info.getAttribute(), info.getDisplayName()) : null;
  };

  ConvertorFactory SEARCH_TEXT = field -> {
    JqlSearchInfo<String> info = JqlSearchInfo.loadScalar(field, String.class);
    return info != null ? new JqlSubStrings(info.getJqlName(), info.getAttribute(), info.getDisplayName()) : null;
  };

  ConvertorFactory USER = field -> {
    JqlSearchInfo<?> info = JqlSearchInfo.load(field);
    return info != null ? JqlEnum.user(info.getJqlName(), info.getAttribute(), info.getDisplayName()) : null;
  };
}
