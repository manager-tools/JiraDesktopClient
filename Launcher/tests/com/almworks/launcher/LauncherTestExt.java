package com.almworks.launcher;

/**
 * :todoc:
 *
 * @author sereda
 */
public class LauncherTestExt extends Launcher {
  static {
    // this class must be loaded by special class loader
    assert !LauncherTestExt.class.getClassLoader().equals(LauncherTest.class.getClassLoader());
  }

  LauncherTestExt(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new LauncherTestExt(args).run();
  }
}
