package com.almworks.actions.console;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Function2;
import org.almworks.util.detach.Lifespan;

public class SimpleModelFilter<T> implements Function<Lifespan, VariantModelController<T>> {
  private final AListModel<T> mySource;
  private final Function2<T, String, Boolean> myCondition;

  public SimpleModelFilter(AListModel<T> source, Function2<T, String, Boolean> condition) {
    mySource = source;
    myCondition = condition;
  }

  @Override
  public VariantModelController<T> invoke(Lifespan life) {
    MyVariants<T> variants = new MyVariants<T>(mySource, myCondition);
    variants.start(life);
    return variants;
  }

  private static class MyVariants<T> implements VariantModelController<T> {
    private final FilteringListDecorator<T> myFiltered;
    private final Function2<T, String, Boolean> myCondition;

    public MyVariants(AListModel<T> source, Function2<T, String, Boolean> condition) {
      myCondition = condition;
      myFiltered = FilteringListDecorator.createNotStarted(source);
    }

    @Override
    public void setText(String text) {
      if (myCondition != null) myFiltered.setFilter(new MyFilter<T>(text, myCondition));
    }

    @Override
    public AListModel<T> getVariants() {
      return myFiltered;
    }

    private void start(Lifespan life) {
      life.add(myFiltered.getDetach());
    }
  }

  private static class MyFilter<T> extends Condition<T> {
    private final String myText;
    private final Function2<T, String, Boolean> myCondition;

    public MyFilter(String text, Function2<T, String, Boolean> condition) {
      myText = text;
      myCondition = condition;
    }

    @Override
    public boolean isAccepted(T value) {
      return Boolean.TRUE.equals(myCondition.invoke(value, myText));
    }
  }

}
