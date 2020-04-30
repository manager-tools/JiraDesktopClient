package com.almworks.util.components;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.debug.DebugFrame;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.TimeZone;

public class ADateFieldDemo {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JPanel panel = new JPanel(new FlowLayout());
        TimeZone timeZone = TimeZone.getTimeZone("Pacific/Apia");
        LAFUtil.initializeLookAndFeel();
        final ADateField field = new ADateField(ADateField.Precision.DAY, timeZone);
        field.setDate(new Date());
        final JTextField shower = new JTextField(20);
        final JLabel label = new JLabel();
        field.getDateModel().addChangeListener(Lifespan.FOREVER, new ChangeListener() {
          public void onChange() {
            Date value = field.getDateModel().getValue();
            shower.setText(String.valueOf(value));
            label.setText(value == null ? "null" : String.valueOf(value.getTime()));
          }
        });
        panel.add(new JLabel(timeZone.getID()));
        panel.add(field);
        panel.add(shower);
        panel.add(label);
        DebugFrame.show(panel, 500, 200, true);
      }
    });
  }
}
