package com.almworks.sumtable;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.NameResolver;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.UnresolvedNameException;
import com.almworks.api.application.qb.ConstraintFilterNode;
import com.almworks.api.application.qb.ConstraintType;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.explorer.qbuilder.filter.FilterGramma;
import com.almworks.items.api.DP;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.config.Configuration;
import com.almworks.util.events.DetachableValue;
import com.almworks.util.exec.Context;
import com.almworks.util.text.parser.FormulaWriter;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class CounterConfiguration {
  private static final String COUNTER_SUBSET = "counter";
  private static final String COUNTER_NAME = "name";
  private static final String COUNTER_FORMULA = "formula";
  private static final String SORTER_INDEX = "sorter";

  private final OrderListModel<STFilter> myModel = OrderListModel.create();
  private Configuration myConfig;
  private final SimpleModifiable mySorterModifiable = new SimpleModifiable();

  private final DetachableValue<QueryResult> myQueryResult = DetachableValue.create();

  private int mySorterIndex;

  public CounterConfiguration(Configuration config) {
    myConfig = config;
  }

  public void changeConfig(Configuration config) {
    myConfig = config;
  }

  public void attach(Lifespan lifespan, QueryResult queryResult) {
    myQueryResult.set(lifespan, queryResult);
    load(queryResult);
  }

  public AListModel<STFilter> getCounterModel() {
    return myModel;
  }

  public void move(Collection<STFilter> counters, int indexIncrement) {
    if (indexIncrement == 0)
      return;
    int[] indexes = myModel.indeciesOf(counters);
    int length = indexes.length;
    if (length == 0)
      return;
    Arrays.sort(indexes);
    boolean dir = indexIncrement < 0;
    for (int i = 0; i < length; i++) {
      int ii = dir ? i : length - i - 1;
      int k = indexes[ii];
      if (k != -1) {
        int m = k + indexIncrement;
        if (m >= 0 && m < myModel.getSize()) {
          myModel.swap(k, m);
        }
      }
    }
    save();
  }

  public void remove(Collection<STFilter> counters) {
    if (counters == null)
      return;
    myModel.removeAll(counters);
    save();
  }

  public void configureCounterName(Lifespan lifespan, final STFilter counter, final JTextComponent nameComponent) {
    nameComponent.setText(counter.getName());
    lifespan.add(UIUtil.addFocusListener(nameComponent, new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        String name = nameComponent.getText().trim();
        counter.setName(name);
        myModel.updateElement(counter);
        save();
      }
    }));
  }

  public void configureCounterLabel(Lifespan lifespan, final STFilter counter, final AbstractButton useIcon,
    final AbstractButton useText, AbstractButton iconAutoColor, final AbstractButton iconSelectedColor,
    final AbstractButton iconByFilter, AbstractButton textAuto, final AbstractButton textSpecified,
    final JTextComponent textSpecifiedEditor)
  {
    STFilterFormat format = counter.getFormat();
    useIcon.setSelected(format.isUseIcon());
    useText.setSelected(format.isUseLabel());
    int iconMode = format.getIconMode();
    iconAutoColor.setSelected(iconMode == STFilterFormat.ICON_MODE_AUTO_COLOR);
    iconSelectedColor.setSelected(iconMode == STFilterFormat.ICON_MODE_SELECTED_COLOR);
    iconByFilter.setSelected(iconMode == STFilterFormat.ICON_MODE_BY_FILTER);
    int labelMode = format.getLabelMode();
    textAuto.setSelected(labelMode == STFilterFormat.LABEL_MODE_AUTO);
    textSpecified.setSelected(labelMode == STFilterFormat.LABEL_MODE_SPECIFIED);
    textSpecifiedEditor.setText(format.getSpecifiedLabel());

    final ChangeListener listener = new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        int iconMode;
        if (iconSelectedColor.isSelected())
          iconMode = STFilterFormat.ICON_MODE_SELECTED_COLOR;
        else if (iconByFilter.isSelected())
          iconMode = STFilterFormat.ICON_MODE_BY_FILTER;
        else
          iconMode = STFilterFormat.ICON_MODE_AUTO_COLOR;
        int labelMode;
        if (textSpecified.isSelected())
          labelMode = STFilterFormat.LABEL_MODE_SPECIFIED;
        else
          labelMode = STFilterFormat.LABEL_MODE_AUTO;
        STFilterFormat format = new STFilterFormat(useIcon.isSelected(), iconMode, null, useText.isSelected(),
          labelMode, textSpecifiedEditor.getText());
        counter.setFormat(format);
        myModel.updateElement(counter);
        save();
      }
    };

    UIUtil.addChangeListeners(lifespan, listener, useIcon, useText, iconAutoColor, iconSelectedColor, iconByFilter,
      textAuto, textSpecified);
    DocumentUtil.addListener(lifespan, textSpecifiedEditor.getDocument(), new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        listener.stateChanged(null);
      }
    });
  }

  public void updatedCounter(STFilter counter) {
    myModel.updateElement(counter);
    save();
  }

  private void save() {
    myConfig.clear();
    for (STFilter counter : myModel.toList()) {
      Configuration config = myConfig.createSubset(COUNTER_SUBSET);
      config.setSetting(COUNTER_NAME, counter.getName());
      config.setSetting(COUNTER_FORMULA, counter.getFormula());
      counter.getFormat().saveTo(config);
    }
    if (mySorterIndex != 0) {
      myConfig.setSetting(SORTER_INDEX, mySorterIndex);
    }
  }

  private void load(QueryResult queryResult) {
    myModel.clear();
    ItemHypercube hypercube = queryResult.getEncompassingHypercube();
    List<Configuration> subsets = myConfig.getAllSubsets(COUNTER_SUBSET);
    for (Configuration config : subsets) {
      String name = config.getSetting(COUNTER_NAME, null);
      if (name == null)
        continue;
      String formula = config.getSetting(COUNTER_FORMULA, null);
      if (formula == null)
        continue;
      FilterNode filterNode;
      try {
        filterNode = FilterGramma.parse(formula);
      } catch (ParseException e) {
        Log.warn("cannot parse [" + formula + "]", e);
        filterNode = null;
      }
      BoolExpr<DP> filter = createFilter(filterNode, hypercube);
      STFilter counter = new STFilter(name, formula, filterNode, filter);
      counter.setIcon(getIcon(filterNode, hypercube));
      STFilterFormat format = STFilterFormat.loadFrom(config);
      if (format != null) {
        counter.setFormat(format);
      }
      myModel.addElement(counter);
    }
    mySorterIndex = myConfig.getIntegerSetting(SORTER_INDEX, 0);
    if (myModel.getSize() == 0) {
      addNewCounter("All", FilterNode.ALL_ITEMS, -1);
    }
  }

  public STFilter addNewCounter(String counterName, FilterNode filterNode, int afterIndex) {
    QueryResult queryResult = myQueryResult.get();
    if (queryResult == null) {
      assert false : this;
      return STFilter.TOTAL;
    }
    ItemHypercube hypercube = queryResult.getEncompassingHypercube();
    BoolExpr<DP> filter = createFilter(filterNode, hypercube);
    STFilter counter = new STFilter(counterName, FormulaWriter.write(filterNode), filterNode, filter);
    counter.setIcon(getIcon(filterNode, hypercube));

    if (afterIndex < 0) {
      myModel.addElement(counter);
    } else {
      myModel.insert(afterIndex + 1, counter);
    }
    save();
    return counter;
  }

  @Nullable
  private static Icon getIcon(FilterNode filterNode, ItemHypercube hypercube) {
    Icon icon = null;
    if (filterNode instanceof ConstraintFilterNode) {
      ConstraintFilterNode cfn = ((ConstraintFilterNode) filterNode);
      ConstraintType type = cfn.getType();
      if (type instanceof EnumConstraintType) {
        List<ItemKey> keys = cfn.getValue(EnumConstraintType.SUBSET);
        if (keys != null && keys.size() == 1) {
          List<ResolvedItem> resolvedArtifacts = ((EnumConstraintType) type).resolveKey(keys.get(0).getId(), hypercube);
          if (resolvedArtifacts != null && resolvedArtifacts.size() == 1) {
            icon = resolvedArtifacts.get(0).getIcon();
          }
        }
      }
    }
    return icon;
  }

  @Nullable
  private BoolExpr<DP> createFilter(FilterNode filterNode, ItemHypercube hypercube) {
    BoolExpr<DP> filter = null;
    if (filterNode != null) {
      filterNode.normalizeNames(Context.require(NameResolver.ROLE), hypercube);
      try {
        filter = filterNode.createFilter(hypercube);
      } catch (UnresolvedNameException e) {
        Log.warn("cannot resolve " + e.getMessage());
      }
    }
    return filter;
  }

  public int getSorterIndex() {
    if (mySorterIndex < 0 || mySorterIndex >= myModel.getSize())
      return 0;
    else
      return mySorterIndex;
  }

  public void setSortingCounter(STFilter counter) {
    int index = myModel.indexOf(counter);
    if (index < 0)
      index = 0;
    mySorterIndex = index;
    save();
    mySorterModifiable.fireChanged();
  }

  public Modifiable getSorterModifiable() {
    return mySorterModifiable;
  }
}
