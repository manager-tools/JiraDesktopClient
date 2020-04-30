package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.api.DP;
import com.almworks.items.impl.dbadapter.DBFilterInvalidException;
import com.almworks.items.impl.sqlite.TransactionContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class InjectionExtractionFactory implements ExtractionOperatorFactory {
  private final Class<? extends DP> myPredicateClass;
  private final Constructor<? extends ExtractionOperator> myConstructor;
  private final boolean myUse3;

  public InjectionExtractionFactory(Class<? extends DP> predicateClass,
    Class<? extends ExtractionOperator> operatorClass) throws NoSuchMethodException
  {
    myPredicateClass = predicateClass;
    Constructor<? extends ExtractionOperator> constructor;
    boolean use3 = false;
    try {
      constructor = operatorClass.getConstructor(myPredicateClass, boolean.class, TransactionContext.class);
      use3 = true;
    } catch (NoSuchMethodException e) {
      constructor = operatorClass.getConstructor(myPredicateClass, boolean.class);
    }
    myUse3 = use3;
    myConstructor = constructor;
  }

  public ExtractionOperator convert(DP predicate, boolean negated, TransactionContext transactionContext) {
    if (!myPredicateClass.isInstance(predicate))
      return null;
    try {
      if (myUse3) {
        return myConstructor.newInstance(predicate, negated, transactionContext);
      } else {
        return myConstructor.newInstance(predicate, negated);
      }
    } catch (InstantiationException e) {
      throw new DBFilterInvalidException(e);
    } catch (IllegalAccessException e) {
      throw new DBFilterInvalidException(e);
    } catch (InvocationTargetException e) {
      throw new DBFilterInvalidException(e);
    }
  }
}
