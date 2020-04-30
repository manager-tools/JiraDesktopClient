package com.almworks.sumtable;

import com.almworks.api.application.NameResolver;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.util.advmodel.*;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.events.DetachableValue;
import com.almworks.util.exec.Context;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

class AxisConfigurationModelHolder implements ChangeListener {
  private final AxisConfiguration myConfiguration;
  private final ComboBoxModelHolder<AxisDefinition> myOptions = ComboBoxModelHolder.create();
  private final DetachableValue<QueryResult> myQueryResult = DetachableValue.create();
  private final Lifecycle myContextLife = new Lifecycle();

  public AxisConfigurationModelHolder(AxisConfiguration configuration) {
    myConfiguration = configuration;
  }

  public ComboBoxModelHolder<AxisDefinition> getModel() {
    return myOptions;
  }

  public void attach(final Lifespan lifespan, QueryResult queryResult) {
    myQueryResult.set(lifespan, queryResult);
    lifespan.add(myContextLife.getAnyCycleDetach());
    queryResult.addAWTChangeListener(lifespan, this);
    onChange();
  }

  public void onChange() {
    myContextLife.cycle();
    QueryResult queryResult = myQueryResult.get();
    if (queryResult == null)
      return;
    ItemHypercube hypercube = queryResult.getHypercube(false);
    if (hypercube == null)
      hypercube = new ItemHypercubeImpl();
    NameResolver nameResolver = Context.require(NameResolver.ROLE);
    Lifespan life = myContextLife.lifespan();
    AListModel<ConstraintDescriptor> m1 = nameResolver.getConstraintDescriptorModel(life, hypercube);
    AListModel<AxisDefinition> m2 = FilteringConvertingListDecorator.create(life, m1, IsEnum.INSTANCE, CreateEnumAxis.INSTANCE);
    SortedListDecorator<AxisDefinition> m3 = SortedListDecorator.create(life, m2, AxisDefinition.COMPARATOR);
    SelectionInListModel<AxisDefinition> comboModel = SelectionInListModel.create(life, m3, null);
    makeInitialSelection(comboModel);
    myOptions.setModel(comboModel);
    myOptions.addSelectionChangeListener(life, new SelectionSynchronizer(life, true));
    myConfiguration.getDefinitionModifiable().addAWTChangeListener(life, new SelectionSynchronizer(life, false));
  }

  @ThreadAWT
  private void makeInitialSelection(final SelectionInListModel<AxisDefinition> comboModel) {
    Threads.assertAWTThread();
    final AxisDefinition definition = myConfiguration.getAxisDefinition();
    if (definition == null)
      return;
    int k = comboModel.indexOf(definition);
    if (k >= 0) {
      comboModel.setSelectedItem(definition);
      return;
    }
    final DetachComposite detach = new DetachComposite(true);
    myContextLife.lifespan().add(detach);
    detach.add(comboModel.addListener(new AListModel.Adapter() {
      public void onInsert(int index, int length) {
        int k = comboModel.indexOf(definition, index, index + length);
        if (k >= 0) {
          detach.detach();
          comboModel.setSelectedItem(definition);
        }
      }
    }));
    myOptions.addSelectionChangeListener(detach, new ChangeListener() {
      public void onChange() {
        detach.detach();
      }
    });
  }

  private class SelectionSynchronizer implements ChangeListener {
    private boolean myDispatching = false;

    private final Lifespan myLifespan;
    private final boolean myToConfiguration;

    public SelectionSynchronizer(Lifespan lifespan, boolean toConfiguration) {
      myLifespan = lifespan;
      myToConfiguration = toConfiguration;
    }

    public void onChange() {
      if (!myDispatching && !myLifespan.isEnded()) {
        myDispatching = true;
        try {
          if (myToConfiguration) {
            myConfiguration.setAxisDefinition(myOptions.getSelectedItem());
          } else {
            myOptions.setSelectedItem(myConfiguration.getAxisDefinition());
          }
        } finally {
          myDispatching = false;
        }
      }
    }
  }


  private static class IsEnum extends Condition<ConstraintDescriptor> {
    private static final Condition<ConstraintDescriptor> INSTANCE = new IsEnum();

    public boolean isAccepted(ConstraintDescriptor value) {
      return value.getType() instanceof EnumConstraintType;
    }
  }


  private static class CreateEnumAxis extends Convertor<ConstraintDescriptor, AxisDefinition> {
    private static final Convertor<ConstraintDescriptor, AxisDefinition> INSTANCE = new CreateEnumAxis();

    public AxisDefinition convert(ConstraintDescriptor value) {
      return new EnumAxisDefinition(value);
    }
  }
}
