package com.almworks.util.ui;

import com.almworks.util.collections.Accessor;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: dyoma
 */
public class GraphicsConfig {
  private final Graphics2D myGraphics;
  private final Map<Accessor, Object> mySavedValues = new HashMap<Accessor, Object>();

  public GraphicsConfig(Graphics2D graphics) {
    myGraphics = graphics;
  }

  public void setStroke(Stroke stroke) {
    Accessor<Graphics2D, Stroke> accessor = STROKE;
    saveAndSetValue(accessor, stroke);
  }

  public void restoreAll() {
    for (Iterator<Map.Entry<Accessor, Object>> iterator = mySavedValues.entrySet().iterator(); iterator.hasNext();) {
      Map.Entry<Accessor, Object> entry = iterator.next();
      entry.getKey().setValue(myGraphics, entry.getValue());
    }
  }

  public void setRenderingHint(RenderingHints.Key key, Object value) {
    saveAndSetValue(new RenderingHintAccessor(key), value);
  }

  private <T> void saveAndSetValue(Accessor<Graphics2D, T> accessor, T stroke) {
    if (mySavedValues.containsKey(accessor)) return;
    mySavedValues.put(accessor, accessor.getValue(myGraphics));
    accessor.setValue(myGraphics, stroke);
  }

  private static class RenderingHintAccessor implements Accessor<Graphics2D, Object> {
    private final RenderingHints.Key myKey;

    public RenderingHintAccessor(RenderingHints.Key key) {
      if (key == null) throw new NullPointerException("key");
      myKey = key;
    }

    public Object getValue(Graphics2D g2) {
      return g2.getRenderingHint(myKey);
    }

    public void setValue(Graphics2D g2, Object value) {
      g2.setRenderingHint(myKey, value);
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof RenderingHintAccessor)) return false;
      RenderingHintAccessor other = (RenderingHintAccessor) obj;
      return myKey.equals(other.myKey);
    }

    public int hashCode() {
      return myKey.hashCode();
    }
  }

  private static final Accessor<Graphics2D, Stroke> STROKE = new Accessor<Graphics2D, Stroke>() {
    public Stroke getValue(Graphics2D g2) {
      return g2.getStroke();
    }

    public void setValue(Graphics2D g2, Stroke stroke) {
      g2.setStroke(stroke);
    }
  };
}
