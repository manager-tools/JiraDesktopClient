package com.almworks.util.commons;

/**
 * Created by IntelliJ IDEA.
 * User: dyoma
 * Date: Mar 25, 2010
 * Time: 4:54:51 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ObjInt2Procedure<T> {
  void invoke(T o, int a, int b);

  ObjInt2Procedure<IntProcedure2> CALL_PROCEDURE = new ObjInt2Procedure<IntProcedure2>() {
    @Override
    public void invoke(IntProcedure2 o, int a, int b) {
      o.invoke(a, b);
    }
  };
}
