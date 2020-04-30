package com.almworks.recentitems;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.properties.Role;

public interface RecentItemsService {
  Role<RecentItemsService> ROLE = Role.role("RecentItems", RecentItemsService.class);

  DBNamespace NS = DBNamespace.moduleNs("com.almworks.recentItems");
  DBNamespace REC_NS = RecentItemsService.NS.subNs("recentItem");
  DBItemType TYPE_RECORD = REC_NS.type();
  DBAttribute<Long> ATTR_MASTER = REC_NS.master("master");
  DBAttribute<Long> ATTR_TIMESTAMP = REC_NS.longAttr("timestamp");
  DBAttribute<Integer> ATTR_REC_TYPE = REC_NS.integer("recordType");
  BoolExpr<DP> EXPR_RECORDS = DPEqualsIdentified.create(DBAttribute.TYPE, TYPE_RECORD);

  void addRecord(long artifactKey, RecordType type);
}
