package com.almworks.util.threads;

import com.almworks.util.BaseInvocationHandler;
import com.almworks.util.exec.ImmediateThreadGate;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.ExceptionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author : Dyoma
 */
public class Marshaller<T> {
  private final Class<T> myTargetClass;
  private final ThreadGate myGate;

  public Marshaller(Class<T> targetInterface, ThreadGate gate) {
    assert targetInterface != null;
    assert targetInterface.isInterface();
    myTargetClass = targetInterface;
    myGate = gate;
  }

  public T marshal(T target) {
    return marshal(target, myGate);
  }

  public T marshal(T target, ThreadGate gate) {
    assert target != null;
    assert gate != null;
    return createMarshalled(target, myTargetClass, gate);
  }

  public static <T> Marshaller<T> create(Class<T> targetClass) {
    return new Marshaller<T>(targetClass, null);
  }

  public static <T> T createMarshalled(T target, Class<T> targetClass, final ThreadGate gate) {
    assert gate != null;
    MyBaseInvocationHandler<T> handler = new MyBaseInvocationHandler<T>(target) {
      protected Object invokeTarget(final Method method, final Object[] args) {
        Object result;
        if (gate instanceof ImmediateThreadGate) {
          result = ((ImmediateThreadGate) gate).compute(new Computable<Object>() {
            public Object compute() {
              try {
                return method.invoke(getTarget(), args);
              } catch (Exception e) {
                throw ExceptionUtil.wrapUnchecked(e);
              }
            }
          });
        } else {
          gate.execute(new Job() {
            public void perform() throws Exception {
              method.invoke(getTarget(), args);
            }
          });
          result = null;
        }
        //noinspection ConstantConditions
        return result;
      }
    };
    return (T) Proxy.newProxyInstance(targetClass.getClassLoader(), new Class[] {targetClass}, handler);
  }


  public static <T> boolean isTargetEqual(T marshalled, T other) {
    InvocationHandler invocationHandler = Proxy.getInvocationHandler(marshalled);
    if (!(invocationHandler instanceof MyBaseInvocationHandler))
      return false;
    MyBaseInvocationHandler handler = (MyBaseInvocationHandler) invocationHandler;
    return handler.getTarget().equals(other);
  }

  private static abstract class MyBaseInvocationHandler<T> extends BaseInvocationHandler {
    private final T myTarget;

    public MyBaseInvocationHandler(T target) {
      assert target != null;
      myTarget = target;
    }

    public T getTarget() {
      return myTarget;
    }
  }
}
