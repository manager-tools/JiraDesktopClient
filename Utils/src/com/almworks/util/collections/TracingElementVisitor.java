package com.almworks.util.collections;

/**
 * @author dyoma
 */
public interface TracingElementVisitor<E, T> {
  /**
   * @param parentTrace arbitrary object passed from visit() on a parent node
   * @see ElementVisitor
   * @return value to pass as parentTrace to visit() on child  
   */
  T visit(E element, T parentTrace) throws BreakVisitingException;
}
