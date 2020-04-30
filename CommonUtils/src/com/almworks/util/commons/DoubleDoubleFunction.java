package com.almworks.util.commons;

public interface DoubleDoubleFunction {
  public double invoke(double arg);

  class Linear implements DoubleDoubleFunction {
    private final double a;
    private final double b;

    public Linear(double a, double b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public double invoke(double arg) {
      return a*arg + b;
    }
  }
}
