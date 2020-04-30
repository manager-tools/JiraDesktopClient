package com.almworks.util.tests;

import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Factory;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * @author : Dyoma
 */
public class MockProxy <T> implements InvocationHandler {
  private final String myName;
  private final T myObject;
  private final Class<T> myClass;
  private final Map<Method, MethodHandler> myHandlers = Collections15.hashMap();
  private boolean myHandleObjectMethods = false;

  public MockProxy(Class<T> aClass, String name) {
    myName = name;
    myClass = aClass;
    myObject = (T) Proxy.newProxyInstance(myClass.getClassLoader(), new Class[]{myClass}, this);
  }

  public static <T> MockProxy<T> create(Class<T> aClass, String name) {
    return new MockProxy<T>(aClass, name);
  }

  public T getObject() {
    return myObject;
  }

  public void putValue(String getterName, Object argument, Object value) {
    Object[] arguments = new Object[]{argument};
    putValue(getterName, arguments, value, false);
  }

  public <T> MockProxy<T> putProxyValue(String getterName, Class<T> proxyClass) {
    return putProxyValue(getterName, new Object[0], proxyClass);
  }

  public <X> MockProxy<X> putProxyValue(String getterName, Object argument, Class<X> proxyClass) {
    return putProxyValue(getterName, new Object[]{argument}, proxyClass);
  }

  public <X> MockProxy<X> putProxyValue(String getterName, Object[] arguments, Class<X> proxyClass) {
    MockProxy<X> valueProxy = create(proxyClass, myName + "." + getterName);
    putValue(getterName, arguments, valueProxy.getObject(), false);
    return valueProxy;
  }

  public void putValue(String getterName, Object[] arguments, Object value, boolean allowOverride) {
    Method getter = resolveMethod(getterName, arguments);
    List argumentsList = (List) Arrays.asList(arguments);
    if (getter == null)
      throw createCantResolve(getterName, arguments);
    if (value instanceof MockProxy)
      value = ((MockProxy) value).getObject();
    GetterHandler.addValue(myHandlers, getter, argumentsList, value, allowOverride);
  }

  public void consumeParametersTo(String methodName, List parameters1, List parameters2, Object value) {
    consumeParametersTo(methodName, new List[]{parameters1, parameters2}, value);
  }

  public void consumeParametersTo(String methodName, List parameters, Object value) {
    consumeParametersTo(methodName, new List[]{parameters}, value);
  }

