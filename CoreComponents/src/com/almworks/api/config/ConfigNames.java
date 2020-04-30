package com.almworks.api.config;

import com.almworks.util.config.MediumOptimization;
import com.almworks.util.config.UtilConfigNames;

public class ConfigNames {
  public static final String ATTRIBUTE_ID = "attributeId";
  public static final String PINNED_SETTING = "pinned";
  public static final String NAME_SETTING = "name";
  public static final String NODE_ID = "nodeId";
  public static final String HIDE_EMPTY_CHILDREN = "hideEmptyChildren";
  public static final String IS_HIDING_EMPTY_CHILDREN = "isHidingEmptyChildren";
  public static final String CHILD_NODE_ORDER = "childNodeOrder";
  public static final String CHILD_NODE_ORDER_2 = "childNodeOrder2";
  public static final String EXPANDER_KEY = "expander";
  public static final String CONNECTION_LOADING_KEY = "connectionLoading";
  public static final String QUERY_FORMULA = "query";
  public static final String DEFAULT_NAME_FLAG = "defaultName";
  public static final String KLUDGE_DISTRIBUTION_QUERY_TAG_NAME = "distributionQuery";
  public static final String KLUDGE_DISTRIBUTION_FOLDER_TAG_NAME = "distributionFolder";
  public static final String KLUDGE_DISTRIBUTION_GROUP_TAG_NAME = "distributionGroup";
  public static final String FOLDER_KEY = "folder";
  public static final String USER_QUERY_KEY = "userQuery";
  public static final String NOTE_KEY = "note";
  public static final String LAZY_DISTRIBUTION_KEY = "lazyDistribution";
  public static final String PRESET_QUERY = "presetQuery";
  public static final String PRESET_FOLDER = "presetFolder";
  public static final String PRESET_DISTRIBUTION_FOLDER = "presetDistributionFolder";
  public static final String PRESET_DISTRIBUTION_QUERY = "presetDistributionQuery";
  public static final String PRESET_LAZY_DISTRIBUTION_QUERY = "presetLazyDistribution";
  public static final String PROTOTYPE_TAG = "prototype";

  // distribution params
  public static final String DISTRIBUTION_GROUPING = "grouping";
  public static final String DISTRIBUTION_VALUES_FILTER = "valuesFilter";
  public static final String DISTRIBUTION_GROUPS_FILTER = "groupsFilter";
  public static final String DISTRIBUTION_ARRANGE_IN_GROUPS = "arrangeGroups";

  // 1.5
  public static final String ICON_PATH = "iconPath";


  public static void register() {
    UtilConfigNames.register();
    MediumOptimization.addInternedNames(ATTRIBUTE_ID, PINNED_SETTING, NAME_SETTING, NODE_ID, HIDE_EMPTY_CHILDREN,
      CHILD_NODE_ORDER, CHILD_NODE_ORDER_2, EXPANDER_KEY, CONNECTION_LOADING_KEY, QUERY_FORMULA, DEFAULT_NAME_FLAG,
      KLUDGE_DISTRIBUTION_FOLDER_TAG_NAME, KLUDGE_DISTRIBUTION_QUERY_TAG_NAME, FOLDER_KEY, USER_QUERY_KEY, NOTE_KEY,
      LAZY_DISTRIBUTION_KEY, PRESET_QUERY, PRESET_FOLDER, PRESET_DISTRIBUTION_FOLDER, PRESET_DISTRIBUTION_QUERY,
      PRESET_LAZY_DISTRIBUTION_QUERY, PROTOTYPE_TAG, ICON_PATH, IS_HIDING_EMPTY_CHILDREN);

    for (int i = 0; i < 30; i++) {
      MediumOptimization.addInternedName(String.valueOf(i));
    }
  }
}
