package com.almworks.util.xmlrpc;

import com.almworks.dup.util.ApiLog;

import java.util.Collection;

public abstract class OutgoingMessage {
  protected abstract String getRpcMethod();

  protected abstract Collection<?> getRpcParameters();

  protected void requestFailed(Exception problem) {
    ApiLog.debug(this + " failed", problem);
  }

  protected void requestDone(Object result) {
    if (!preprocessResponse(result)) {
      // ApiLog.debug(this + " done: " + result);
    }
  }

  protected final boolean preprocessResponse(Object result) {
    if (!(result instanceof String))
      return false;
    String message = ((String) result);
    if (XmlRpcUtils.RESPONSE_OK.equals(message)) {
      responseOk();
      return true;
    }
    for (String response : XmlRpcUtils.FAILURE_RESPONSES) {
      if (message.startsWith(response)) {
        int len = response.length();
        String payload;
        if (message.length() > len) {
          if (message.charAt(len) != ':')
            continue;
          else
            payload = message.substring(len + 1);
        } else {
          payload = "";
        }
        responseError(response, payload);
        return true;
      }
    }
    return false;
  }

  protected void responseError(String error, String payload) {
    ApiLog.debug(this + " error: " + error + " " + payload);
  }

  protected void responseOk() {
    ApiLog.debug(this + " done");
  }

  public String toString() {
    return getRpcMethod() + XmlRpcUtils.dumpCollection(new StringBuffer(), getRpcParameters()).toString();
  }
}
