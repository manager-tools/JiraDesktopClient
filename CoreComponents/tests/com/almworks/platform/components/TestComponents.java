package com.almworks.platform.components;

/**
 * :todoc:
 *
 * @author sereda
 */
public class TestComponents {
  public static final Class[] CLASSES = {CompoOne.class, CompoTwo.class};
  public static final String[] CLASS_NAMES = buildClassNames(CLASSES);
  public static final String XML = buildDescriptor(CLASSES);

  private static String buildDescriptor(Class[] classes) {
    StringBuffer descriptor = new StringBuffer();
    descriptor.append("<library>\n");
    for (Class<?> aClass : classes)
      descriptor.append("  ").append("<component class=\"" + aClass.getName() + "\"/>\n");
    descriptor.append("</library>\n");
    return descriptor.toString();
  }

  private static String[] buildClassNames(Class[] classes) {
    String[] r = new String[classes.length];
    for (int i = 0; i < classes.length; i++)
      r[i] = classes[i].getName();
    return r;
  }
}
