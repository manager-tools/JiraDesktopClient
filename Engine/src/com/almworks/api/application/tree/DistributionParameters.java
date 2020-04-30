package com.almworks.api.application.tree;

import com.almworks.api.config.ConfigNames;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.sfs.StringFilterSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DistributionParameters {
  private final String myGroupingName;
  private final boolean myArrangeInGroups;
  private final StringFilterSet myValuesFilter;
  private final StringFilterSet myGroupsFilter;
  private final boolean myHideEmptyQueries;

  public DistributionParameters(String groupingName, boolean arrangeInGroups, StringFilterSet valuesFilter,
    StringFilterSet groupsFilter, boolean hideEmptyQueries) {
    myGroupingName = groupingName;
    myArrangeInGroups = arrangeInGroups;
    myValuesFilter = valuesFilter;
    myGroupsFilter = groupsFilter;
    myHideEmptyQueries = hideEmptyQueries;
  }

  @Nullable
  public String getGroupingName() {
    return myGroupingName;
  }

  @Nullable
  public StringFilterSet getValuesFilter() {
    return myValuesFilter;
  }

  @Nullable
  public StringFilterSet getGroupsFilter() {
    return myGroupsFilter;
  }

  public boolean isArrangeInGroups() {
    return myArrangeInGroups;
  }

  public boolean isHideEmptyQueries() {
    return myHideEmptyQueries;
  }

  public DistributionParameters setHideEmptyQueries(boolean hide) {
    return new DistributionParameters(myGroupingName, myArrangeInGroups, myValuesFilter, myGroupsFilter, hide);
  }

  public void writeConfig(Configuration config) {
    if (myGroupingName == null) {
      config.removeSettings(ConfigNames.DISTRIBUTION_GROUPING);
    } else {
      config.setSetting(ConfigNames.DISTRIBUTION_GROUPING, myGroupingName);
    }
    config.setSetting(ConfigNames.DISTRIBUTION_ARRANGE_IN_GROUPS, myArrangeInGroups);
    if (myValuesFilter != null) {
      myValuesFilter.writeTo(config.getOrCreateSubset(ConfigNames.DISTRIBUTION_VALUES_FILTER));
    } else {
      config.removeSubsets(ConfigNames.DISTRIBUTION_VALUES_FILTER);
    }
    if (myGroupsFilter != null) {
      myGroupsFilter.writeTo(config.getOrCreateSubset(ConfigNames.DISTRIBUTION_GROUPS_FILTER));
    } else {
      config.removeSubsets(ConfigNames.DISTRIBUTION_GROUPS_FILTER);
    }
    config.setSetting(ConfigNames.HIDE_EMPTY_CHILDREN, myHideEmptyQueries);
  }

  @NotNull
  public static DistributionParameters readConfig(ReadonlyConfiguration config) {
    String grouping = config.getSetting(ConfigNames.DISTRIBUTION_GROUPING, null);
    boolean arrange = config.getBooleanSetting(ConfigNames.DISTRIBUTION_ARRANGE_IN_GROUPS, true);
    StringFilterSet valuesFilter =
      StringFilterSet.readFrom(config.getSubset(ConfigNames.DISTRIBUTION_VALUES_FILTER));
    StringFilterSet groupsFilter =
      StringFilterSet.readFrom(config.getSubset(ConfigNames.DISTRIBUTION_GROUPS_FILTER));
    boolean hideEmpty = config.getBooleanSetting(ConfigNames.HIDE_EMPTY_CHILDREN, true);
    return new DistributionParameters(grouping, arrange, valuesFilter, groupsFilter, hideEmpty);
  }
}
