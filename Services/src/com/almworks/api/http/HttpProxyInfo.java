package com.almworks.api.http;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

public interface HttpProxyInfo {
  Role<HttpProxyInfo> ROLE = Role.role("HttpProxyInfo");
  HttpProxyInfo NO_PROXY = new NoProxy();

  boolean isUsingProxy();

  String getProxyHost();

  int getProxyPort();

//  String getProxyAuthenticationLogin();

  void editProxySettings(ActionContext context) throws CantPerformException;

  boolean isAuthenticatedProxy();

  String getProxyUser();

  String getProxyPassword();

  Modifiable getModifiable();

  public static class NoProxy implements HttpProxyInfo {
    public boolean isUsingProxy() {
      return false;
    }

    public String getProxyHost() {
      return null;
    }

    public int getProxyPort() {
      return -1;
    }

    public Detach addAWTChangeListener(ChangeListener listener) {
      return Detach.NOTHING;
    }

    public void addAWTChangeListener(Lifespan life, ChangeListener listener) {
    }

    public void addChangeListener(Lifespan life, ChangeListener listener) {
    }

    public void addChangeListener(Lifespan life, ThreadGate gate, ChangeListener listener) {
    }


    public void editProxySettings(ActionContext context) throws CantPerformException {
    }

    public String getProxyUser() {
      return null;
    }

    public String getProxyPassword() {
      return null;
    }

    public boolean isAuthenticatedProxy() {
      return false;
    }

    public Modifiable getModifiable() {
      return Modifiable.NEVER;
    }
  }
}
