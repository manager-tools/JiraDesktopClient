package com.almworks.util.commons;

public abstract class Condition2 <T1, T2> {
  public abstract boolean isAccepted(T1 value1, T2 value2);

  public Function2<T1, T2, Boolean> fun() {
    return new Function2<T1, T2, Boolean>() {
      @Override
      public Boolean invoke(T1 t1, T2 t2) {
        return isAccepted(t1, t2);
      }
    };
  }
}
