package com.almworks.api.engine.util;

import com.almworks.engine.gui.TextController;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersion;
import org.almworks.util.StringUtil;

/**
 * @author dyoma
 */
public class MatchAllHelper {
  private final StringUtil.FindAnyMethod myFindAny;

  public MatchAllHelper(char[][] wordChars) {
    myFindAny = new StringUtil.FindAnyMethod(wordChars, true);
  }

  public boolean matchAttr(ItemVersion item, DBAttribute<String> attr) {
    return item != null && matchString(item.getValue(attr));
  }

  public boolean matchHumanText(ItemVersion item, DBAttribute<String> attr) {
    return matchString(TextController.toHumanText(item != null ? item.getValue(attr) : ""));
  }

  public boolean matchString(String s) {
    if (s == null)
      return false;
    char[] chars = s.toCharArray();
    int changed = myFindAny.perform(chars, false);
    return changed > 0 && myFindAny.areAllFound();
  }
}
