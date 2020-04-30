package com.almworks.util.commons;

public interface FunctionE<A, R, E extends Exception> {
  R invoke(A argument) throws E;
}
