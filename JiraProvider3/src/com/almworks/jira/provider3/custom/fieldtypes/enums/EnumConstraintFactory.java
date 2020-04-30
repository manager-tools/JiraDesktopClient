package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.custom.fieldtypes.ConvertorFactory;
import com.almworks.jira.provider3.custom.fieldtypes.JqlSearchInfo;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.jira.provider3.sync.jql.JqlEnum;

class EnumConstraintFactory implements ConvertorFactory {
  private final DBAttribute<?> myJqlAttribute;

  public EnumConstraintFactory(DBAttribute<?> jqlIdAttribute) {
    myJqlAttribute = jqlIdAttribute;
  }

  @Override
  public JQLConvertor createJql(ItemVersion field) {
    JqlSearchInfo<?> info = JqlSearchInfo.load(field);
    return info != null ? JqlEnum.generic(info.getJqlName(), info.getAttribute(), myJqlAttribute, info.getDisplayName()) : null;
  }
}
