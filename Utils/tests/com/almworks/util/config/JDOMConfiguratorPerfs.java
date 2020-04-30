package com.almworks.util.config;

import java.io.File;

public class JDOMConfiguratorPerfs {
  public static void main(String[] args) {
    try {
      for (int i = 0; i < 3; i++) {
        long time = System.currentTimeMillis();
        JDOMConfigurator jdc = new JDOMConfigurator(new File("c:\\config.xml"), 1200000);
        long spent = System.currentTimeMillis() - time;
        System.out.println("spent " + spent + "ms");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
