package com.almworks.engine.gui.attachments;

import com.almworks.api.config.MiscConfig;
import com.almworks.engine.gui.attachments.content.ContentView;
import com.almworks.engine.gui.attachments.content.ContentViewFactory;
import com.almworks.util.components.AActionButton;
import com.almworks.util.components.PlaceHolder;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileActions;
import com.almworks.util.files.FileUtil;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;

public class AttachmentContent implements UIComponentWrapper {
  private static final MessageFormat CANNOT_LOAD_FILE = new MessageFormat("Cannot load file: {1}", Locale.US);
  private static final Object THREAD_GATE_KEY = new Object();

  private static final ContentViewFactory NO_FILE_FACTORY = new ContentViewFactory() {
    public ContentView create(FileData data) {
      return new DisabledContentView(createErrorComponent("No file selected."));
    }
  };

  private static Paint ourErrorPaint = null;

  private final ContentViewFactory myFactory;
  private final PlaceHolder myPlaceHolder = new PlaceHolder();
  private final File myFile;
  private final String myMimeType;
  
  private ContentView myContentView;
  private FileData myData;
  private Listener myListener = null;

  public AttachmentContent(File file, String mimeType) {
    myFile = file;
    if (mimeType == null || mimeType.length() == 0)
      mimeType = guessMimeType(file);
    myMimeType = mimeType;
    myFactory = createFactory(file, mimeType);
    if (myFile.exists()) {
      showLoading();
    }
  }

  private void showLoading() {
    JLabel loading = new JLabel("Loading file\u2026");
    loading.setOpaque(true);
    loading.setBackground(UIUtil.getEditorBackground());
    loading.setHorizontalAlignment(SwingUtilities.CENTER);
    loading.setPreferredSize(UIUtil.getRelativeDimension(loading, 20, 15));
    myPlaceHolder.show(new JScrollPane(ScrollablePanel.adapt(loading)));
  }

  public void dispose() {
    myPlaceHolder.clear();
    myData = null;
    myListener = null;
  }

  // (MOU,2008-05-18) Too many injections. Suggest postpone
  private static ContentViewFactory createFactory(File file, String mimeType) {
    if (FileUtil.isEmpty(file))
      return NO_FILE_FACTORY;
    ContentViewFactory factory = ContentViewFactory.getFactoryByMimeType(mimeType);
    return factory;
  }

  public static String guessMimeType(File file) {
    if (file == null)
      return null;
    String name = file.getName();
    if (name.length() == 0)
      return null;
    return FileUtil.guessMimeType(name);
  }


  public JComponent getComponent() {
    return myPlaceHolder;
  }

  public void copyToClipboard() {
    ContentView view = myContentView;
    if (view != null)
      view.copyToClipboard();
  }

  public void loadFile() {
    ThreadGate.LONG_QUEUED(THREAD_GATE_KEY).execute(new Runnable() {
      public void run() {
        loadFileAndShowComponent();
      }
    });
  }

