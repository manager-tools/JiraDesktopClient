package com.almworks.util.xmlrpc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

public class SimpleOutgoingMessage extends OutgoingMessage {
  private final String myMethod;
  private final Vector myParameters;

  public SimpleOutgoingMessage(String method, Object ... objects) {
    this(method, buildVector(objects));
  }

  public SimpleOutgoingMessage(String method, Vector parameters) {
    myMethod = method;
    myParameters = parameters;
  }

  private static Vector buildVector(Object... objects) {
    Vector parameters = new Vector();
    if (objects != null) {
      parameters.addAll(Arrays.asList(objects));
    }
    return parameters;
  }

  protected String getRpcMethod() {
    return myMethod;
  }

  protected Collection<?> getRpcParameters() {
    return myParameters;
  }

  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    final SimpleOutgoingMessage that = (SimpleOutgoingMessage) o;

    if (!myMethod.equals(that.myMethod))
      return false;
    if (!myParameters.equals(that.myParameters))
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myMethod.hashCode();
    result = 29 * result + myParameters.hashCode();
    return result;
  }

  protected void requestFailed(Exception problem) {
    super.requestFailed(problem);
  }

  protected void responseError(String error, String payload) {
    super.responseError(error, payload);
  }
}
