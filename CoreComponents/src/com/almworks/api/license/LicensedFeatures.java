package com.almworks.api.license;

import com.almworks.util.collections.MultiMap;
import org.almworks.util.TypedKey;

import java.util.Collections;
import java.util.Map;

public abstract class LicensedFeatures {
  public static final String FEATURE_UNLICENSED_EVALUATION = "evaluation";
  public static final String FEATURE_BUGZILLA = "bugzilla";
  public static final String FEATURE_JIRA = "jira";
  public static final String FEATURE_ACTIVATABLE = "activate";
  public static final String FEATURE_HINTS = "hints";

  public static final TypedKey<Boolean> ACTIVE = TypedKey.create("active", Boolean.class);
  public static final TypedKey<String> LOCK_URL = TypedKey.create("url", String.class);
  public static final TypedKey<String> LOCK_URL_HASH = TypedKey.create("urlhash", String.class);
  public static final TypedKey<String> SUBJECT = TypedKey.create("subject", String.class);
  public static final TypedKey<Integer> SITE_COUNT = TypedKey.create("siteCount", Integer.class);
  public static final TypedKey<Integer> MAX_PROJECTS_PER_CONNECTION = TypedKey.create("maxppc", Integer.class);

  public static final MultiMap<String, String> ACTIVE_MAP = createActiveMap();

  private static MultiMap<String, String> createActiveMap() {
    MultiMap<String, String> result = MultiMap.create();
    result.add(ACTIVE.getName(), "true");
    return result;
  }

  public static final Map<String, MultiMap<String, String>> LICENSE_VERSION_1_SUBST_FEATURES =
    Collections.unmodifiableMap(Collections.singletonMap(FEATURE_BUGZILLA, ACTIVE_MAP));

}
