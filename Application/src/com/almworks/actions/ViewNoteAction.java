package com.almworks.actions;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.tree.NoteNode;
import com.almworks.util.ErrorHunt;
import com.almworks.util.L;
import com.almworks.util.ui.UIComponentWrapper2Support;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.Detach;

import javax.swing.*;

public class ViewNoteAction extends SimpleAction {
  public ViewNoteAction() {
    super("View &Note");
    setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION,
      L.tooltip("View note text"));
    watchRole(GenericNode.NAVIGATION_NODE);
    setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
  }

  protected void customUpdate(UpdateContext context) throws CantPerformException {
    GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    if (node instanceof NoteNode) {
      String text = ((NoteNode) node).getHtmlText();
      if (text != null)
        context.setEnabled(EnableState.ENABLED);
    }
  }

  protected void doPerform(ActionContext context) throws CantPerformException {
    ExplorerComponent explorer = context.getSourceObject(ExplorerComponent.ROLE);
    if (explorer == null)
      return;
    GenericNode node = context.getSourceObject(GenericNode.NAVIGATION_NODE);
    if (node instanceof NoteNode) {
      NoteNode note = ((NoteNode) node);
      String text = note.getHtmlText();
      if (text != null)
        explorer.showComponent(new NoteDisplay(text), note.getName());
    }
  }

  private static class NoteDisplay extends UIComponentWrapper2Support {
    private final JEditorPane myEditorPane = new JEditorPane();
    private final JScrollPane myScrollPane = new JScrollPane(myEditorPane);

    public NoteDisplay(String text) {
      myEditorPane.setContentType("text/html");
      myEditorPane.setEditable(false);
      ErrorHunt.setEditorPaneText(myEditorPane, text);
    }

    public Detach getDetach() {
      return new Detach() {
        protected void doDetach() {
          ErrorHunt.setEditorPaneText(myEditorPane, "");
        }
      };
    }

    public JComponent getComponent() {
      return myScrollPane;
    }
  }
}
