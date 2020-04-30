package com.almworks.util.collections;

/**
 * @author dyoma
 */
public interface ElementVisitor2<A, B> {
  boolean visit(A a, B b);
}
