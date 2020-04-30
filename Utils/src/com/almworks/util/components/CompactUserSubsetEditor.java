package com.almworks.util.components;

import com.almworks.util.Env;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.Shortcuts;

import java.awt.*;
import java.awt.event.KeyEvent;

import static com.almworks.util.ui.swing.Shortcuts.ksMenu;

/**
 * A variant of CompactSubsetEditor with an additional "myself"
 * item and an "Add Me" button.
 */
public class CompactUserSubsetEditor<T> extends CompactSubsetEditor<T> {
  private static final int ADD_ME_SORTKEY = ADD_SORTKEY - 1;

  private T myMyself;
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final AToolbarButton myAddMeButton;

  public CompactUserSubsetEditor() {
    super();
    myAddMeButton = createAddMeButton();
  }

  public CompactUserSubsetEditor(
    AListModel<? extends T> aListModel, T nothingSelectedItem, T myself)
  {
    super(aListModel, nothingSelectedItem);
    myMyself = myself;
    myAddMeButton = createAddMeButton();
  }

  private AToolbarButton createAddMeButton() {
    final AToolbarButton button = setAction(ADD_ME_SORTKEY, new AddMeAction());
    getJList().addKeyListener(
      UIUtil.pressButtonWithKeyStroke(button,
        ksMenu(KeyEvent.VK_INSERT), ksMenu(KeyEvent.VK_PLUS),
        ksMenu(KeyEvent.VK_EQUALS), ksMenu(KeyEvent.VK_ADD)));
    return button;
  }

  public void setMyself(T myself) {
    myMyself = myself;
    myModifiable.fireChanged();
  }

  @Override
  protected boolean isMyComponent(Component component) {
    if(component == null) {
      return false;
    }
    if(super.isMyComponent(component)) {
      return true;
    }
    return component == myAddMeButton;
  }

  private class AddMeAction extends SimpleAction {
    public AddMeAction() {
      super("", Icons.ADD_ME_ACTION);
      setDefaultPresentation();
      setUpdateSources();
    }

    private void setDefaultPresentation() {
      setDefaultPresentation(PresentationKey.SHORT_DESCRIPTION, getTooltip());
    }

    private String getTooltip() {
      return String.format("Add Me (%s%s)",
        Shortcuts.menu("+"),
        Env.isMac() ? "" : (", " + Shortcuts.menu("Ins")));
    }

    private void setUpdateSources() {
      updateOnChange(getSubsetModel());
      updateOnChange(getFullModel());
      updateOnChange(getInternalModifiable());
      updateOnChange(myModifiable);
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.setEnabled(shouldBeEnabled());
    }

    private boolean shouldBeEnabled() {
      return CompactUserSubsetEditor.this.isEnabled()
        && !isEveryItemSelected()
        && haveLegalMyself()
        && !isMyselfSelected();
    }

    private boolean haveLegalMyself() {
      return myMyself != null && getFullModel().contains(myMyself);
    }

    private boolean isMyselfSelected() {
      return getSubsetModel().contains(myMyself);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      getSubsetAccessor().addSelection(myMyself);
      fireUserModification();
      getJList().requestFocusInWindow();
      getSelectionAccessor().setSelected(myMyself);
    }
  }
}
