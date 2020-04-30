package com.almworks.util.text.layout;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.Failure;

import java.awt.*;
import java.lang.reflect.Constructor;

/**
 * @author : Dyoma
 */
public class ParagraphImplTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  public static final FontMetrics SAMPLE_FONT_METRICS;
  static {
    Class fontMetricsClass;
    FontMetrics fontMetrics;
    try {
      fontMetricsClass = Class.forName("sun.awt.font.FontDesignMetrics"); // 1.4
    } catch (ClassNotFoundException e) {
      try {
        fontMetricsClass = Class.forName("sun.font.FontDesignMetrics"); // 1.5
      } catch (ClassNotFoundException e1) {
        throw new Failure(e1);
      }
    }
    try {
      Constructor constructor = fontMetricsClass.getDeclaredConstructor(new Class[]{Font.class});
      constructor.setAccessible(true);
      fontMetrics = (FontMetrics) constructor.newInstance(new Object[]{new Font("Arial", Font.PLAIN, 12)});
    } catch (Exception e1) {
      throw new Failure(e1);
    }
    SAMPLE_FONT_METRICS = fontMetrics;
  }

  public void test() {
    FontMetrics metrics = SAMPLE_FONT_METRICS;
    ParagraphImpl paragraph = new ParagraphImpl("abcdef def", metrics);
    CHECK.singleElement("abcdef def", paragraph.getLines());
    assertEquals(metrics.stringWidth("abcdef def"), paragraph.getPixelWidth(), 0);
    paragraph.updatePixelWidth(metrics.stringWidth("abcdef"));
    CHECK.order(new String[]{"abcdef ", "def"}, paragraph.getLines());
    assertEquals(metrics.stringWidth("abcdef"), paragraph.getPixelWidth(), 0);
  }
}