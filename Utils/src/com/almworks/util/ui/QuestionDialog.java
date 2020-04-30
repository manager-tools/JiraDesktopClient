package com.almworks.util.ui;

import com.almworks.util.commons.Procedure;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;

/**
 * @author dyoma
 */
public class QuestionDialog {
  private final FireEventSupport<Procedure<Reply>> myListeners = (FireEventSupport)FireEventSupport.createSynchronized(Procedure.class);
  private final JOptionPane myPane;
  private final Object myLock = new Object();
  private String myTitle;
  private Reply myReply;
  private JDialog myDialog;
  private boolean myCancelled = false;
  private static final int CANCEL_REPLY = -2;
  private static final int NO_OPTION = -1;

  public QuestionDialog() {
    myPane = new JOptionPane();
  }

  public void setTitle(String title) {
    myTitle = title;
  }

  public void setMessage(Object message) {
    myPane.setMessage(message);
  }

  public void setOptions(Object[] options) {
    myPane.setOptions(options);
  }

  public void addReplyListener(Procedure<Reply> listener) {
    boolean hasReply;
    synchronized (myLock) {
      hasReply = myReply != null;
    }
    if (hasReply)
      listener.invoke(myReply);
    else
      myListeners.addStraightListener(Lifespan.FOREVER, listener);
  }

  public void askUser() {
    synchronized (myLock) {
      assert myDialog == null;
      if (myCancelled)
        return;
    }
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        final JDialog dialog;
        synchronized(myLock) {
          if (myCancelled)
            return;
          dialog = myPane.createDialog(null, myTitle);
        }
        myDialog = dialog;
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            synchronized(myLock) {
              if (myCancelled)
                dialog.dispose();
            }
          }
        });
        dialog.show();
        dialog.dispose();
        myDialog = null;
        synchronized(myLock) {
          if (myCancelled)
            return;
          Object value = myPane.getValue();
          Object[] options = myPane.getOptions();
          int index = NO_OPTION;
          for (int i = 0; i < options.length; i++) {
            Object option = options[i];
            if (Util.equals(option, value))
              index = i;
          }
          setReply(new Reply(value, index));
        }
      }
    });
  }

  private void setReply(Reply reply) {
    synchronized (myLock) {
      assert myReply == null : myReply;
      myReply = reply;
    }
    myListeners.getDispatcher().invoke(myReply);
  }

  public void cancelQuestion() {
    boolean hasDialog;
    synchronized(myLock) {
      if (myReply != null)
        return;
      assert !myCancelled;
      myCancelled = true;
      hasDialog = myDialog != null;
      setReply(new Reply(null, CANCEL_REPLY));
    }
    if (hasDialog) {
      myDialog.dispose();
      myDialog = null;
    }
  }

  public static class Reply {
    private final Object myOption;
    private final int myOptionIndex;

    private Reply(Object option, int optionIndex) {
      myOption = option;
      myOptionIndex = optionIndex;
    }

    public Object getOption() {
      return myOption;
    }

    public int getOptionIndex() {
      return myOptionIndex;
    }

    public boolean isCancelled() {
      return myOptionIndex == CANCEL_REPLY;
    }
  }
}
