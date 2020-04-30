package com.almworks.timetrack.gui.bigtime;

import com.almworks.items.api.Database;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerWindow;
import com.almworks.timetrack.gui.TimeTrackerForm;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Procedure;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * The action that shows the Big Time value selected by
 * the user and allows to select any other kind of Big Time
 * from the list provided by {@link BigTimeRegistry}. 
 */
public class SelectBigTimeAction extends SimpleAction implements Procedure<String> {
  private static final Font BIGGER_FONT = new Font("Monospaced", Font.BOLD, 32);
  private static final Font SMALLER_FONT = new Font("Monospaced", Font.BOLD, 24);
  private static final Color TEXT_COLOR = ColorUtil.between(
    UIUtil.getEditorForeground(), UIUtil.getEditorBackground(), 0.35F);

  private final Detach myDetach = new Detach() {
    @Override
    protected void doDetach() throws Exception {
      doSetBigTime(null, null);
    }
  };

  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private BigTime myBigTime;
  private String myTimeText;

  public SelectBigTimeAction() {
    super(BigTime.EMPTY_VALUE);
    watchRole(TimeTracker.TIME_TRACKER);
    TimeTrackerForm.TRACKING_TICKS.install(this);
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, "Select the value to display");
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.ENABLED);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.updateOnChange(myModifiable);

    final TimeTracker tt = context.getSourceObject(TimeTracker.TIME_TRACKER);
    if(tt != null) {
      context.updateOnChange(tt.getModifiable());
    }

    if(myBigTime == null) {
      loadBigTime(context);
    }

    if(myBigTime != null) {
      context.putPresentationProperty(
        PresentationKey.SHORT_DESCRIPTION, myBigTime.getDescription());
      myBigTime.getBigTimeText(context.getSourceObject(Database.ROLE), context.getSourceObject(TimeTracker.TIME_TRACKER), this);
    }

    final String text = Util.NN(myTimeText, BigTime.EMPTY_VALUE);
    context.getComponent().setFont(text.length() <= 8 ? BIGGER_FONT : SMALLER_FONT);
    context.getComponent().setForeground(TEXT_COLOR);
    context.putPresentationProperty(PresentationKey.NAME, text);
    context.setEnabled(BigTimeRegistry.hasAvailableBigTimes());
  }

  private void loadBigTime(UpdateContext context) throws CantPerformException {
    final TimeTrackerWindow ttw = context.getSourceObject(TimeTrackerWindow.ROLE);
    BigTime bigTime = BigTimeRegistry.getBigTime(ttw.getBigTimeId());
    if(bigTime == null) {
      bigTime = BigTimeRegistry.getDefaultBigTime();
    }
    doSetBigTime(context.getSourceObject(Database.ROLE), bigTime);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    final JPopupMenu menu = createPopupMenu(context);
    showPopupMenu(context, menu);
  }

  private JPopupMenu createPopupMenu(ActionContext context) throws CantPerformException {
    final JPopupMenu menu = UIUtil.createJPopupMenu();

    final TimeTrackerWindow ttw = context.getSourceObject(TimeTrackerWindow.ROLE);
    Database db = context.getSourceObject(Database.ROLE);
    for(final BigTime bigTime : BigTimeRegistry.getAvailableBigTimes()) {
      menu.add(new BigTimeItem(bigTime, ttw, db));
    }

    menu.pack();
    return menu;
  }

  private void showPopupMenu(ActionContext context, JPopupMenu menu) {
    final Component comp = context.getComponent();
    menu.show(comp, 0, comp.getHeight());
  }

  @Override
  public void invoke(String arg) {
    myTimeText = arg;
    myModifiable.fireChanged();
  }

  private void setBigTime(Database db, BigTime bigTime) {
    if(doSetBigTime(db, bigTime)) {
      myModifiable.fireChanged();
    }
  }

  private boolean doSetBigTime(Database db, BigTime bigTime) {
    if(bigTime != myBigTime) {
      if(myBigTime != null) {
        myBigTime.detach();
      }
      myBigTime = bigTime;
      if(myBigTime != null && db != null) {
        myBigTime.attach(db, myModifiable);
      }
      return true;
    }
    return false;
  }

  public Detach getDetach() {
    return myDetach;
  }

  private final class BigTimeItem extends JCheckBoxMenuItem implements ItemListener {
    private final BigTime myBigTime;
    private final TimeTrackerWindow myWindow;
    private final Database myDb;

    public BigTimeItem(@NotNull BigTime bigTime, @NotNull TimeTrackerWindow window, Database db) {
      super(bigTime.getName());

      myBigTime = bigTime;
      myWindow = window;
      myDb = db;

      setToolTipText(myBigTime.getDescription());

      final BigTime formTime = SelectBigTimeAction.this.myBigTime;
      if(formTime != null && formTime.getId().equals(myBigTime.getId())) {
        setSelected(true);
      }

      addItemListener(this);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
      if(e.getStateChange() == ItemEvent.SELECTED) {
        setBigTime(myDb, myBigTime);
        myWindow.setBigTimeId(myBigTime.getId());
      }
    }
  }
}