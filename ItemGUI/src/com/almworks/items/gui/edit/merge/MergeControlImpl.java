package com.almworks.items.gui.edit.merge;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.ATable;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.ui.actions.ConstProvider;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.List;

class MergeControlImpl extends SimpleModifiable implements MergeControl {
  private static final String SETTING_FILTERED = "filtered";
  private static final Condition<MergeValue> FILTER = new Condition<MergeValue>() {
    @Override
    public boolean isAccepted(MergeValue value) {
      return value.isChangeOrConflict();
    }
  };

  private final FilteringListDecorator<MergeValue> myModel;
  private final Configuration myConfig;
  private final ATable<MergeValue> myTable;

  private MergeControlImpl(FilteringListDecorator<MergeValue> model, Configuration config, ATable<MergeValue> table) {
    myModel = model;
    myConfig = config;
    myTable = table;
  }

  @Override
  public boolean isFiltered() {
    return myModel.getFilter() != Condition.always();
  }

  @Override
  public void setFiltered(boolean filtered) {
    if (isFiltered() == filtered) return;
    myConfig.setSetting(SETTING_FILTERED, filtered);
    myModel.setFilter(getFilter(filtered));
    fireChanged();
  }

  @Override
  public Modifiable getSelectionModifiable() {
    return myTable.getSelectionAccessor();
  }

  @Override
  public List<MergeValue> getAllValues() {
    return Collections15.unmodifiableListCopy(myTable.getDataModel().toList());
  }

  @Override
  public Configuration getConfig(String subset) {
    return myConfig.getOrCreateSubset("utility").getOrCreateSubset(subset);
  }

  private static Condition<MergeValue> getFilter(boolean filtered) {
    return filtered ? FILTER : Condition.<MergeValue>always();
  }

  private static boolean isFiltered(ReadonlyConfiguration config) {
    return config.getBooleanSetting(SETTING_FILTERED, true);
  }

  public static void install(Lifespan life, final ATable<MergeValue> table, List<MergeValue> values, Configuration config) {
    AListModel<MergeValue> fixedModel = FixedListModel.create(values);
    Condition<MergeValue> filter = getFilter(isFiltered(config));
    final FilteringListDecorator<MergeValue> filteredModel = FilteringListDecorator.create(life, fixedModel, filter);
    table.setDataModel(filteredModel);
    final MergeControlImpl control = new MergeControlImpl(filteredModel, config, table);
    ConstProvider.addGlobalValue(table.getSwingComponent(), ROLE, control);
    table.addGlobalRoles(MergeValue.ROLE);
    ChangeListener listener = new ChangeListener() {
      @Override
      public void onChange() {
        filteredModel.resynch();
        control.fireChanged();
        table.repaint();
      }
    };
    for (MergeValue value : values) value.addChangeListener(life, listener);
  }
}
