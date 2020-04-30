package com.almworks.util;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface RunnableRE <R, E extends Exception> {
  R run() throws E;
}
