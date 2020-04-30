package com.almworks.gui;

import com.almworks.util.ui.actions.ActionRegistry;
import org.picocontainer.Startable;

/**
 * @author : Dyoma
 */
public class DefaultActionsCollection implements Startable {
  private final ActionRegistry myRegistry;

  public DefaultActionsCollection(ActionRegistry registry) {
    myRegistry = registry;
  }

  public void start() {
    // maybe will be of use later
/*

    myRegistry.registerAction(MainMenu.CLOSE_WINDOW, new AnAbstractAction(L.action("Close")) {
      public void perform(ActionContext context) throws CantPerformException {
        WindowController.CLOSE_WINDOW.perform(context);
      }

      public void update(UpdateContext context) throws CantPerformException {
        super.update(context);
        context.getSourceObject(ApplicationManager.ROLE);
        Window window = context.getSourceObject(WindowController.ROLE).getWindow();
        int defaultCloseOperation;
        defaultCloseOperation = UIUtil.getDefaultCloseOperation(window);
        String name;
        switch (defaultCloseOperation) {
        case JFrame.EXIT_ON_CLOSE:
          name = "E&xit";
          break;
        case JFrame.DISPOSE_ON_CLOSE:
          name = "&Close";
          break;
        case JFrame.HIDE_ON_CLOSE:
          name = "&Hide";
          break;
        case JFrame.DO_NOTHING_ON_CLOSE:
          name = "&Close";
          break;
        default:
          assert false : defaultCloseOperation;
          name = null;
          break;
        }
        context.setEnabled(name != null ? EnableState.ENABLED : EnableState.INVISIBLE);
        context.putPresentationProperty(PresentationKey.NAME, name);
      }
    });
*/
  }

  public void stop() {
  }
}
