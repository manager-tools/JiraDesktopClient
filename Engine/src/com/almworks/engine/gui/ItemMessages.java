package com.almworks.engine.gui;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.UIController;
import com.almworks.util.Terms;
import com.almworks.util.collections.Containers;
import com.almworks.util.components.Link;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.AlphaIcon;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Map;

public class ItemMessages implements UIController<JPanel>, ActionListener {
  private static final Border MESSAGE_BORDER = Aqua.isAqua()
    ? new EmptyBorder(2, 6, 0, 6) : new EmptyBorder(2, 0, 0, 0);

  private static final Border AREA_BORDER = Aqua.isAqua()
    ? UIUtil.getCompoundBorder(Aqua.MAC_LIGHT_BORDER_NORTH, new EmptyBorder(0, 0, 2, 0))
    : UIUtil.getCompoundBorder(
        new BrokenLineBorder(
          ColorUtil.between(DocumentFormAugmentor.backgroundColor(), Color.BLACK, 0.3F),
          1, BrokenLineBorder.NORTH),
        Aero.isAero() ? new EmptyBorder(3, 5, 5, 5) : new EmptyBorder(3, 0, 5, 0));

  private final Object CLEAR_KEY = new Object();

  private final JPanel myPanel = new JPanel(new InlineLayout(InlineLayout.VERTICAL, 0, true));
  private final JPanel myWholePanel = new JPanel(new BorderLayout());
  private final Map<Object, MessageComponent> myMessages = Collections15.treeMap(Containers.stringComparator());

  private final Timer myTimer;
  private final Map<Object, ItemMessage> myBurst = Collections15.hashMap();

  private final boolean myHideActions;
  private final boolean myShowDBState;

  private static JComponent create(boolean hideActions, boolean showDBState) {
    ItemMessages messages = new ItemMessages(hideActions, showDBState);
    JPanel component = messages.myPanel;
    UIController.CONTROLLER.putClientValue(component, messages);
    return messages.myWholePanel;
  }

  public static JComponent createMessagesForViewer() {
    return create(false, true);
  }

  private ItemMessages(boolean hideActions, boolean showDBState) {
    myHideActions = hideActions;
    myShowDBState = showDBState;
    myPanel.setBackground(DocumentFormAugmentor.backgroundColor());
    myWholePanel.setBorder(AREA_BORDER);
    myWholePanel.setBackground(DocumentFormAugmentor.backgroundColor());
    myWholePanel.add(myPanel, BorderLayout.CENTER);
    myWholePanel.add(createHelpComponent(), BorderLayout.EAST);
    myTimer = new Timer(125, this);
    myTimer.setRepeats(false);
  }

  private JComponent createHelpComponent() {
    final String actionCaluse = myHideActions ? "" : ", or select a related action";
    final String help = Local.parse(
      String.format(
        "<html>This area shows messages related to the current %s." +
        "<br>You can click on a message to get more information%s.",
        Terms.ref_artifact, actionCaluse));

    final Link link = createHelpLink(help);
    return createHelpBox(link);
  }

