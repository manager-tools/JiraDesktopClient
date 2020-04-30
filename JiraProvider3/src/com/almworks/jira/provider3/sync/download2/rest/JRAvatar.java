package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.restconnector.json.JSONKey;
import org.json.simple.JSONObject;

public class JRAvatar {
  /**
   * Url of 16x16 icon
   */
  public static final JSONKey<String> URL_16 = JSONKey.text("16x16");
  /**
   * Url of 48x48 icon
   */
  public static final JSONKey<String> URL_48 = JSONKey.text("48x48");

  /**
   * Common reference name to avatar object
   */
  public static final JSONKey<JSONObject> EXT_REF = JSONKey.object("avatarUrls");

  /**
   * Accessor key to get 16x16 right from external object.<br><br>
   * <code>JSONObject avatar = EXT_REF.getValue(myObject);<br>
   * String url = URL_16.getValue(avatar);
   * </code><br><br>
   * is equal to<br><br>
   * <code>
   *   String url = EXT_URL16.getValue(myObject);
   * </code>
   */
  public static final JSONKey<String> EXT_URL_16 = JSONKey.composition(EXT_REF, URL_16);
}
