package com.almworks.util.collections;

/**
 * @author dyoma
 */
public interface Convertor2<T1, T2, R> {
  R convert(T1 arg1, T2 arg2);
}
