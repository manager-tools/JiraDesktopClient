package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.SelfIdExtractor;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

public class JRProject {
  // Available in brief
  public static final JSONKey<String> NAME = JSONKey.text("name");
  public static final JSONKey<String> KEY = JSONKey.text("key");
  public static final JSONKey<Integer> ID = JSONKey.integer("id");
  public static final JSONKey<String> ICON = JRAvatar.EXT_URL_16;

  // Available in full only
  public static final JSONKey<String> DESCRIPTION = JSONKey.text("description");
  public static final JSONKey<String> ASSIGNEE_TYPE = JSONKey.text("assigneeType");
  /**
   * @see JRUser
   */
  public static final JSONKey<JSONObject> LEAD = JSONKey.object("lead");
  /**
   * @see JRComponent
   */
  public static final ArrayKey<JSONObject> COMPONENTS = ArrayKey.objectArray("components");
  /**
   * @see JRIssueType
   */
  public static final ArrayKey<JSONObject> ISSUE_TYPES = ArrayKey.objectArray("issueTypes");
  /**
   * @see JRVersion
   */
  public static final ArrayKey<JSONObject> VERSIONS = ArrayKey.objectArray("versions");
  /**
   * Map "Role name" -&gt; "Role url" (format [server]/rest/api/2/project/[prjKey]/role/[roleId])
   */
  public static final JSONKey<JSONObject> ROLES = JSONKey.object("roles");
  /**
   * Convertor to convert project role url to role ID
   * @see #ROLES
   */
  public static final SelfIdExtractor EXTRACT_ROLE_ID = new SelfIdExtractor("/role/");
  public static final EntityParser PARSER =
    new EntityParser.Builder()
      .map(ID, ServerProject.ID)
      .map(NAME, ServerProject.NAME)
      .map(KEY, ServerProject.KEY)
      .create(null); // todo description, projectURL, lead, URL

  @Nullable
  public static String getDisplayName(JSONObject project) {
    String key = KEY.getValue(project);
    String name = NAME.getValue(project);
    if (key == null && name == null) return null;
    else if (key == null || name == null) return key != null ? key : name;
    else return String.format("%s (%s)", key, name);
  }
}
