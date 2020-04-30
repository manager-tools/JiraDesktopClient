package com.almworks.jira.provider3.custom.loadxml;

import org.almworks.util.TypedKey;

import java.util.Map;

public class ConfigKeys {
  // Generic keys
  /**
   * JIRA field key
   */
  public static final TypedKey<String> KEY = TypedKey.create("key", String.class);
  /**
   * Field type. Values: enum, multiEnum, text, decimal, day, datetime
   */
  public static final TypedKey<String> TYPE = TypedKey.create("type", String.class);
  /**
   * If true constraint is unknown for remote search
   */
  public static final TypedKey<Boolean> NO_REMOTE_SEARCH = TypedKey.create("noRemoteSearch", Boolean.class);
  /**
   * Override default attribute prefix (for backward compatibility)
   */
  public static final TypedKey<String> PREFIX = TypedKey.create("prefix", String.class);
  /**
   * Describes editable capability
   */
  @SuppressWarnings("unchecked")
  public static final TypedKey<Map<TypedKey<?>, ?>> EDITABLE = TypedKey.<Map<TypedKey<?>, ?>>create("editable", (Class) Map.class);

  // Enum keys
  /**
   * Name for nothing-selected constraint.
   */
  public static final TypedKey<String> NONE_NAME = TypedKey.create("noneName", String.class);

  // Multi enum keys

  // Editable keys
  /**
   * Defines editor. Values depends on type
   */
  public static final TypedKey<String> EDITOR = TypedKey.create("editor", String.class);
  /**
   * Defines upload. Values depends on type
   * @deprecated Not used, Left for compatibility with old DB-stored data
   */
  @Deprecated
  public static final TypedKey<String> UPLOAD = TypedKey.create("upload", String.class);
  /**
   * For enums defines to JSON conversion.
   */
  public static final TypedKey<String> UPLOAD_JSON = TypedKey.create("uploadJson", String.class);
}
