package com.almworks.util.ui.actions.presentation;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationMapping;
import org.almworks.util.Collections15;

import java.util.Collections;
import java.util.List;

/**
 * @author dyoma
 */
public interface PopupEntry {
  PopupEntry SEPARATOR = new PopupEntry() {
  public void addToPopup(ActionGroupVisitor popup) {
    popup.visitSeparator();
  }
};

  void addToPopup(ActionGroupVisitor component);

  abstract class CompositeMenuEntry implements PopupEntry {
    private final List<PopupEntry> myEntries = Collections15.arrayList();

    public CompositeMenuEntry addEntry(PopupEntry entry) {
      myEntries.add(entry);
      return this;
    }

    protected void doAddToMenu(ActionGroupVisitor submenu) {
      for (PopupEntry entry : myEntries) {
        entry.addToPopup(submenu);
      }
    }

    public List<? extends PopupEntry> getEntries() {
      return Collections.unmodifiableList(myEntries);
    }

    public void clearEntries() {
      myEntries.clear();
    }
  }

  class SubMenuEntry extends CompositeMenuEntry {
    private final NameMnemonic myName;

    public SubMenuEntry(String name) {
      myName = NameMnemonic.parseString(name);
    }

    public void addToPopup(final ActionGroupVisitor parent) {
      parent.visitGroup(this, myName);
    }
  }

  class ToggleActionEntry implements PopupEntry {
    private final AnAction myAction;

    public ToggleActionEntry(AnAction action) {
      myAction = action;
    }

    public void addToPopup(ActionGroupVisitor component) {
      component.visitToggle(myAction);
    }
  }


  static class DefaultPopupEntry implements PopupEntry {
    private final AnAction myAction;

    public DefaultPopupEntry(AnAction action) {
      myAction = action;
    }

    public void addToPopup(ActionGroupVisitor component) {
      component.visitDefault(myAction);
    }
  }


  static class EnabledActionsEntry implements PopupEntry {
    private final AListModel<? extends AnAction> myActions;

    public EnabledActionsEntry(AListModel<? extends AnAction> actions) {
      myActions = actions;
    }

    public void addToPopup(ActionGroupVisitor visitor) {
      for (AnAction action : myActions.toList()) {
        visitor.visitAction(action, PresentationMapping.VISIBLE_ONLY_IF_ENABLED);
      }
    }
  }


  static class ActionEntry implements PopupEntry {
    private final AnAction myAction;

    public ActionEntry(AnAction action) {
      myAction = action;
    }

    public void addToPopup(ActionGroupVisitor popup) {
      popup.visitAction(myAction);
    }
  }
}
