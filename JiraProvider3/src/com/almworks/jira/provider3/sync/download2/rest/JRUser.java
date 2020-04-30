package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.operations.RestAuth1Session;

public class JRUser {
  public static final JSONKey<String> ACCOUNT_ID = RestAuth1Session.USER_ACCOUNT_ID;
  public static final JSONKey<String> NAME = JSONKey.text("displayName");
  public static final JSONKey<String> ICON_16 = JRAvatar.EXT_URL_16;
}
