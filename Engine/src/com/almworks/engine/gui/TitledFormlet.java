package com.almworks.engine.gui;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.layout.WidthDrivenComponentAdapter;
import com.almworks.util.components.layout.WidthDrivenStackedCouple;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;

public class TitledFormlet extends WidthDrivenStackedCouple {
  private final Formlet myFormlet;
  private final ChangeListener myListener = new MyListener();

  public TitledFormlet(Formlet formlet, String title, int index) {
    this(new FormletTitle(formlet, title, index));
  }

  private TitledFormlet(FormletTitle title) {
    super(new WidthDrivenComponentAdapter(title), title.getFormlet().getContent());
    myFormlet = title.getFormlet();
  }

  private void attach(Lifespan lifespan) {
    myFormlet.getModifiable().addAWTChangeListener(lifespan, myListener);
    update();
  }

  private void update() {
    boolean collapsed = myFormlet.isCollapsed() && myFormlet.isCollapsible();
    boolean visible = myFormlet.isVisible();
    getComponent().setVisible(visible);
    myFormlet.getContent().getComponent().setVisible(visible && !collapsed);
  }

  protected JPanel createPanel() {
    return new MyPanel();
  }

  private class MyPanel extends JPanel {
    private final Lifecycle myLife = new Lifecycle(false);

    public void addNotify() {
      super.addNotify();
      myLife.cycleStart();
      attach(myLife.lifespan());
    }

    public void removeNotify() {
      myLife.cycleEnd();
      super.removeNotify();
    }
  }

  private class MyListener implements ChangeListener {
    public void onChange() {
      update();
    }
  }
}
