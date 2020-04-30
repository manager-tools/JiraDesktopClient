package com.almworks.settings.engine;

import com.almworks.api.container.EventRouter;
import com.almworks.api.engine.EngineListener;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.misc.WorkArea;
import com.almworks.engine.gui.attachments.AttachmentsControllerUtil;
import com.almworks.items.api.Database;
import com.almworks.store.sqlite.CrashSQLiteAction;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.L;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.almworks.util.Log;
import org.picocontainer.Startable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : Dyoma
 */
public class EngineActions implements Startable {
  private final ActionRegistry myRegistry;
  private final EngineListener myEventSink;

  public EngineActions(ActionRegistry registry, EventRouter eventRouter) {
    myRegistry = registry;
    myEventSink = eventRouter.getEventSink(EngineListener.class, true);
  }

  public void start() {
    myRegistry.registerAction(
      MainMenu.File.RELOAD_CONFIGURATION, new DoSynchronizeAction.ReloadConfig(myEventSink));
    myRegistry.registerAction(
      MainMenu.File.RELOAD_CONFIGURATION_POPUP, new DoSynchronizeAction.ReloadConfigPopup(myEventSink));
    myRegistry.registerAction(
      MainMenu.File.DOWNLOAD_CHANGES_QUICK, new DoSynchronizeAction.GetChangesNow(myEventSink));
    myRegistry.registerAction(
      MainMenu.File.DOWNLOAD_CHANGES_QUICK_POPUP, new DoSynchronizeAction.GetChangesNowPopup(myEventSink));
    myRegistry.registerAction(
      MainMenu.File.CONNECTION_AUTO_SYNC, ToggleAutoSyncAction.TOGGLE);

    AttachmentsControllerUtil.registerActions(myRegistry);
    registerInternalActions();
  }

  private void registerInternalActions() {
    if (!Env.getBoolean(GlobalProperties.INTERNAL_ACTIONS))
      return;

    myRegistry.registerAction(MainMenu.Tools.EXCEPTION, new SimpleAction(L.actionName("Throw Exception")) {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        throw new Failure("Your Exception, Sir.");
      }
    });

    myRegistry.registerAction(MainMenu.Tools.LOG_ERROR, new SimpleAction(L.actionName("Log Error")) {
      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
      }

      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        Log.error(new Throwable("Your Error, Sir."));
      }
    });

    myRegistry.registerAction(MainMenu.Tools.CONNECTION_INTERRUPT, new SimpleAction("Disable Network") {
      private final BasicScalarModel<Boolean> myNetworkDisabled = BasicScalarModel.createWithValue(false, true);

      {
        updateOnChange(myNetworkDisabled.getModel());
      }

      @Override
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.putPresentationProperty(PresentationKey.NAME, myNetworkDisabled.getValue() ? "Enable Net&work" : "Disable Net&work");
      }

      public void doPerform(ActionContext context) throws CantPerformException {
        myNetworkDisabled.setValue(!myNetworkDisabled.getValue());
        System.setProperty("debug.disable.network", myNetworkDisabled.getValue() ? "true" : "false");
      }
    });

    myRegistry.registerAction(MainMenu.Tools.EAT_MEMORY, new AnAbstractAction("Eat Memory") {
      public void perform(ActionContext context) throws CantPerformException {
        ThreadGate.LONG(new Object()).execute(new Runnable() {
          public void run() {
            List<List> filler = Collections15.arrayList();
            while (true) {
              List x = new ArrayList(1000000);
              filler.add(x);
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
                // ignore
              }
            }
          }
        });
      }
    });

    myRegistry.registerAction(MainMenu.Tools.CRASH_SQLITE, new CrashSQLiteAction());


    myRegistry.registerAction(MainMenu.Tools.DUMP_DB, new EnabledAction("Dump Database") {
      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        WorkArea wa = context.getSourceObject(WorkArea.APPLICATION_WORK_AREA);
        context.getSourceObject(Database.ROLE).dump(new File(wa.getRootDir(), "dump.txt").getPath());
        JOptionPane.showMessageDialog(context.getComponent(), "Dump Done");
      }
    });
  }

  public void stop() {

  }
}
