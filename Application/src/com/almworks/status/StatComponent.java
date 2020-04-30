package com.almworks.status;

import com.almworks.api.gui.StatusBarComponent;
import com.almworks.api.gui.StatusBarLink;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.util.English;
import com.almworks.util.Terms;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.plaf.LinkUI;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.DoubleBottleneck;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionListenerBridge;
import com.almworks.util.ui.actions.AnActionListener;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import util.concurrent.Synchronized;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

abstract class StatComponent implements StatusBarComponent {
  private static final EmptyBorder INVISIBLE_BORDER = new EmptyBorder(0, 0, 0, 0);
  private static final EmptyBorder VISIBLE_BORDER = new EmptyBorder(0, 4, 0, 4);

  private final StatusBarLink myLabel;
  private final Lifecycle myLife = new Lifecycle();
  private final Lifecycle myActionLife = new Lifecycle();
  protected final Synchronized<Integer> myCount = new Synchronized<Integer>(null);

  private int myLastCount = -1;

  private final DoubleBottleneck myBottleneck;
  private final Bottleneck myRecountLater;

  private boolean myUnclickable = false;

  protected StatComponent(final Database db, @NotNull Icon icon, ThreadGate countGate) {
    myBottleneck = new DoubleBottleneck(50, 300, countGate, new Runnable() {
      public void run() {
        doCount(db);
      }
    });
    myRecountLater = new Bottleneck(500, ThreadGate.STRAIGHT, myBottleneck);

    myLabel = new StatusBarLink();
    myLabel.setIcon(icon);
    myLabel.setText(getLabelText());
    myLabel.setIconTextGap(4);
    myLabel.setVisible(false);
    myLabel.setBorder(INVISIBLE_BORDER);
  }

  private void doCount(Database db) {
    if (isCountNeedingDatabase()) {
      db.readBackground(new ReadTransaction<Integer>() {
        public Integer transaction(DBReader reader) {
          int c = count(reader);
          if (c >= 0) {
            myCount.set(c);
          }
          return c;
        }
      }).finallyDo(ThreadGate.AWT, new Procedure<Integer>() {
        public void invoke(Integer count) {
          if (count == null || count < 0) {
            myRecountLater.requestDelayed();
          } else {
            showCount(count);
          }
        }
      });
    } else {
      int count = count(null);
      if (count >= 0) {
        myCount.set(count);
      }
      if (count < 0) {
        myRecountLater.requestDelayed();
      } else {
        showCount(count);
      }
    }
  }

  public int getReservedWidth() {
    return 0;
  }

  public void attach() {
    myBottleneck.requestDelayed();
  }

  public JComponent getComponent() {
    return myLabel;
  }

  public void dispose() {
    myBottleneck.abort();
    myLife.cycle();
    myActionLife.cycle();
  }

  protected Lifespan lifespan() {
    return myLife.lifespan();
  }

  public boolean isShowUnknownAndZero() {
    return false;
  }

  protected void addActionListener(final AnActionListener listener) {
    myActionLife.cycle();
    UIUtil.addActionListener(myActionLife.lifespan(), myLabel, ActionListenerBridge.listener(listener));
  }

  protected static String items(int count) {
    return count + " " + English.getSingularOrPlural(Local.text(Terms.key_artifact), count);
  }

  /**
   * @return the new count, or -1 if stats should be counted a little bit later
   */
  protected abstract int count(DBReader reader);

  protected abstract boolean isCountNeedingDatabase();

  protected void recount() {
    myBottleneck.requestDelayed();
  }

  protected String getTooltip(int count) {
    return null;
  }

  protected void setUnclickable() {
    myUnclickable = true;
    Color color = myLabel.getForeground();
    myLabel.setHoverColor(color);
    myLabel.setPressedColor(color);
    myLabel.setUnderlined(false);
    myLabel.setEnabled(false);
    myLabel.setDisabledLook(LinkUI.NormalPaint.createDefault());
  }

  public boolean isUnclickable() {
    return myUnclickable;
  }

  private void showCount(int count) {
    if (count != myLastCount) {
      myLastCount = count;
      myLabel.setText(getLabelText());
      myLabel.setToolTipText(getTooltip(count));
      if (count > 0 || isShowUnknownAndZero()) {
        if (!myLabel.isVisible()) {
          myLabel.setVisible(true);
          myLabel.setBorder(VISIBLE_BORDER);
        }
      } else {
        if (myLabel.isVisible()) {
          myLabel.setVisible(false);
          myLabel.setBorder(INVISIBLE_BORDER);
        }
      }
    }
  }

  private String getLabelText() {
    return myLastCount < 0 ? "?" : Integer.toString(myLastCount);
  }
}
