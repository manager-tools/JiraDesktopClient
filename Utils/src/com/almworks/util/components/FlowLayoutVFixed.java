package com.almworks.util.components;

import java.awt.*;

public class FlowLayoutVFixed extends FlowLayout {
  public FlowLayoutVFixed() {
  }

  public FlowLayoutVFixed(int align) {
    super(align);
  }

  public FlowLayoutVFixed(int align, int hgap, int vgap) {
    super(align, hgap, vgap);
  }

  @Override
  public Dimension preferredLayoutSize(Container target) {
    Dimension ps = super.preferredLayoutSize(target);
    // hack based on current state
    Dimension sz = target.getSize();
    if (sz == null || sz.width < 32) return ps;
    if (sz.width >= ps.width) return ps;
    int count = (ps.width + sz.width - 1) / sz.width;
    ps.height *= count;
    ps.height += count - 1;
    return ps;
  }
}