  private void loadFileAndShowComponent() {
    JComponent component;
    try {
      if (!FileUtil.isEmpty(myFile)) {
        checkFileSize();
        byte[] bytes = FileUtil.loadFile(myFile);
        myData = new FileData(myFile, myMimeType, bytes);
      } else {
        myData = new FileData(myFile, myMimeType, new byte[] {});
      }
      myContentView = createView();
      component = myContentView.getComponent();
    } catch (IOException e) {
      component = createErrorComponent(e, myFile);
      myData = null;
    }
    final JComponent content = component;
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        myPlaceHolder.show(content);
        myPlaceHolder.invalidate();
        myPlaceHolder.revalidate();
        myPlaceHolder.repaint();
        onLoaded();
      }
    });
  }

  private void checkFileSize() throws IOException {
    if (myFactory != null)
      myFactory.checkFileSize(myFile);
  }

  private ContentView createView() throws IOException {
    if (myFactory != null)
      return myFactory.create(myData);
    else
      return createUndisplayableFileView(myData);
  }

  private static JComponent createErrorComponent(IOException e, File file) {
    String message = CANNOT_LOAD_FILE.format(new Object[] {file.getAbsolutePath(), e.getMessage()});
    return createErrorComponent(message);
  }

  private void onLoaded() {
    Listener listener = getListener();
    if (listener != null)
      listener.onLoaded(myData, myContentView);
  }

  private ContentView createUndisplayableFileView(final FileData data) {
    JPanel panel = new JPanel() {
      protected void paintComponent(Graphics g) {
        AwtUtil.applyRenderingHints(g);
        UIUtil.fillPaint(g, this, getErrorPaint(), false);
        super.paintComponent(g);
      }
    };
    panel.setOpaque(false);
    panel.setLayout(new SingleChildLayout());

    JPanel content = new JPanel(UIUtil.createBorderLayout());
    content.setOpaque(false);
    String text = "Cannot display file";
    if (data.getMimeType() != null)
      text += " (type " + data.getMimeType() + ")";
    text += ".";
    content.add(new JLabel(text), BorderLayout.CENTER);
    if (FileActions.isSupported(FileActions.Action.OPEN_AS)) {
      AActionButton button = new AActionButton(AttachmentUtils.createOpenWithAction(data.getFile(), myPlaceHolder));
      button.setContextComponent(myPlaceHolder);
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          DataProvider.DATA_PROVIDER.putClientValue(myPlaceHolder, ConstProvider.singleData(FileData.FILE_DATA, data));
        }
      });
      content.add(button, BorderLayout.SOUTH);
    }
    panel.add(content);

    return new DisabledContentView(ScrollablePanel.adapt(panel));
  }

  private synchronized Listener getListener() {
    return myListener;
  }

  public synchronized Detach setListener(Listener listener) {
    myListener = listener;
    return new Detach() {
      protected void doDetach() {
        myListener = null;
      }
    };
  }

  private static JComponent createErrorComponent(String message) {
    JTextArea text = new JTextArea() {
      protected void paintComponent(Graphics g) {
        AwtUtil.applyRenderingHints(g);
        UIUtil.fillPaint(g, this, getErrorPaint(), false);
        super.paintComponent(g);
      }
    };
    text.setText(message);
    text.setMargin(new Insets(9, 9, 9, 9));
    //text.setPreferredSize(UIUtil.getRelativeDimension(text, 40, 5));
    text.setEditable(false);
    text.setOpaque(false);
    text.setWrapStyleWord(true);
    text.setLineWrap(true);
    return text;
  }

  private static synchronized Paint getErrorPaint() {
    if (ourErrorPaint != null)
      return ourErrorPaint;
    int step = 20;
    int width = 9;
    Color c1 = UIUtil.getEditorBackground();
    Color c2 = ColorUtil.between(c1, GlobalColors.ERROR_COLOR, 0.05F);
    BufferedImage image = new BufferedImage(step, step, BufferedImage.TYPE_INT_RGB);
    Graphics g = image.getGraphics();
    try {
      g.setColor(c1);
      g.fillRect(0, 0, step, step);
      g.setColor(c2);
      g.fillPolygon(new int[]{0, width, 0}, new int[]{0, 0, width}, 3);
      g.fillPolygon(new int[]{0, step, step, width}, new int[]{step, 0, width, step}, 4);
    } finally {
      g.dispose();
    }
    ourErrorPaint = new TexturePaint(image, new Rectangle2D.Float(0, 0, step, step));
    return ourErrorPaint;
  }


  private static class DisabledContentView implements ContentView {
    private final JComponent myComponent;

    public DisabledContentView(JComponent component) {
      myComponent = component;
    }

    public JComponent getComponent() {
      return myComponent;
    }

    public void init(Lifespan life, Configuration config, MiscConfig miscConfig) {
    }

    public void copyToClipboard() {
    }
  }

  public static interface Listener {
    void onLoaded(FileData data, ContentView contentView);
  }
}
