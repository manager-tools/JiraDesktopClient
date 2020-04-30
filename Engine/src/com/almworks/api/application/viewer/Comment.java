package com.almworks.api.application.viewer;

import com.almworks.api.application.UiItem;
import com.almworks.util.collections.Containers;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Date;

/**
 * @author dyoma
*/
public interface Comment extends UiItem {
  Comparator<Comment> DATE_COMPARATOR = new Comparator<Comment>() {
    // first come earlier dates, nulls come last
    private final Comparator<Date> dateComp = Containers.comparablesComparator(false);

    public int compare(Comment o1, Comment o2) {
      Date created1 = o1.getWhen();
      Date created2 = o2.getWhen();
      boolean local1 = created1 == null;
      boolean local2 = created2 == null;
      if (local1 || local2) {
        if (!local2) return 1;
        if (!local1) return -1;
        return Util.compareLongs(o1.getItem(), o2.getItem());
      }
      return dateComp.compare(created1, created2);
    }
  };

  String getText();

  String getWhenText();

  Date getWhen();

  String getWhoText();

  @Nullable
  String getHeaderTooltipHtml();
}
