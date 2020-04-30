package com.almworks.api.application.viewer;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.ATable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

/**
 * @author dyoma
 */
class CommentState<T extends Comment> {
  private final T myComment;
  private boolean myExpanded = false;
  private Object myParsed;
  public static final Convertor<CommentState<?>, Object> GET_COMMENT = new Convertor<CommentState<?>, Object>() {
    public Object convert(CommentState<?> value) {
      return value.getComment();
    }
  };

  public CommentState(T comment) {
    myComment = comment;
  }

  public boolean equals(Object obj) {
    return obj instanceof CommentState && ((CommentState<?>) obj).myComment.equals(myComment);
  }

  public int hashCode() {
    return myComment.hashCode();
  }

  public String getText() {
    return myComment.getText();
  }

  public T getComment() {
    return myComment;
  }

  public boolean isCollapsed() {
    return !myExpanded;
  }

  public Object getParsedText() {
    return myParsed;
  }

  public void setParsed(Object parsed) {
    myParsed = parsed;
  }

  public String getWho() {
    return myComment.getWhoText();
  }

  public String getWhen() {
    return myComment.getWhenText();
  }

  @Nullable
  public String getHeaderTooltipHtml() {
    return myComment.getHeaderTooltipHtml();
  }

  public void ensureExpanded(ATable<CommentState<T>> table) {
    if (isCollapsed())
      expand(table);
  }

  public void expand(ATable<? extends CommentState<?>> table) {
    if (myExpanded)
      return;
    AListModel<? extends CommentState<?>> model = table.getCollectionModel();
    int index = model.indexOf(this);
    if (index < 0) {
      assert false;
      return;
    }
    myExpanded = true;
    model.forceUpdateAt(index);
  }

  void setExpanded(boolean expanded) {
    myExpanded = expanded;
  }

  public static <T extends Comment> Comparator<CommentState<T>> createOrder(final Comparator<? super T> comparator) {
    return new Comparator<CommentState<T>>() {
      public int compare(CommentState<T> o1, CommentState<T> o2) {
        return comparator.compare(o1.getComment(), o2.getComment());
      }
    };
  }

  public static <T extends Comment> Convertor<CommentState<T>, T> getGetComment() {
    //noinspection RedundantCast,RawUseOfParameterizedType
    return (Convertor<CommentState<T>, T>) (Convertor) GET_COMMENT;
  }
}
