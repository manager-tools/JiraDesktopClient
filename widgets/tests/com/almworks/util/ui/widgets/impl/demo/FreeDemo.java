package com.almworks.util.ui.widgets.impl.demo;

import com.almworks.util.components.SizeCalculator1D;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.widgets.impl.HostComponentState;
import com.almworks.util.ui.widgets.impl.WidgetHostComponent;
import com.almworks.util.ui.widgets.util.ScrollPolicy;
import com.almworks.util.ui.widgets.util.ScrollWidget;

public class FreeDemo implements Runnable {
  @Override
  public void run() {
    WidgetHostComponent host = new WidgetHostComponent();
    HostComponentState<Node> state = host.createState();
    state.setWidget(ScrollWidget.wrap(new DemoWidget(), new ScrollPolicy.Fixed(SizeCalculator1D.fixedPixels(1), true), true));
    Node root = new Node.NodeRoot().getSelected();
    root.setSize(500, 500);
    state.setValue(root);
    host.setState(state);
    DebugFrame.show(host);
  }

  public static void main(String[] args) {
    ThreadGate.AWT.execute(new FreeDemo());
  }
}
