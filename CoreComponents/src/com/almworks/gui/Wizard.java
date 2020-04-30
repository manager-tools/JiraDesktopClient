package com.almworks.gui;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.WindowController;
import com.almworks.util.Env;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.AActionButton;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * This class is for creating wizards. How to use:
 * <ul>
 * <li>subclass it;
 * <li>have a few inner classes that implement {@link WizardPage};
 * <li>it's advised to call {@link #initialize(WizardPage...)} in your wizard subclass' constructor;
 * <li>override {@link #wizardCancelled()} and {@link #wizardFinished()} as you see fit;
 * <li>to fire up the wizard, instantiate it and invoke {@link #showPage(String)}
 * giving it the ID of the first page.
 * </ul>
 */
public class Wizard {
  public static final String NEXT_TEXT = Env.isMac() ? "Continue" : "Next";
  /** "Next" button text. */
  public static final NameMnemonic NEXT = NameMnemonic.parseString(Env.isMac() ? NEXT_TEXT : "&" + NEXT_TEXT + " >");

  /** "Back" button text. */
  public static final NameMnemonic BACK = NameMnemonic.parseString(Env.isMac() ? "Go Back" : "< &Back");

  /** "Finish" button text. */
  public static final NameMnemonic FINISH = NameMnemonic.parseString("&Finish");

  /** "Cancel" button text. */
  public static final NameMnemonic CANCEL = NameMnemonic.parseString("&Cancel");

  /** "Close" button text. */
  public static final NameMnemonic CLOSE = NameMnemonic.parseString("&Close");

  /** Special page ID used to cancel the wizard. */
  public static final String CANCEL_ID = "wizard.cancel";

  /** Special page ID used to finish the wizard. */
  public static final String FINISH_ID = "wizard.finish";

  /** Special page ID used to close the wizard. */
  public static final String CLOSE_ID = "wizard.close";

  private final DialogBuilder myBuilder;
  private final Map<String, WizardPage> myPageMap;

  private final JPanel myContent;
  private final CardLayout myLayout;
  private final WizardAction myBack;
  private final WizardAction myNext;
  private final JComponent myBottomPlace;

  private boolean myInitDone = false;
  private volatile boolean myShowing = false;
  private boolean myFinishing = false;
  private boolean myClosing = false;
  private String myPageID = null;

  /**
   * A no-title constructor.
   * @param builder The {@code DialogBuilder} to use for creting the window.
   */
  public Wizard(DialogBuilder builder) {
    myBuilder = builder;
    myPageMap = Collections15.hashMap();

    myLayout = new CardLayout();
    myContent = new JPanel(myLayout);

    myBack = new BackAction();
    myNext = new NextAction();
    myBottomPlace = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, UIUtil.GAP, true));
  }

  /**
   * A constructor.
   * @param builder The {@code DialogBuilder} to use for creting the window.
   * @param title Wizard window title.
   */
  public Wizard(DialogBuilder builder, String title) {
    this(builder);
    setTitle(title);
  }

  /**
   * Initializes the wizard. This method must be called exactly once
   * and given all possible pages in this wizard. It initalizes the
   * window and its controls, fills internal data structures, and
   * calculates the preferred size of the content.
   * @param pages The pages.
   */
  public void initialize(WizardPage... pages) {
    if(myInitDone) {
      assert false;
      return;
    }

    registerPages(pages);
    prepareDialogContent(pages);
    adjustContentSize(pages);
    prepareDialogBuilder();

    myInitDone = true;
  }

  private void registerPages(WizardPage[] pages) {
    for(final WizardPage page : pages) {
      myPageMap.put(page.getPageID(), page);
    }
  }

  private void prepareDialogContent(WizardPage[] pages) {
    for(final WizardPage page : pages) {
      myContent.add(page.getContent(), page.getPageID());
    }
  }

  private void adjustContentSize(WizardPage[] pages) {
    int maxWidth = 0, maxHeight = 0;
    for(final WizardPage page : pages) {
      final Dimension pref = page.getContent().getPreferredSize();
      maxWidth = Math.max(maxWidth, pref.width);
      maxHeight = Math.max(maxHeight, pref.height);
    }
    
    maxHeight = Math.max(maxHeight, maxWidth / 2);

    myContent.setPreferredSize(new Dimension(maxWidth, maxHeight));
  }

  private void prepareDialogBuilder() {
    myBuilder.setContent(myContent);
    myBuilder.addAction(myBack);
    myBuilder.addAction(myNext);
    myBuilder.setBottomLineComponent(myBottomPlace);
    myBuilder.setIgnoreStoredSize(true);
  }

  /**
   * Set the title of the wizard window.
   * Has no effect if the window is created already.
   * @param title The new title.
   */
  public void setTitle(String title) {
    myBuilder.setTitle(title);
  }

  /**
   * The method is used to show a page. If the wizard window
   * is not open yet, then this method would open it.
   * Notifies the pages as necessary.
   * @param pageID A page ID of a page that was given earlier
   * to {@link #initialize(WizardPage...)}, or {@link #CANCEL_ID},
   * or {@link #FINISH_ID}, or {@link #CLOSE_ID}.
   */
  public void showPage(@NotNull final String pageID) {
    assert myInitDone : "wizard not initialized";
    assert pageID != null : "pageID is null";
    assert myPageMap.containsKey(pageID) || isPoison(pageID) : pageID;

    if(pageID.equals(myPageID)) {
      return;
    }

    if(myPageID != null) {
      final WizardPage oldPage = myPageMap.get(myPageID);
      oldPage.aboutToHidePage(pageID);
    }

    if(isPoison(pageID)) {
      closeWindow(pageID);
      return;
    }

    final WizardPage newPage = myPageMap.get(pageID);
    newPage.aboutToDisplayPage(myPageID);

    final String replacedPageID = myPageID;
    myLayout.show(myContent, pageID);
    myPageID = pageID;

    adjustBackAction(newPage);
    adjustNextAction(newPage);
    addMoreActions(newPage);

    if(!myShowing) {
      showWizardWindow();
    }

    newPage.displayingPage(replacedPageID);
  }

  private boolean isPoison(String pageID) {
    return CANCEL_ID.equals(pageID) || FINISH_ID.equals(pageID) || CLOSE_ID.equals(pageID);
  }

  private void closeWindow(String pageID) {
    if(FINISH_ID.equals(pageID)) {
      myFinishing = true;
    }
    final WindowController ec = getWindowController();
    if(ec != null) {
      ec.close();
    }
  }

  private void adjustBackAction(WizardPage newPage) {
    final String prevID = newPage.getPrevPageID();
    assert prevID != null;

    if(CANCEL_ID.equals(prevID)) {
      myBack.setName(CANCEL);
    } else {
      myBack.setName(BACK);
    }
  }

  private void adjustNextAction(WizardPage newPage) {
    final String nextID = newPage.getNextPageID();
    assert nextID != null;

    if(FINISH_ID.equals(nextID)) {
      myNext.setName(FINISH);
    } else if(CLOSE_ID.equals(nextID)) {
      myNext.setName(CLOSE);
      myClosing = true;
    } else {
      myNext.setName(NEXT);
    }
  }

  private void addMoreActions(WizardPage newPage) {
    myBottomPlace.removeAll();
    final AnAction[] more = newPage.getMoreActions();
    if(more != null) {
      for(final AnAction a : more) {
        myBottomPlace.add(new AActionButton(a));
      }
    }
    myBottomPlace.invalidate();
    myBottomPlace.revalidate();
    myBottomPlace.repaint();
  }

  private void showWizardWindow() {
    myShowing = true;
    myFinishing = false;
    myClosing = false;
    myBuilder.showWindow(new Detach() {
      protected void doDetach() throws Exception {
        windowClosed();
        myShowing = false;
        myPageID = null;
      }
    });
  }

  private void windowClosed() {
    if(myFinishing) {
      wizardFinished();
    } else if(myClosing) {
      wizardClosed();
    } else {
      wizardCancelled();
    }
    wizardReleased();
  }

  /**
   * This method gets called when the wizard is cancelled.
   */
  protected void wizardCancelled() {}

  /**
   * This method gets called when the wizard is finished.
   */
  protected void wizardFinished() {}

  /**
   * This method gets called when the wizard is closed with
   * the "Close" button, not cancelled.
   */
  protected void wizardClosed() {}

  /**
   * This method gets called when the wizard is finished, cancelled,
   * or closed, after the specific method.
   */
  protected void wizardReleased() {}

  /**
   * This method can be used by wizard's pages to
   * disable or enable the "Next" button.
   * @param enabled Whether the "Next" button is enabled.
   */
  public void setNextEnabled(boolean enabled) {
    myNext.setEnabled(enabled);
  }

  /**
   * This method can be used by wizard's pages to
   * disable or enable the "Back" button.
   * @param enabled Whether the "Back" button is enabled.
   */
  protected void setBackEnabled(boolean enabled) {
    myBack.setEnabled(enabled);
  }

  /**
   * This method can be used by wizard's pages to
   * focus the "Back" button.
   */
  protected void focusBack() {
    myBack.focus();
  }

  /**
   * This method can be used by wizard's pages to
   * focus the "Next" button.
   */
  protected void focusNext() {
    myNext.focus();
  }

  /**
   * Checks whether the wizard window is still visible. This
   * method can be used by wizard's pages to make sure that
   * the user hasn't closed the window during a background
   * process or a pause. 
   * @return {@code true} if the wizard window is still open.
   */
  protected boolean isWizardShowing() {
    return myShowing;
  }

  private static Dimension ourButtonMinSize;

  /**
   * Calculates and returns the {@code Dimension} that's used
   * as the minimum, preferred, and maximum size for Back and
   * Next buttons, so they don't change their size when their
   * text changes.
   * @return The button size.
   */
  private static Dimension getButtonMinSize() {
    if(ourButtonMinSize == null) {
      ourButtonMinSize = calculateButtonMinSize();
    }
    return ourButtonMinSize;
  }

  private static Dimension calculateButtonMinSize() {
    final NameMnemonic[] texts = { BACK, NEXT, CANCEL, FINISH };
    final JButton button = new JButton();

    int maxWidth = 0, maxHeight = 0;
    for(final NameMnemonic text : texts) {
      text.setToButton(button);
      final Dimension pref = button.getPreferredSize();
      maxWidth = Math.max(maxWidth, pref.width);
      maxHeight = Math.max(maxHeight, pref.height);
    }

    return new Dimension(maxWidth, maxHeight);
  }

  public WindowController getWindowController() {
    return myBuilder.getWindowContainer().getActor(WindowController.ROLE);
  }

  /**
   * Auxiliary superclass for the "Back" and "Next" actions.
   */
  private static abstract class WizardAction implements AnAction {
    private final SimpleModifiable myModifiable = new SimpleModifiable();

    private String myName = "No Name";
    private boolean myEnabled = true;
    private boolean myFocus = false;
    private JButton myButton;

    public void update(UpdateContext context) throws CantPerformException {
      context.updateOnChange(myModifiable);

      final Component comp = context.getComponent();
      JButton button = Util.castNullable(JButton.class, comp);
      if (button != null) {
        myButton = button;
        adjustButtonSize();
        focusButtonIfNeeded();
      } else myButton = null;

      context.putPresentationProperty(PresentationKey.NAME, myName);
      context.putPresentationProperty(PresentationKey.ENABLE, myEnabled ? EnableState.ENABLED : EnableState.DISABLED);
    }

    private void adjustButtonSize() {
      if(myButton != null) {
        myButton.setMinimumSize(getButtonMinSize());
        myButton.setPreferredSize(getButtonMinSize());
        myButton.setMaximumSize(getButtonMinSize());
      }
    }

    private void focusButtonIfNeeded() {
      if(myButton != null && myFocus) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            myButton.requestFocusInWindow();
          }
        });
        myFocus = false;
      }
    }

    void setName(NameMnemonic name) {
      final String text = name != null ? name.getTextWithMnemonic() : null;
      if(!Util.equals(myName, text)) {
        myName = text;
        myModifiable.fireChanged();
      }
    }

    void setEnabled(boolean enabled) {
      if(enabled != myEnabled) {
        myEnabled = enabled;
        myModifiable.fireChanged();
      }
    }

    void focus() {
      if (myButton != null) myButton.requestFocusInWindow();
      else {
        myFocus = true;
        myModifiable.fireChanged();
      }
    }
  }

  /**
   * "Next" action class.
   * Responsible for handling "Next", "Finish", and "Close" buttons.
   */
  private class NextAction extends WizardAction {
    @Override
    public void update(UpdateContext context) throws CantPerformException {
      super.update(context);
      makeTheButtonDefault(context.getComponent());
    }

    private void makeTheButtonDefault(Component comp) {
      if(comp instanceof JButton) {
        final JRootPane pane = SwingTreeUtil.findAncestorOfType(comp, JRootPane.class);
        if(pane != null) {
          pane.setDefaultButton((JButton)comp);
        }
      }
    }

    public void perform(ActionContext context) throws CantPerformException {
      final WizardPage page = CantPerformException.ensureNotNull(myPageMap.get(myPageID));
      page.nextInvoked();
      showPage(page.getNextPageID());
    }
  }

  /**
   * "Back" action class.
   * Responsible for handling "Back" and "Cancel" buttons.
   */
  private class BackAction extends WizardAction {
    @Override
    public void update(UpdateContext context) throws CantPerformException {
      super.update(context);
      context.putPresentationProperty(PresentationKey.SHORTCUT, Shortcuts.ESCAPE);
    }

    public void perform(ActionContext context) throws CantPerformException {
      final WizardPage page = CantPerformException.ensureNotNull(myPageMap.get(myPageID));
      page.backInvoked();
      showPage(page.getPrevPageID());
    }
  }
}
