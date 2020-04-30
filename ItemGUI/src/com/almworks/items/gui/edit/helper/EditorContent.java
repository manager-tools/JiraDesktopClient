package com.almworks.items.gui.edit.helper;

import com.almworks.api.gui.BasicWindowBuilder;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.gui.WindowController;
import com.almworks.gui.InitialWindowFocusFinder;
import com.almworks.items.gui.edit.DefaultEditModel;
import com.almworks.items.gui.edit.editors.DataVerificationFailure;
import com.almworks.items.sync.EditorLock;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.config.Configuration;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.SimpleProvider;
import com.almworks.util.ui.actions.globals.GlobalData;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicReference;

class EditorContent implements UIComponentWrapper {
  public static final TypedKey<EditorLock> EDITOR_LOCK = TypedKey.create("editorLock");
  public static final int WAIT_MESSAGE_DELAY = 500;
  private static final EmptyBorder ERROR_BORDER = new EmptyBorder(5, 5, 5, 5);
  private final JPanel myWholePanel = new JPanel(new BorderLayout(0, 0));
  private final SimpleProvider myProvider = new SimpleProvider(DefaultEditModel.ROLE, ItemEditController.ROLE);
  private final DetachComposite myLife = new DetachComposite();
  private final EditItemHelper myHelper;
  private final EditDescriptor myDescriptor;
  private final AtomicReference<WindowController> myWindowController = new AtomicReference<WindowController>(null);
  private JWindow myWaitMessage = null;

  EditorContent(EditItemHelper helper, EditDescriptor descriptor) {
    myHelper = helper;
    myDescriptor = descriptor;
    DataProvider.DATA_PROVIDER.putClientValue(myWholePanel, myProvider);
    GlobalData.KEY.addClientValue(myWholePanel, DefaultEditModel.ROLE, ItemEditController.ROLE);
    ConstProvider.addRoleValue(myWholePanel, EditDescriptor.DESCRIPTION_STRINGS, descriptor.getDescriptionStrings());
  }

  @Override
  public JComponent getComponent() {
    return myWholePanel;
  }

  @Override
  public void dispose() {
    if (!myLife.isDetachStarted()) myLife.detach();
    WindowController controller = myWindowController.get();
    if (controller != null) {
      controller.disableCloseConfirmation(true);
      controller.close();
    }
    JWindow waitMessage = myWaitMessage;
    if (waitMessage != null) {
      myWaitMessage = null;
      waitMessage.dispose();
    }
  }

  public Lifespan getLifespan() {
    return myLife;
  }

  public void showWindow(EditFeature feature, DefaultEditModel.Root model, ItemEditController controller) {
    Threads.assertAWTThread();
    boolean success = false;
    try {
      JWindow dialog = myWaitMessage;
      if (dialog != null) {
        myWaitMessage = null;
        dialog.dispose();
      }
      BasicWindowBuilder<UIComponentWrapper> builder = myDescriptor.buildWindow(myHelper.getContainer());
      if (builder == null) return;
      builder.setContent(this);
      builder.addGlobalDataRoot();
      builder.setCloseConfirmation(new DiscardConfirmListener());
      Configuration config = builder.getConfiguration().getOrCreateSubset("editor");
      JComponent editor = feature.editModel(myLife, model, config);
      if (editor == null) return;
      model.saveInitialValues();
      myProvider.setSingleData(DefaultEditModel.ROLE, model);
      myProvider.setSingleData(ItemEditController.ROLE, controller);
      prepareContentComponent(model, editor);
      myWindowController.set(builder.showWindow());
      success = true;
    } finally {
      if (!success) dispose();
    }
  }

  private void prepareContentComponent(DefaultEditModel.Root model, JComponent editor) {
    JComponent panel = myDescriptor.createToolbarPanel(model);
    if (panel != null) {
      JToolBar toolBar = new JToolBar();
      toolBar.setFloatable(false);
      toolBar.setRollover(true);
      toolBar.add(panel);
      Aqua.addSouthBorder(toolBar);
      Aero.addSouthBorder(toolBar);
      myWholePanel.add(toolBar, BorderLayout.NORTH);
    }
    myWholePanel.add(editor, BorderLayout.CENTER);
    myWholePanel.add(createErrorComponent(model, editor), BorderLayout.SOUTH);
    InitialWindowFocusFinder.focusInitialComponent(myWholePanel);
  }

  private static final Border DEFAULT_ERROR_BORDER = new BrokenLineBorder(Color.GRAY, 1, BrokenLineBorder.NORTH);
  private JComponent createErrorComponent(DefaultEditModel.Root model, JComponent editor) {
    Border actualErrorBorder = ERROR_BORDER;
    Border outerErrorBorder;
    if (Boolean.TRUE.equals(EditFeature.ERROR_BORDER.getClientValue(editor))) {
      if (Aero.isAero()) outerErrorBorder = Aero.getAeroBorderNorth();
      else if (Aqua.isAqua()) outerErrorBorder = Aqua.MAC_BORDER_NORTH;
      else outerErrorBorder = DEFAULT_ERROR_BORDER;
    } else outerErrorBorder = null;
    if (outerErrorBorder != null) actualErrorBorder = new CompoundBorder(outerErrorBorder, actualErrorBorder);
    JComponent errorComponent = DataVerificationFailure.install(myLife, model);
    errorComponent.setBorder(actualErrorBorder);
    return errorComponent;
  }

  public boolean isAlive() {
    if (isDisposed()) return false;
    WindowController controller = myWindowController.get();
    return controller != null && controller.isVisible();
  }

  public boolean isDisposed() {
    return myLife.isEnded();
  }

  public void activate() {
    WindowController controller = myWindowController.get();
    if (controller != null) controller.activate();
  }

  public void showWaitMessage() {
    final Timer timer = new Timer(WAIT_MESSAGE_DELAY, null);
    ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        timer.stop();
        maybeShowWaitMessage();
      }
    };
    timer.addActionListener(listener);
    timer.start();
  }

  private static final CompoundBorder MESSAGE_BORDER =
    new CompoundBorder(new EmptyBorder(1, 1, 1, 1), new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(15, 50, 15, 50)));
  private void maybeShowWaitMessage() {
    Threads.assertAWTThread();
    if (myLife.isEnded() || myWindowController.get() != null || myWaitMessage != null) return;
    MainWindowManager windowManager = myHelper.getContainer().getActor(MainWindowManager.ROLE);
    if (windowManager == null) return;
    JFrame frame = windowManager.getMainFrame();
    JWindow window = new JWindow(frame);
    JLabel message = new JLabel("Preparing editor. Please wait\u2026");
    message.setBorder(MESSAGE_BORDER);
    window.getContentPane().add(message);
    window.pack();
    UIUtil.centerWindow(window);
    window.setVisible(true);
    myWaitMessage = window;
  }
}
