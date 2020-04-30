package com.almworks.util.ui;

import com.almworks.util.tests.BaseTestCase;

import java.awt.*;

/**
 * @author Vasya
 */
public class ColorUtilTests extends BaseTestCase{
  public void testColorFormatting() {
    assertEquals("#FFFFFF", ColorUtil.formatColor(Color.WHITE));
    assertEquals("#000000", ColorUtil.formatColor(Color.BLACK));
  }

  public void testColorWithoutAlpha() {
    assertEquals("#FFFFFF", ColorUtil.formatColor(new Color(1.0F, 1.0F, 1.0F, 0.01F)));
  }
}
