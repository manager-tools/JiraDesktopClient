package com.almworks.sumtable;

import com.almworks.api.application.tree.QueryResult;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.config.Configuration;
import org.almworks.util.detach.Lifespan;

public class SummaryTableConfiguration {
  private final AxisConfiguration myColumnsConfiguration;
  private final AxisConfiguration myRowsConfiguration;
  private final CounterConfiguration myCounterConfiguration;
  private Configuration myConfig;

  public SummaryTableConfiguration(Configuration configuration) {
    myConfig = configuration;
    myColumnsConfiguration = new AxisConfiguration(myConfig.getOrCreateSubset("columns"));
    myRowsConfiguration = new AxisConfiguration(myConfig.getOrCreateSubset("rows"));
    myCounterConfiguration = new CounterConfiguration(myConfig.getOrCreateSubset("counters"));
  }

  public void changeConfig(Configuration configuration) {
    myConfig = configuration;
    myColumnsConfiguration.changeConfig(myConfig.getOrCreateSubset("columns"));
    myRowsConfiguration.changeConfig(myConfig.getOrCreateSubset("rows"));
    myCounterConfiguration.changeConfig(myConfig.getOrCreateSubset("counters"));
  }

  public void attach(Lifespan lifespan, QueryResult queryResult) {
    myColumnsConfiguration.attach(lifespan, queryResult);
    myRowsConfiguration.attach(lifespan, queryResult);
    myCounterConfiguration.attach(lifespan, queryResult);
  }

  public AxisConfiguration getColumnsConfiguration() {
    return myColumnsConfiguration;
  }

  public AxisConfiguration getRowsConfiguration() {
    return myRowsConfiguration;
  }

  public CounterConfiguration getCounterConfiguration() {
    return myCounterConfiguration;
  }

  public void addSortingListener(Lifespan lifespan, ChangeListener changeListener) {
    myColumnsConfiguration.getSortingModifiable().addAWTChangeListener(lifespan, changeListener);
    myRowsConfiguration.getSortingModifiable().addAWTChangeListener(lifespan, changeListener);
    myCounterConfiguration.getSorterModifiable().addAWTChangeListener(lifespan, changeListener);
  }
}
