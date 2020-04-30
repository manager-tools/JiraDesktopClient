package com.almworks.util.components.renderer;

import com.almworks.util.commons.Function;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TextCell;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;

public class CellLinesRendererDemo implements Runnable, ActionListener {
  private static final TypedKey<String> BLABLA = TypedKey.create("BLABLA");

  private final Timer myTimer = new Timer(2000, this);
  private final Lifecycle myTimerCycle = new Lifecycle();

  private RendererHostComponent myHost;

  public static void main(String[] args) throws InvocationTargetException, InterruptedException {
    LAFUtil.initializeLookAndFeel();
    SwingUtilities.invokeAndWait(new CellLinesRendererDemo());
  }


  public void run() {
    JPanel panel = new JPanel(new BorderLayout(0, 5));
    panel.setBorder(UIUtil.BORDER_5);

    panel.add(new JButton("button-up"), BorderLayout.NORTH);
    panel.add(new JButton("button-down"), BorderLayout.SOUTH);

    myHost = new RendererHostComponent();
    TableRenderer renderer = new TableRenderer();

    Function<RendererContext, String> f1 = new Function<RendererContext, String>() {
      public String invoke(RendererContext argument) {
        return Util.NN(argument.getValue(BLABLA));
      }
    };
    Function<RendererContext, String> f2 = new Function<RendererContext, String>() {
      public String invoke(RendererContext argument) {
        String s = Util.NN(argument.getValue(BLABLA));
        return s + "+" + s;
      }
    };

    renderer.addLine("AttributeColumn Number 1", new TextCell(FontStyle.BOLD, f1));
    renderer.addLine("Another line that shows the same number", new TextCell(FontStyle.BOLD, f1));
    renderer.addLine("plus +", new TextCell(FontStyle.PLAIN, f2));

    myHost.setRenderer(renderer);
    panel.add(myHost);

    new DocumentFormAugmentor().augmentForm(Lifespan.FOREVER, panel, true);
    DebugFrame.show(panel, 500, 400);

    myTimer.setRepeats(true);
    myTimer.start();
  }


  public void actionPerformed(ActionEvent e) {
    myTimerCycle.cycle();
    myHost.putValue(myTimerCycle.lifespan(), BLABLA, String.valueOf(System.currentTimeMillis()));
    myHost.invalidateRenderer();
  }
}
