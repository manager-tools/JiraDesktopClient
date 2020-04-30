package com.almworks.services;

import com.almworks.api.gui.MainMenu;
import com.almworks.api.http.HttpProxyInfo;
import com.almworks.util.L;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.AnAbstractAction;
import com.almworks.util.ui.actions.CantPerformException;
import org.picocontainer.Startable;

public class HttpProxyAction implements Startable {
  public static final Role<HttpProxyAction> ROLE = Role.role(HttpProxyAction.class);

  private final ActionRegistry myActionRegistry;
  private final HttpProxyInfo myProxyInfo;

  public HttpProxyAction(ActionRegistry actionRegistry, HttpProxyInfo proxyInfo) {
    myActionRegistry = actionRegistry;
    myProxyInfo = proxyInfo;
  }

  public void start() {
    myActionRegistry.registerAction(MainMenu.Tools.CONFIGURE_PROXY, new AnAbstractAction(L.actionName("Configure HTTP Prox&y\u2026")) {
      public void perform(ActionContext context) throws CantPerformException {
        myProxyInfo.editProxySettings(context);
      }
    });
  }

  public void stop() {
  }
}
