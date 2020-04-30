package com.almworks.timetrack.gui;

import com.almworks.util.commons.Procedure;
import com.almworks.util.components.ATextField;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.text.parser.ParseException;
import com.almworks.util.ui.DocumentAdapter;
import com.almworks.util.ui.GlobalColors;
import org.almworks.util.Util;

import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * A text field for editing time durations (like 2h 15m).
 * The quickest and dirtiest implementation that could possibly work.
 */
public class DurationField extends ATextField {
  private final Color normalFg = getForeground();
  private final Color errorFg = GlobalColors.ERROR_COLOR;

  private final Procedure<Integer> myListener;

  public DurationField() {
    this(null);
  }

  public DurationField(Procedure<Integer> listener) {
    super();

    myListener = listener;

    getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void documentChanged(DocumentEvent e) {
        checkInput();
      }
    });
  }

  private void checkInput() {
    final String text = Util.NN(getText()).trim();

    Integer value = null;
    boolean errors = false;

    if (text.length() > 0) {
      try {
        value = DateUtil.parseDuration(text, true);
      } catch (ParseException pe) {
        errors = true;
      }
    }

    final Color fg = errors ? errorFg : normalFg;
    if (!Util.equals(getForeground(), fg)) {
      setForeground(fg);
    }

    if(!errors && myListener != null) {
      myListener.invoke(value);
    }
  }

  public void setSeconds(int seconds) {
    setText(DateUtil.getFriendlyDuration(seconds, false));
  }

  public int getSeconds() {
    try {
      return DateUtil.parseDuration(getText(), true);
    } catch (ParseException pe) {
      return -1;
    }
  }
}
