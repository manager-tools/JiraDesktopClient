package com.almworks.sumtable;

import java.awt.*;

interface STFilterController {
  void runQuery(boolean newTab);

  void setFeedbackCursor(Cursor cursor);

  void setFeedbackTooltip(String string);
}
