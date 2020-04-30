package com.almworks.api.actions;

import com.almworks.api.application.ItemWrapper;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.UpdateContext;
import org.almworks.util.TypedKey;

import java.util.List;

/**
 * This state is used to simulate instant toggling of a button after performing an action.<br><br>

 * <i>Rationale:</i> Actions that toggle item state may have "toggled" presentation property which represents the state.
 * Usually, action presentation is updated when item is reloaded from DB. But for such toggling actions, this is undesirable
 * because there is a considerable time gap between user invoking an action and the reload (locks need to be obtained, commit to finish, then reload), which
 * looks like "user clicks a button, it appears pressed (because of a click), then action begins to schedule commit and for that time button is unpressed back, and only after
 * some time (after reload) it is pressed".
 * For a better visual feedback, use this class so that button be instantly toggled until "true" state is loaded from DB. <br><br>
 * Keep in mind that instant toggle state may differ from the one resulted from the change (e.g. in case transaction fails).
 */
public class InstantToggleSupport {
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  private final TypedKey<Boolean> myToggleKey;

  public InstantToggleSupport(String debugName) {
    myToggleKey = TypedKey.create(debugName + ".instantToggle");
  }

  public SimpleModifiable getModifiable() {
    return myModifiable;
  }

  public Boolean getState(UpdateContext context) throws CantPerformException {
    Boolean commonState = null;
    for (ItemWrapper wrapper : context.getSourceCollection(ItemWrapper.ITEM_WRAPPER)) {
      Boolean toggled = wrapper.getLastDBValues().get(myToggleKey);
      if (toggled == null) return null;
      commonState = commonState == null || commonState == toggled ? toggled : null;
    }
    return commonState;
  }

  public void setState(ActionContext context, boolean instantToggle) throws CantPerformException {
    List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    for (ItemWrapper wrapper : wrappers) {
      wrapper.getLastDBValues().put(myToggleKey, instantToggle);
    }
    myModifiable.fireChanged();
  }
}
