package com.almworks.spi.provider.util;

import com.almworks.api.engine.CommonConfigurationConstants;
import com.almworks.util.WeakEncryption;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.ui.DocumentAdapter;
import org.almworks.util.Log;
import org.almworks.util.Util;

import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.security.GeneralSecurityException;

public class PasswordUtil {
  public static String getPassword(ReadonlyConfiguration configuration) {
    String setting = configuration.getSetting(CommonConfigurationConstants.PASSWORD, "");
    if (setting.length() > 0) {
      try {
        setting = WeakEncryption.decryptString(setting);
      } catch (GeneralSecurityException e) {
        Log.debug(e);
        setting = "";
      }
    }
    return setting;
  }

  public static void setPassword(Configuration configuration, String password) {
    password = Util.NN(password);
    if (password.length() > 0) {
      password = WeakEncryption.encryptString(password);
    }
    configuration.setSetting(CommonConfigurationConstants.PASSWORD, password);
  }

  public static void attachPasswordField(final JTextComponent component, final Configuration configuration) {
    component.setText(PasswordUtil.getPassword(configuration));
    component.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void documentChanged(DocumentEvent e) {
        PasswordUtil.setPassword(configuration, component.getText());
      }
    });
    PasswordUtil.setPassword(configuration, component.getText());
  }
}
