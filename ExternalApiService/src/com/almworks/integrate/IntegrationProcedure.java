package com.almworks.integrate;

import javax.swing.*;

interface IntegrationProcedure {
  String getTitle();

  boolean integrate();

  JComponent getComponent();

  boolean checkAvailability();
}
