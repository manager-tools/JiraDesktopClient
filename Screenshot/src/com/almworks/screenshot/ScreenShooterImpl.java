package com.almworks.screenshot;

import com.almworks.api.gui.FrameBuilder;
import com.almworks.api.gui.WindowController;
import com.almworks.api.gui.WindowManager;
import com.almworks.api.screenshot.ScreenShooter;
import com.almworks.api.screenshot.Screenshot;
import com.almworks.screenshot.editor.image.ImageEditor;
import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.shooter.ShootDialog;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.AToolbar;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.files.ExtensionFileFilter;
import com.almworks.util.images.Icons;
import com.almworks.util.images.ImageUtil;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.StringUtil;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Set;

class ScreenShooterImpl implements ScreenShooter {
  public static final String ATTACHMENT_NAME_SETTING = "attachmentName";
  private final Configuration myConfig;
  private final WindowManager myWindowManager;

  private boolean myShooting = false;

  public ScreenShooterImpl(WindowManager windowManager, Configuration conf) {
    myWindowManager = windowManager;
    myConfig = conf;
  }

  public void shoot(Component contextComponent, final Configuration conf, final Procedure<Screenshot> result) {
    Threads.assertAWTThread();
    if(myShooting) {
      return;
    }

    final ShootDialog dialog = ShootDialog.create(contextComponent, myConfig.getOrCreateSubset("ShootDialog"),
      new Procedure<BufferedImage>() {
        public void invoke(final BufferedImage image) {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              editSnapshot(image, conf, result);
            }
          });
        }
      });

    dialog.addDetach(new Detach() {
      protected void doDetach() throws Exception {
        myShooting = false;
      }
    });

    myShooting = true;
    dialog.setVisible(true);
    dialog.requestFocus();
  }

  public void edit(Image image, Configuration conf, Procedure<Screenshot> result) {
    BufferedImage edited = ImageUtil.createBufferedImage(image);
    editSnapshot(edited, conf, result);
  }

  private void editSnapshot(BufferedImage capture, final Configuration conf, final Procedure<Screenshot> result) {
    final ImageEditor editor = new ImageEditor(capture);
    FrameBuilder frameBuilder = myWindowManager.createFrame("screenshot");
    frameBuilder.setTitle("Screenshot");

    final JTextField attachmentNameField = new JTextField(conf.getSetting(ATTACHMENT_NAME_SETTING, "Screenshot"), 20);
    attachmentNameField.selectAll();
    UIUtil.addTextListener(Lifespan.FOREVER, attachmentNameField, new ChangeListener() {
      @Override
      public void onChange() {
        conf.setSetting(ATTACHMENT_NAME_SETTING, attachmentNameField.getText());
      }
    });

    ToolbarBuilder builder = new ToolbarBuilder();
    builder.setContextComponent(editor.getComponent());
    builder.addAction(new AttachAndCloseAction(frameBuilder, editor, result, attachmentNameField));
    builder.addAction(new SaveAsFileAction(editor));
    builder.addSeparator();
    builder.addAction(UndoRedoAction.UNDO);
    builder.addAction(UndoRedoAction.REDO);
    builder.addSeparator();
    builder.addAction(new CloseAction(frameBuilder, editor, result));
    AToolbar toolbar = builder.createHorizontalToolbar();

    JPanel topbar = new JPanel(new BorderLayout());

    JPanel descriptionPanel = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, 5, false));
    JLabel sfnLabel = new JLabel();
    NameMnemonic.parseString("Attachment &Name:").setToLabel(sfnLabel);
    sfnLabel.setLabelFor(attachmentNameField);
    descriptionPanel.add(sfnLabel);
    descriptionPanel.add(attachmentNameField);

    topbar.add(toolbar, BorderLayout.WEST);
    topbar.add(descriptionPanel, BorderLayout.EAST);

    JPanel framePanel = new JPanel(new BorderLayout());

    if (Aqua.isAqua()) {
      Aqua.addSouthBorder(topbar);
    } else {
      framePanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 3));
      topbar.setBorder(new EmptyBorder(0, 0, 5, 0));
    }

    framePanel.add(editor.getComponent(), BorderLayout.CENTER);
    framePanel.add(topbar, BorderLayout.NORTH);
    //framePanel.add(toolbar, BorderLayout.NORTH);

    frameBuilder.setContent(framePanel);
    frameBuilder.showWindow(editor.getDetach());
  }

  private class AttachAndCloseAction extends SimpleAction {
    private final ImageEditor myEditor;
    private final Procedure<Screenshot> myResult;
    private final FrameBuilder myFrameBuilder;
    private final JTextField myAttachmentNameField;

    public AttachAndCloseAction(FrameBuilder frameBuilder, ImageEditor editor, Procedure<Screenshot> result,
      JTextField attachmentNameField)
    {
      super("Attach", Icons.ACTION_ATTACH_FILE);
      myFrameBuilder = frameBuilder;
      myAttachmentNameField = attachmentNameField;
      myEditor = editor;
      myResult = result;
      setDefaultPresentation(PresentationKey.SHORTCUT,
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK));
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
        "Attach the image to the selected " + Terms.ref_artifact + " and close the editor");
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(myResult != null);
    }

    public void doPerform(ActionContext context) throws CantPerformException {
      BufferedImage image = myEditor.getImage();
      String attachmentName = myAttachmentNameField.getText();
      myResult.invoke(new ValidScreenshot(image, attachmentName));
      myFrameBuilder.getWindowContainer().requireActor(WindowController.ROLE).close();
    }
  }


  private class CloseAction extends SimpleAction {
    private final ImageEditor myEditor;
    private final Procedure<Screenshot> myResult;
    private final FrameBuilder myFrameBuilder;

    public CloseAction(FrameBuilder frameBuilder, ImageEditor editor, Procedure<Screenshot> result) {
      super("Cancel", Icons.ACTION_GENERIC_CANCEL_OR_REMOVE);
      myFrameBuilder = frameBuilder;
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Close image editor, discarding all changes");
      myEditor = editor;
      myResult = result;
    }

    public void doPerform(ActionContext context) throws CantPerformException {
      myFrameBuilder.getWindowContainer().requireActor(WindowController.ROLE).close();
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(myResult != null);
    }
  }


  public static class SaveAsFileAction extends SimpleAction {
    private final ImageEditor myEditor;

    public SaveAsFileAction(ImageEditor editor) {
      super("Save As\u2026", Icons.ACTION_SAVE_ALL);
      setDefaultPresentation(PresentationKey.SHORTCUT, Shortcuts.ksMenu(KeyEvent.VK_S));
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Save image into a file and continue editing");
      myEditor = editor;
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(true);
    }

    public void doPerform(ActionContext context) throws CantPerformException {
      JFileChooser chooser = new JFileChooser();
      Set<String> formats = Collections15.hashSet(ImageIO.getWriterFormatNames());
      assert formats.contains("png") : formats;
      ExtensionFileFilter defaultFilter = new ExtensionFileFilter("png", "PNG files", true);
      chooser.addChoosableFileFilter(defaultFilter);
      if (formats.contains("jpg")) {
        chooser.addChoosableFileFilter(new ExtensionFileFilter("jpg", "JPEG files", true));
      }
      chooser.setFileFilter(defaultFilter);
      int retVal = chooser.showSaveDialog(myEditor.getComponent());
      if (retVal == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        if (file != null) {
          String format = null;
          String filename = file.getName();
          int k = filename.lastIndexOf('.');
          if (k > 0 && k < filename.length() - 1) {
            format = filename.substring(k + 1).toLowerCase();
          }
          if (format == null || !formats.contains(format)) {
            FileFilter ff = chooser.getFileFilter();
            if (ff instanceof ExtensionFileFilter) {
              format = ((ExtensionFileFilter) ff).getExtension();
            }
            if (format == null || !formats.contains(format)) {
              format = "png";
            }
            file = new File(file.getPath() + "." + format);
          }
          try {
            ImageIO.write(myEditor.getImage(), format, file);
          } catch (Exception e) {
            JOptionPane.showMessageDialog(context.getComponent(),
              "Cannot save image into file: " + StringUtil.limitString(e.getMessage(), 30));
            Log.warn("cannot write file " + file, e);
          }
        }
      }
    }
  }


  private static class UndoRedoAction extends SimpleAction {
    public static final AnAction UNDO = new UndoRedoAction("Undo", Icons.ACTION_UNDO, true);
    public static final AnAction REDO = new UndoRedoAction("Redo", Icons.ACTION_REDO, false);
    private final boolean myUndo;

    private UndoRedoAction(@Nullable String name, @Nullable Icon icon, boolean undo) {
      super(name, icon);
      myUndo = undo;
      watchModifiableRole(WorkingImage.HISTORY_ROLE);
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, name);
      setDefaultPresentation(PresentationKey.SHORTCUT,
        myUndo ? Shortcuts.UNDO : Shortcuts.REDO);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      WorkingImage.History history = context.getSourceObject(WorkingImage.HISTORY_ROLE);
      context.setEnabled(history.canPerform(myUndo));
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      WorkingImage.History history = context.getSourceObject(WorkingImage.HISTORY_ROLE);
      history.perform(myUndo);
    }
  }
}
