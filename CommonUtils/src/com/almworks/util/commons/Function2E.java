package com.almworks.util.commons;

/**
 * @author dyoma
 */
public interface Function2E<A, B, R, E extends Exception> {
  R invoke(A a, B b) throws E;
}
