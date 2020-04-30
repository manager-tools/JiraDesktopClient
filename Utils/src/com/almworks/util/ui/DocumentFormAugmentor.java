package com.almworks.util.ui;

import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.ADateField;
import com.almworks.util.components.BackgroundCanvasRenderer;
import com.almworks.util.components.plaf.macosx.Aqua;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class DocumentFormAugmentor {
  public static final ComponentProperty<Boolean> DO_NOT_AUGMENT = ComponentProperty.createProperty("DO_NOT_AUGMENT");
  private final Color myFormBackground;

  public DocumentFormAugmentor() {
    myFormBackground = backgroundColor();
  }

  public DocumentFormAugmentor(Color backgroundColor) {
    myFormBackground = backgroundColor;
  }

  public static Color backgroundColor() {
    return UIManager.getColor("EditorPane.background");
  }

  /**
   * Makes form augmented, i.e.:
   * - scrollable with no eye harm
   * - resembling web page or a document
   */
  public void augmentForm(Lifespan life, final JComponent parent, boolean scrollOnFocus) {
    assert parent != null;
//    augment(parent, 0);

    setupDescendantsOpaque(parent);

    if (scrollOnFocus)
      installFocusTracker(life, parent);
  }

  public void setupDescendantsOpaque(final JComponent parent) {
    assert parent != null;
    UIUtil.visitComponents(parent, JComponent.class, new ElementVisitor<JComponent>() {
      public boolean visit(JComponent component) {
        final boolean notMac = !Aqua.isAqua();
        if(!(component instanceof JButton
            || component instanceof JTextComponent)
          && !(component.getParent() instanceof BackgroundCanvasRenderer)
          && DO_NOT_AUGMENT.getClientValue(component) == null)
        {
          if(notMac) {
            component.setOpaque(component == parent);
            component.setBackground(myFormBackground);
          } else {
            // On Mac OS X many controls have "glowing" borders,
            // and setting opaqueness on them or their parents
            // leads to visible glitches, hence this check.
            if(!(component instanceof ADateField
                || component instanceof JComboBox
                || component instanceof AComboBox)) {
              component.setOpaque(component == parent);
              component.setBackground(myFormBackground);
            }
          }
        }
        return true;
      }
    });
  }


  private void installFocusTracker(Lifespan life, final JComponent parent) {
    assert life != Lifespan.FOREVER; // The following code adds listener to KeyboardFocusManager which is static. So if the life never ends the the component refered by parent won't ever be GCed.  
    final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    final PropertyChangeListener listener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if ("focusOwner".equals(evt.getPropertyName())) {
          Object value = evt.getNewValue();
          if (value instanceof JComponent) {
            JComponent component = ((JComponent) value);
            if (parent.isAncestorOf(component)) {
              scrollToVisible(component, parent);
            }
          }
        }
      }
    };
    focusManager.addPropertyChangeListener(listener);
    life.add(new Detach() {
      protected void doDetach() {
        focusManager.removePropertyChangeListener(listener);
      }
    });
  }

  /**
   * Scrolls component into view of superparent. If component is nested into several JViewports, tries
   * to scroll through all of them to make sure the component is visible.
   */
  private void scrollToVisible(JComponent component, JComponent superparent) {
    assert superparent.isAncestorOf(component);

    Container parent = component.getParent();
    Rectangle rect = component.getBounds();
    boolean scroll = true;

    while (parent != null) {
      if (scroll && parent instanceof JComponent)
        ((JComponent) parent).scrollRectToVisible(rect);

      if (parent instanceof JViewport) {
        // if this is view port, scroll its scroll pane into the view.
        parent = parent.getParent();
        if (parent == null)
          break;
        rect = new Rectangle(parent.getSize());
        scroll = true;
      } else {
        scroll = false;
      }

      rect.x += parent.getX();
      rect.y += parent.getY();

      if (parent == superparent)
        break;
      parent = parent.getParent();
    }
  }

  private void augment(Component component, int depth) {
    if (component instanceof JComponent && !(component instanceof JButton)) {
      ((JComponent) component).setOpaque(depth == 0);
    }

    if (component instanceof JPanel)
      augmentPanel((JPanel) component, depth == 0);

    if (component instanceof Container) {
      Component[] components = ((Container) component).getComponents();
      for (int i = 0; i < components.length; i++)
        augment(components[i], depth + 1);
    }
  }

  private void augmentPanel(JPanel panel, boolean isTop) {
    if (panel.isOpaque())
      panel.setBackground(myFormBackground);
  }
}