  private Link createHelpLink(final String help) {
    final Link link = new Link();
    link.setIcon(new AlphaIcon(Icons.QUESTION, 0.67f));
    link.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        UIUtil.showNoticePopup(help, link, null,
          new UIUtil.IndependentPositioner(UIUtil.BEFORE, UIUtil.ALIGN_END));
      }
    });
    adjustLinkSizeToLookAligned(link);
    return link;
  }

  private void adjustLinkSizeToLookAligned(Link link) {
    final Dimension size = new Dimension(link.getPreferredSize().width, MessageComponent.BUTTON_HEIGHT);
    link.setMinimumSize(size);
    link.setPreferredSize(size);
    link.setMaximumSize(size);
  }

  private Box createHelpBox(Link link) {
    final Box box = Box.createVerticalBox();
    box.add(Box.createVerticalGlue());
    box.add(link);
    box.setBorder(MESSAGE_BORDER);
    return box;
  }

  public void setMessage(Object messageKey, @Nullable ItemMessage message) {
    Threads.assertAWTThread();
    myBurst.put(messageKey, message);
    myTimer.restart();
  }

  public void clearMessages() {
    Threads.assertAWTThread();
    myBurst.put(CLEAR_KEY, null);
    myTimer.restart();
  }

  public boolean hasMessage(Object key) {
    return myMessages.get(key) != null;
  }

  public void connectUI(@NotNull Lifespan life, @NotNull ModelMap model, @NotNull JPanel component) {
    assert component == myPanel;
    clearMessages();
    attachStaticMessages(life, model);
    attachMessageProviders(life, model);
  }

  private void attachStaticMessages(Lifespan life, ModelMap model) {
    new SyncProblems(model, this).attach(life);
    new DownloadStageMessage(model, this).attach(life);
    new LocalChangeMessage(model, this, myShowDBState).attach(life);
  }

  private void attachMessageProviders(Lifespan life, ModelMap model) {
    final LoadedItemServices itemServices = LoadedItemServices.VALUE_KEY.getValue(model);
    if(itemServices != null) {
      final ItemMessagesRegistry registry = itemServices.getActor(ItemMessagesRegistry.ROLE);
      final ItemMessageProvider[] providers = registry.getProviders();
      for(final ItemMessageProvider provider : providers) {
        provider.attachMessages(life, model, this);
      }
    }
  }

  public void actionPerformed(ActionEvent e) {
    Threads.assertAWTThread();
    assert e.getSource() == myTimer;
    processBurst();
  }

  private void processBurst() {
    if(shouldClear()) {
      doClearMessages();
    } else {
      doUpdateMessages();
    }
    myBurst.clear();
  }

  private boolean shouldClear() {
    if(!myBurst.containsKey(CLEAR_KEY)) {
      return false;
    }

    for(final ItemMessage message : myBurst.values()) {
      if(message != null) {
        return false;
      }
    }

    return true;
  }

  private void doClearMessages() {
    Threads.assertAWTThread();
    myPanel.removeAll();
    myMessages.clear();
    myWholePanel.setVisible(false);
  }

  private void doUpdateMessages() {
    final boolean[] requests = new boolean[2];
    boolean revalidate = false;
    boolean relayout = false;

    for(final Map.Entry<Object, ItemMessage> messageEntry : myBurst.entrySet()) {
      final Object messageKey = messageEntry.getKey();
      if(messageKey == CLEAR_KEY) {
        continue;
      }

      requests[0] = requests[1] = false;
      doSetMessage(messageKey, messageEntry.getValue(), requests);
      revalidate |= requests[0];
      relayout |= requests[1];
    }

    final boolean visible = !myMessages.isEmpty();
    adjustWholePanel(visible, revalidate, relayout);
  }

  /**
   * Find or add or remove or adjust a component for the message.
   * @param messageKey Message key.
   * @param message The message, {@code null} to remove.
   * @param requests A boolean array to store requests:
   * set {@code requests[0]} to {@code true} for an invalidate/revalidate request;
   * set {@code requests[1]} to {@code true} for a relayout/repaint request;
   * {@code requests[0]} implies {@code requests[1]}.
   */
  private void doSetMessage(Object messageKey, @Nullable ItemMessage message, boolean[] requests) {
    Threads.assertAWTThread();

    MessageComponent component = myMessages.get(messageKey);
    if(message != null && component == null) {
      component = new MessageComponent(myHideActions);
      myMessages.put(messageKey, component);
      myPanel.add(component.getComponent(), getIndex(messageKey));
      requests[0] = true;
    }

    if(component == null) {
      return;
    }

    if(message != null) {
      component.setMessage(message);
      requests[1] = true;
    } else {
      myMessages.remove(messageKey);
      myPanel.remove(component.getComponent());
      if(!myMessages.isEmpty()) {
        requests[0] = true;
      }
    }
  }

  private int getIndex(Object messageKey) {
    int index = 0;
    for(final Object key : myMessages.keySet()) {
      if(key == messageKey) {
        return index;
      }
      index++;
    }
    assert false;
    return index;
  }

  private void adjustWholePanel(boolean visible, boolean revalidate, boolean relayout) {
    myWholePanel.setVisible(visible);
    if(visible) {
      if(revalidate) {
        myPanel.invalidate();
        myPanel.revalidate();
      }
      if(revalidate || relayout) {
        relayout();
        myPanel.repaint();
      }
    }
  }

  private void relayout() {
    int maxWidth = calculateMaxMessageWidth();
    adjustMessageComponentsToWidth(maxWidth);
  }

  private int calculateMaxMessageWidth() {
    int maxWidth = 0;
    for(final Map.Entry<Object, MessageComponent> e : myMessages.entrySet()) {
      final MessageComponent mc = e.getValue();
      if(mc == null) {
        continue;
      }
      maxWidth = Math.max(mc.getTextPrefWidth(), maxWidth);
    }
    return maxWidth + 10;
  }

  private void adjustMessageComponentsToWidth(int maxWidth) {
    for(final Map.Entry<Object, MessageComponent> e : myMessages.entrySet()) {
      final MessageComponent mc = e.getValue();
      if(mc == null) {
        continue;
      }
      mc.setTextFilledWidth(maxWidth);
    }
  }

  private static class MessageComponent {
    private static final UIUtil.Positioner POSITIONER = new UIUtil.IndependentPositioner(null, UIUtil.ALIGN_END) {
      @Override
      public int getX(int screenX, int screenW, int ownerX, int ownerW, int childW) {
        return ownerX + ownerW + 5;
      }
    };

    private static final int BUTTON_HEIGHT = UIUtil.getIconButtonPrefSize().height;

    private final JPanel myComponent = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, 5, false));
    private final JLabel myIcon = new JLabel();
    private final Link myLink = new Link();
    private final JLabel myLabel = new JLabel();
    private final JComponent myFill = new JLabel();

    private ItemMessage myMessage;
    private JComponent myText;
    private JComponent myActions;

    private final boolean myHideActions;

    private MessageComponent(boolean hideActions) {
      myIcon.setPreferredSize(new Dimension(20, 20));
      myIcon.setHorizontalAlignment(SwingConstants.CENTER);
      myLink.setHorizontalAlignment(SwingConstants.LEFT);
      myLink.setFocusable(false);

      final LinkHandler handler = new LinkHandler();
      myLink.addActionListener(handler);
      myLink.addMouseMotionListener(handler);
      myLink.addMouseListener(handler);

      myComponent.add(myIcon);
      myComponent.setOpaque(false);

      myHideActions = hideActions;
    }

    public void setMessage(ItemMessage message) {
      Threads.assertAWTThread();

      myMessage = message;
      myComponent.setBorder(MESSAGE_BORDER);

      myIcon.setIcon(message.getIcon());

      removePreviousText();
      removePreviousToolbar();

      createAndAddText();
      if(message.hasActions() && !myHideActions) {
        createAndAddToolbar(message.getActions());
      }
    }

    private void removePreviousText() {
      if(myText != null) {
        myComponent.remove(myText);
        myComponent.remove(myFill);
        myText = null;
      }
    }

    private void removePreviousToolbar() {
      if(myActions != null) {
        myComponent.remove(myActions);
        myActions = null;
      }
    }

    private void createAndAddText() {
      myText = prepareTextComponent();
      myComponent.add(myText);
      myComponent.add(myFill);
    }

    private JComponent prepareTextComponent() {
      if(hasLongDescription()) {
        myLink.setText(myMessage.getShortDescription());
        myLink.setForeground(myMessage.getColor());
        return myLink;
      } else {
        myLabel.setText(myMessage.getShortDescription());
        myLabel.setForeground(myMessage.getColor());
        return myLabel;
      }
    }

    private boolean hasLongDescription() {
      final String longDescription = myMessage.getLongDescription();
      if(longDescription == null || longDescription.length() == 0) {
        return false;
      }
      return !Util.equals(myMessage.getShortDescription(), longDescription);
    }

    private void createAndAddToolbar(AnAction[] actions) {
      final ToolbarBuilder toolbar = ToolbarBuilder.buttonsWithText();
      toolbar.addComponent(new JLabel(Icons.DOUBLE_SIGN_ARROW_LEFT.getGrayed()));
      for(final AnAction action : actions) {
        toolbar.addAction(action);
      }

      myActions = toolbar.createHorizontalPanel();
      myActions.setOpaque(false);
      myComponent.add(myActions);
    }

    public JComponent getComponent() {
      return myComponent;
    }

    JComponent getTextComponent() {
      return myText;
    }

    int getTextPrefWidth() {
      return getTextComponent().getPreferredSize().width;
    }

    void setTextFilledWidth(int width) {
      final Dimension size = new Dimension(width - getTextPrefWidth(), BUTTON_HEIGHT);
      myFill.setPreferredSize(size);
      myFill.setMaximumSize(size);
      myFill.setSize(size);
    }

    private class LinkHandler extends MegaMouseAdapter implements ActionListener {
      private boolean myMouseOver = false;
      private boolean myDontPopUp = false;
      private long myTimeClosed = 0L;
      private Detach myPopupCloser = null;
      
      @Override
      public void mouseEntered(MouseEvent e) {
        myMouseOver = true;
        myDontPopUp = false;
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        myDontPopUp = false;
      }

      @Override
      public void mouseExited(MouseEvent e) {
        myMouseOver = false;
        myDontPopUp = false;
      }

      public void actionPerformed(ActionEvent e) {
        if(myPopupCloser != null) {
          if(!myPopupCloser.isDetached()) {
            myPopupCloser.detach();
          }
          myPopupCloser = null;
        }

        if(myDontPopUp) {
          myDontPopUp = false;
          if(System.currentTimeMillis() - myTimeClosed <= 250L) {
            return;
          }
        }

        final String longDescription = myMessage.getLongDescription();
        if(longDescription != null && longDescription.length() > 0) {
          final Detach detach = new Detach() {
            protected void doDetach() throws Exception {
              if(myMouseOver) {
                myDontPopUp = true;
                myTimeClosed = System.currentTimeMillis();
              }
            }
          };
          myPopupCloser = UIUtil.showNoticePopup(longDescription, myLink, detach, POSITIONER);
        }
      }
    };
  }
}