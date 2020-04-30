package com.almworks.util.ui;

import com.almworks.util.Env;

import javax.swing.*;

/**
 * Possible scroll bar policies.
 * @author Pavel Zvyagin
 */
public enum ScrollBarPolicy {
  /** System-default policy (ALWAYS on Mac OS, AS_NEEDED otherwise). */
  DEFAULT(
    Env.isMac() ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED,
    Env.isMac() ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
  ),

  /** Never show the scroll bar. */
  NEVER(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, JScrollPane.VERTICAL_SCROLLBAR_NEVER),

  /** Show the scroll bar as needed. */
  AS_NEEDED(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED),

  /** Always show the scroll bar. */
  ALWAYS(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

  /** Corresponding Swing horizontal scroll bar policy constant. */
  private final int hPolicy;

  /** Corresponding Swing vertical scroll bar policy constant. */
  private final int vPolicy;

  ScrollBarPolicy(int hPolicy, int vPolicy) {
    this.hPolicy = hPolicy;
    this.vPolicy = vPolicy;
  }

  /**
   * Set this policy as the horizontal scroll bar policy
   * for the given scroll pane.
   * @param pane The scroll pane.
   */
  public void setHorizontal(JScrollPane pane) {
    pane.setHorizontalScrollBarPolicy(hPolicy);
  }

  /**
   * Set this policy as the vertical scroll bar policy
   * for the given scroll pane.
   * @param pane The scroll pane.
   */
  public void setVertical(JScrollPane pane) {
    pane.setVerticalScrollBarPolicy(vPolicy);
  }

  /**
   * Set this policy as both the horizontal and
   * the vertical scroll bar policy for the given
   * scroll pane.
   * @param pane The scroll pane.
   */
  public void setBoth(JScrollPane pane) {
    pane.setHorizontalScrollBarPolicy(hPolicy);
    pane.setVerticalScrollBarPolicy(vPolicy);
  }

  /**
   * Set default vertical and horizontal policies
   * for the given scroll pane.
   * @param pane The scroll pane.
   */
  public static void setDefaults(JScrollPane pane) {
    DEFAULT.setBoth(pane);
  }

  /**
   * Set the default vertical policy and the given
   * horizontal policy for the given scroll pane.
   * @param pane The scroll pane.
   * @param hPolicy The horizontal policy.
   */
  public static void setDefaultWithHorizontal(JScrollPane pane, ScrollBarPolicy hPolicy) {
    hPolicy.setHorizontal(pane);
    DEFAULT.setVertical(pane);
  }

  /**
   * Set the default horizontal policy and the given
   * vertical policy for the given scroll pane.
   * @param pane The scroll pane.
   * @param vPolicy The vertical policy.
   */
  public static void setDefaultWithVertical(JScrollPane pane, ScrollBarPolicy vPolicy) {
    DEFAULT.setHorizontal(pane);
    vPolicy.setVertical(pane);
  }

  /**
   * Set scroll bar policies for the given scroll pane.
   * @param pane The scroll pane.
   * @param hPolicy The horizontal policy.
   * @param vPolicy The vertical policy.
   */
  public static void setPolicies(JScrollPane pane, ScrollBarPolicy hPolicy, ScrollBarPolicy vPolicy) {
    hPolicy.setHorizontal(pane);
    vPolicy.setVertical(pane);
  }
}
