package com.almworks.gui;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.gui.FrameBuilder;
import com.almworks.util.Env;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.FilteringFocusTraversalPolicy;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.globals.GlobalDataRoot;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import java.awt.*;

/**
 * @author : Dyoma
 */
class FrameBuilderImpl extends BasicWindowBuilderImpl<FrameBuilderImpl.MyFrame> implements FrameBuilder {
  private Dimension myPreferedSize;
  private boolean myResizable = true;

  public FrameBuilderImpl(MutableComponentContainer container, Configuration configuration) {
    super(container, configuration);
  }

  protected MyFrame createWindow(final Detach disposeNotification, boolean addRootPaneDataRoot) {
    final MyFrame frame = new MyFrame();
    frame.setFocusTraversalPolicy(new FilteringFocusTraversalPolicy());    
    frame.addToDispose(disposeNotification);
    frame.setTitle(getTitle());
    frame.setResizable(myResizable);

    if (addRootPaneDataRoot) {
      GlobalDataRoot.install(frame.getRootPane());
    }

    return frame;
  }

  public void setPreferredSize(Dimension size) {
    myPreferedSize = size;
  }

  @Override
  public boolean isModal() {
    return false;
  }

  protected Dimension getPreferredSize() {
    return myPreferedSize;
  }

  public void insertContent(MyFrame window, UIComponentWrapper content) {
    window.getContentPane().add(content.getComponent(), BorderLayout.CENTER);
    window.addToDispose(new UIComponentWrapper.Disposer(content));
  }

  @Override
  public void setResizable(boolean resizable) {
    myResizable = resizable;
  }

  static class MyFrame extends JFrame {
    private final DetachComposite myDetach = new DetachComposite();

    public MyFrame() throws HeadlessException {
      super(getDisplay());
      if(!Env.isMac()) {
        setIconImage(Icons.APPLICATION_LOGO_ICON_BIG.getImage());
      }
    }

    private static GraphicsConfiguration getDisplay() {
      Window window = UIUtil.getDefaultDialogOwner();
      if (window != null)
        return window.getGraphicsConfiguration();
      else
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    public void addToDispose(Detach detach) {
      myDetach.add(detach);
    }

    public void dispose() {
      boolean disposed = false;
      try {
        super.dispose();
        disposed = true;
      } finally {
        if (!disposed) {
          Log.error("Could not dispose of the frame, hiding it");
          setVisible(true);
        }
        myDetach.detach();
        getContentPane().removeAll();
        DataProvider.DATA_PROVIDER.removeAllProviders(getRootPane());
      }
    }
  }
}