  public void consumeParametersTo(String methodName, List[] parameters, Object value) {
    Method[] methods = myClass.getMethods();
    Method candidate = null;
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (!method.getName().equals(methodName))
        continue;
      Class returnType = method.getReturnType();
      if (returnType.equals(void.class) && value != null)
        continue;
      if (value != null && !returnType.isInstance(value))
        continue;
      if (method.getParameterTypes().length != parameters.length)
        continue;
      if (candidate != null)
        throw createException("Two candidates found:\n" + candidate + " and\n" + method);
      candidate = method;
    }
    if (candidate == null)
      throw createException("Can't resolve 'void " + methodName + "(" + parameters.length + " arguments)'");
    registerHandler(candidate, new ConsumeParametersHandler(parameters, value));
  }

  private void registerHandler(Method candidate, MethodHandler handler) {
    if (myHandlers.containsKey(candidate))
      throw createException("Handler already registered for " + candidate);
    myHandlers.put(candidate, handler);
  }

  private Method resolveMethod(String getterName, Object[] arguments) {
    Class[] types = new Class[arguments.length];
    for (int i = 0; i < arguments.length; i++) {
      Object argument = arguments[i];
      if (argument == null)
        continue;
      if (argument instanceof MockProxy)
        argument = ((MockProxy) argument).getObject();
      types[i] = argument.getClass();
    }
    return new MethodResolver(myClass, getterName).setArgumentValues(arguments).resolve();
  }

  private Method resolveMethod(String getterName, Class[] classes) {
    return new MethodResolver(myClass, getterName).setArgumentTypes(classes).resolve();
  }

  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    ThreadChecker.checkThread(method);
    if (OBJECT_toString.equals(method)) {
      return "Proxy: '" + myName + "'";
    }
    if (myHandleObjectMethods && method.getDeclaringClass() == Object.class) {
      if (OBJECT_equals.equals(method)) {
        assert args.length == 1;
        return Boolean.valueOf(proxy == args[0]);
      } else if (OBJECT_hashCode.equals(method)) {
        assert args == null || args.length == 0;
        return hashCode();
      }
      assert false: method.toString();
    }
    List argList = args != null ? Arrays.asList(args) : Collections15.emptyList();
    if (myHandlers.containsKey(method)) {
      return myHandlers.get(method).getValue(argList);
    }
    throw createException("Unknown method: " + method);
  }

  private RuntimeException createException(String message) {
    return new RuntimeException("'" + myName + "' (" + myClass + ")" + "\n" + message);
  }

  private RuntimeException createCantResolve(String methodName, Object[] arguments) {
    return createException("Can't resolve method: " + methodName + "(" + Arrays.asList(arguments) + ")");
  }

  public void putValue(String getterName, Object value) {
    Object[] arguments = new Object[0];
    putValue(getterName, arguments, value, false);
  }

  public MockProxy<T> handleObjectMethods() {
    myHandleObjectMethods = true;
    return this;
  }

  public void reputValue(String getterName, Object argument, Object value) {
    putValue(getterName, new Object[]{argument}, value, true);
  }

  public void putIterator(String getterName, Collection collection) {
    Object[] arguments = new Object[0];
    Method method = resolveMethod(getterName, arguments);
    if (method == null)
      throw createCantResolve(getterName, arguments);
    if (!method.getReturnType().isAssignableFrom(Iterator.class))
      throw createException("Return type should be Iterator. But was: " + method.getReturnType());
    registerHandler(method, new IterateHandler(collection));
  }

  public void putFactory(String methodName, Class argument1, Convertor convertor) {
    putFactory(methodName, new Class[]{argument1}, 0, convertor);
  }

  public void putFactory(String methodName, Class[] paramClasses, int paramIndex, Convertor convertor) {
    Method method = resolveMethod(methodName, paramClasses);
    if (method == null)
      throw createCantResolve(methodName, paramClasses);
    registerHandler(method, new ConvertorHandler(convertor, paramIndex));
  }

  public void putFactory(String methodName, final Factory factory) {
    Class[] argClasses = new Class[0];
    Method method = resolveMethod(methodName, argClasses);
    if (method == null)
      throw createCantResolve(methodName, argClasses);
    registerHandler(method, new MethodHandler() {
      public Object getValue(List arguments) {
        assert arguments != null;
        assert arguments.size() == 0 : arguments;
        return factory.create();
      }
    });
  }

  public HandlerBuilder method(String name) {
    return new HandlerBuilder(name);
  }

  public interface MethodHandler {
    Object getValue(List arguments);
  }

  private static class ConvertorHandler implements MethodHandler {
    private final Convertor myConvertor;
    private final int myArgumentIndex;

    public ConvertorHandler(Convertor convertor, int argumentIndex) {
      myConvertor = convertor;
      myArgumentIndex = argumentIndex;
    }

    public Object getValue(List arguments) {
      assert arguments.size() > myArgumentIndex : "Argument: " + myArgumentIndex + " params: " + arguments;
      return myConvertor.convert(arguments.get(myArgumentIndex));
    }
  }

  private static class IterateHandler implements MethodHandler {
    private final Collection myResult;

    public IterateHandler(Collection result) {
      myResult = result;
    }

    public Object getValue(List arguments) {
      assert arguments.size() == 0;
      return myResult.iterator();
    }
  }

  private static class ConsumeParametersHandler implements MethodHandler {
    private final List[] myParameters;
    private final Object myValue;

    public ConsumeParametersHandler(List[] parameters, Object value) {
      myParameters = parameters;
      myValue = value;
    }

    public Object getValue(List arguments) {
      assert myParameters.length == arguments.size() : "Expected: " + myParameters.length + " but was: " + arguments.size();
      for (int i = 0; i < arguments.size(); i++) {
        Object argument = arguments.get(i);
        List parametersHistory = myParameters[i];
        if (parametersHistory != null)
          parametersHistory.add(argument);
      }
      return myValue;
    }
  }

  private static class GetterHandler implements MethodHandler {
    private final Map<List, Object> myValues = Collections15.hashMap();

    public Object getValue(List arguments) {
      if (myValues.containsKey(arguments))
        return myValues.get(arguments);
      throw new RuntimeException("No value for " + arguments);
    }

    public static void addValue(Map<Method, MethodHandler> handlers, Method getter, List arguments, Object value,
                                                                   boolean allowOverride) {
      MethodHandler handler = handlers.get(getter);
      if (handler == null) {
        handler = new GetterHandler();
        handlers.put(getter, handler);
      }
      if (!(handler instanceof GetterHandler))
        throw new RuntimeException("Wrong handler registered: " + handler.toString());
      Map<List, Object> values = ((GetterHandler) handler).myValues;
      if (!allowOverride && values.containsKey(arguments))
        throw new RuntimeException("Return value already registered for " + arguments + " -> " + values.get(arguments));
      Class returnType = getter.getReturnType();
      if (returnType.isPrimitive()) {
        String str = returnType.toString();
        returnType = MethodResolver.PRIMITIVE_TO_OBJECT.get(returnType);
        assert returnType != null : str;
      }
      if (value != null && !returnType.isInstance(value))
        throw new RuntimeException("Wrong value type: " + value.getClass() + " expected: " + returnType);
      if (returnType.isPrimitive() && value == null)
        throw new RuntimeException("<null> isn't acceptable value for " + returnType);
      values.put(arguments, value);
    }
  }

  private static final Method OBJECT_toString;
  private static final Method OBJECT_equals;
  private static final Method OBJECT_hashCode;

  static {
    try {
      OBJECT_toString = Object.class.getMethod("toString", new Class[0]);
      OBJECT_equals = Object.class.getMethod("equals", new Class[]{Object.class});
      OBJECT_hashCode = Object.class.getMethod("hashCode", new Class[0]);
    } catch (NoSuchMethodException e) {
      throw new Failure("Should not happen");
    }
  }

  public class HandlerBuilder {
    private final MethodResolver myResolver;
    private Object myReturnValue = null;

    public HandlerBuilder(String methodName) {
      myResolver = new MethodResolver(myClass, methodName);
    }

    public HandlerBuilder parameter(int index, Class type) {
      myResolver.setArgumentType(index, type);
      return this;
    }

    public HandlerBuilder returning(Object value) {
      myReturnValue = value;
      myResolver.setReturnValue(myReturnValue);
      return this;
    }

    public HandlerBuilder returningType(Class type) {
      myResolver.setReturnType(type);
      return this;
    }

    public void consumeParametersTo(List params) {
      myResolver.setArgumentCount(1);
      perform(new ConsumeParametersHandler(new List[]{params}, myReturnValue));
    }

    public void consumeParametersTo(List params1, List params2) {
      myResolver.setArgumentCount(2);
      perform(new ConsumeParametersHandler(new List[]{params1, params2}, myReturnValue));
    }

    public HandlerBuilder paramsCount(int count) {
      myResolver.setArgumentCount(count);
      return this;
    }

    public void ignore() {
      Method method = myResolver.resolve();
      registerHandler(method, new ConsumeParametersHandler(new List[method.getParameterTypes().length], myReturnValue));
//      perform(new ConsumeParametersHandler(new List[0], myReturnValue));
    }

    public void perform(MethodHandler handler) {
      registerHandler(myResolver.resolve(), handler);
    }

    public void returnContains(final Collection changedArtifact, final boolean invert) {
      myResolver.setArgumentCount(1);
      myResolver.setReturnType(boolean.class);
      perform(new MethodHandler() {
        public Object getValue(List arguments) {
          assert arguments.size() == 1;
          return Boolean.valueOf(changedArtifact.contains(arguments.get(0)) ? !invert : invert);
        }
      });
    }
  }
}
