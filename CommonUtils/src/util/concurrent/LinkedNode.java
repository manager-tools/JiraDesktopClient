/*
  File: LinkedNode.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  11Jun1998  dl               Create public version
  25may2000  dl               Change class access to public
  26nov2001  dl               Added no-arg constructor, all public access.
*/

package util.concurrent;

/**
 * A standard linked list node used in various queue classes *
 */
public class LinkedNode <T> {
  public T value;
  public LinkedNode<T> next;

  public LinkedNode() {
  }

  public LinkedNode(T x) {
    value = x;
  }

  public LinkedNode(T x, LinkedNode n) {
    value = x;
    next = n;
  }
}
