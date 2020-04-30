package com.almworks.explorer.qbuilder.editor;

import com.almworks.api.application.qb.CannotSuggestNameException;
import com.almworks.api.application.qb.FilterEditor;
import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.application.tree.UserQueryNode;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.CompositeDialogEditor;
import com.almworks.util.ui.TextEditor;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class QueryEditor extends CompositeDialogEditor {
  private final JComponent myComponent;
  private static final Map<TypedKey<?>, ?> NAME_SUGGESTION_HINTS;
  private static final int MAX_NAME_LENGTH = 80;
  private static final String PLEASE_NAME_THIS_QUERY = "Please name this query";

  static {
    HashMap<TypedKey<?>, ?> map = Collections15.hashMap();
    UserQueryNode.MAX_NAME_LENGTH.putTo(map, MAX_NAME_LENGTH);
    NAME_SUGGESTION_HINTS = Collections.unmodifiableMap(map);
  }

  public QueryEditor(final TextEditor nameEditor, final FilterEditor filterEditor) {
    myComponent = new JPanel(UIUtil.createBorderLayout());
    myComponent.add(filterEditor.getComponent(), BorderLayout.CENTER);
    myComponent.add(UIUtil.labelComponentHorizontal("Na&me:", nameEditor.getComponent()), BorderLayout.NORTH);
    addEditor(nameEditor);
    addEditor(filterEditor);
    filterEditor.getModifiable().addChangeListener(Lifespan.FOREVER, ThreadGate.AWT_QUEUED, new ChangeListener() {
      public void onChange() {
        if (!nameEditor.isDefaultContent())
          return;
        FilterNode filter = filterEditor.getUpToDateFilter();
        String suggestedName = null;
        try {
          suggestedName = filter.getSuggestedName(NAME_SUGGESTION_HINTS);
        } catch (CannotSuggestNameException e) {
          // ignore
        }
        if (suggestedName == null)
          suggestedName = PLEASE_NAME_THIS_QUERY;
        if (suggestedName.length() > MAX_NAME_LENGTH)
          suggestedName = suggestedName.substring(0, MAX_NAME_LENGTH - 1) + "\u2026";
        nameEditor.setDefaultContent(suggestedName);
      }
    });
  }

  public JComponent getComponent() {
    return myComponent;
  }

  protected void disposeComposite() {
  }
}
