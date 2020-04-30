package com.almworks.explorer.tree;

import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.NoteNode;
import com.almworks.api.application.tree.QueryResult;
import com.almworks.items.api.Database;
import com.almworks.util.collections.Containers;
import com.almworks.util.components.EditableText;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.ThreadSafe;
import org.jetbrains.annotations.NotNull;

public class NoteNodeImpl extends GenericNodeImpl implements NoteNode {
  public static final String HTML_TEXT = "htmlText";
  private int myOrder = 0;

  public NoteNodeImpl(Database db, String name, Configuration configuration) {
    super(db, new EditableText(name, Icons.ATTENTION), configuration);
    beRemovable();
  }

  @NotNull
  @ThreadSafe
  public QueryResult getQueryResult() {
    return QueryResult.NO_RESULT;
  }

  public boolean isCopiable() {
    return false;
  }

  public NoteNode setName(String name) {
    ((EditableText) getPresentation()).setText(name);
    fireTreeNodeChanged();
    return this;
  }

  public NoteNode setOrder(int order) {
    myOrder = order;
    fireTreeNodeChanged();
    return this;
  }

  public void setHtmlText(String text) {
    getConfiguration().setSetting(HTML_TEXT, text);
  }

  public String getHtmlText() {
    return getConfiguration().getSetting(HTML_TEXT, null);
  }

  public int compareTo(GenericNode node) {
    if (!(node instanceof NoteNodeImpl))
      return super.compareTo(node);
    else
      return Containers.compareInts(myOrder, ((NoteNodeImpl) node).myOrder);
  }
}
