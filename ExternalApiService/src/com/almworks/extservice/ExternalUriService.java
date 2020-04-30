package com.almworks.extservice;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.explorer.ItemUrlService;
import com.almworks.api.misc.CommandLine;
import com.almworks.platform.ComponentLoader;
import com.almworks.util.Env;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ModelUtils;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.MacIntegration;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.almworks.util.Log;
import org.picocontainer.Startable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ExternalUriService implements Startable {
  public static final Role<ExternalUriService> ROLE = Role.role(ExternalUriService.class);

  private final Handler myHandler;
  private final ApplicationLoadStatus myAppStatus;
  private final List<String> myUrlsToOpen;

  public ExternalUriService(ItemUrlService urlService, ApplicationLoadStatus appStatus, CommandLine commandLine) {
    myHandler = new Handler(urlService);
    myAppStatus = appStatus;
    myUrlsToOpen = ComponentLoader.getUrlsToOpen(commandLine.getCommandLine());
  }

  @Override
  public void start() {
    final ScalarModel<Boolean> model = myAppStatus.getApplicationLoadedModel();
    ModelUtils.whenTrue(model, ThreadGate.AWT, myHandler.getInstaller());
    ModelUtils.whenTrue(model, ThreadGate.AWT, getUrlOpener());
  }

  private Runnable getUrlOpener() {
    return new Runnable() {
      @Override
      public void run() {
        for(final String url : myUrlsToOpen) {
          try {
            myHandler.openUri(new URI(url));
          } catch (URISyntaxException e) {
            Log.warn(e);
          }
        }
      }
    };
  }

  @Override
  public void stop() {
    myHandler.uninstall();
  }

  private static class Handler implements Runnable, Procedure<URI> {
    private final ItemUrlService myUrlService;

    public Handler(ItemUrlService urlService) {
      myUrlService = urlService;
    }

    public Runnable getInstaller() {
      return this;
    }

    @Override
    public void run() {
      if(Env.isMac()) {
        installMacHandler();
      }
    }

    private void installMacHandler() {
      if(MacIntegration.isOpenUriHandlerSupported()) {
        MacIntegration.setOpenUriHandler(this);
      } else {
        Log.warn("ExUS: Mac OS X setOpenURIHandler() is not supported.");
      }
    }

    @Override
    public void invoke(URI arg) {
      openUri(arg);
    }

    private void openUri(URI uri) {
      try {
        myUrlService.showItem(uri.toString(), ItemUrlService.ConfirmationHandler.ALWAYS_AGREE);
      } catch (CantPerformExceptionExplained e) {
        Log.error(e);
      }
    }

    public void uninstall() {
      if(Env.isMac()) {
        MacIntegration.setOpenUriHandler(null);
      }
    }
  }
}
