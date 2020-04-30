package com.almworks.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author : Dyoma
 */
public abstract class BaseInvocationHandler implements InvocationHandler{
  public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (Object.class.equals(method.getDeclaringClass())) {
      if ("equals".equals(method.getName())) {
        assert args.length == 1 : args.length;
        return proxy == args[0];
      }
      return method.invoke(BaseInvocationHandler.this, args);
    }
    return invokeTarget(method, args);
  }

  protected abstract Object invokeTarget(Method method, Object[] args);
}
