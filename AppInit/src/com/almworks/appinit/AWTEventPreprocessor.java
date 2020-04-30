package com.almworks.appinit;

import java.awt.*;

public interface AWTEventPreprocessor {
  boolean preprocess(AWTEvent event, boolean alreadyConsumed);

  boolean postProcess(AWTEvent event, boolean alreadyConsumed);
}
