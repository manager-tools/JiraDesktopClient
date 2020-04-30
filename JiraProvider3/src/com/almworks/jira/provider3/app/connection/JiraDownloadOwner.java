package com.almworks.jira.provider3.app.connection;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.download.CannotCreateLoaderException;
import com.almworks.api.download.DownloadOwner;
import com.almworks.api.engine.ConnectionState;
import com.almworks.api.http.HttpLoaderException;
import com.almworks.api.http.HttpResponseData;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.model.BasicScalarModel;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

class JiraDownloadOwner implements DownloadOwner {
  private final JiraConnection3 myConnection;

  JiraDownloadOwner(JiraConnection3 connection) {
    myConnection = connection;
  }

  public String getDownloadOwnerID() {
    return "jira:" + myConnection.getConnectionID();
  }

  public boolean isValid() {
    return myConnection.getState().getValue() == ConnectionState.READY;
  }

  @Override
  public HttpResponseData load(DetachComposite life, String argument, boolean retrying, boolean noninteractive, BasicScalarModel<Boolean> cancelFlag)
    throws CannotCreateLoaderException, IOException, HttpLoaderException
  {
    RestSession session = myConnection.getConfigHolder().createSession();
    if (session == null) throw new CannotCreateLoaderException();
    try {
      RestResponse response = session.perform(RestSession.GetDelete.get(argument, RestSession.getDebugName(argument)), RequestPolicy.SAFE_TO_RETRY);
      if (!response.isSuccessful()) {
        RestResponse fixedResponse = tryFixArgument(session, argument);
        if (fixedResponse != null) response = fixedResponse;
      }
      response.ensureSuccessful();
      return response.getHttpResponse();
    } catch (ConnectorException e) {
      throw new HttpLoaderException(e.getShortDescription(), e);
    }
  }

  /**
   * Reason: https://support.almworks.com/browse/ALM-889
   * This method tries to fix URL by replacing scheme, host and port of the URL with values from the configured base URL.
   * This may help if JIRA provides file URLs on the wrong host
   * @param session configured JIRA connection
   * @param argument URL of the file to download
   * @return not null if the supplied URL (argument) is differs from the configured base URL. Null if no correction has been done, thus no attempt has been performed.
   * @throws ConnectorException on network failure
   */
  @Nullable
  private RestResponse tryFixArgument(RestSession session, String argument) throws ConnectorException {
    try {
      URI uri = new URI(argument);
      URI baseUrl = new URI(session.getBaseUrl());
      if (Objects.equals(uri.getScheme(), baseUrl.getScheme()) && uri.getPort() == baseUrl.getPort()
        && Objects.equals(uri.getHost(), baseUrl.getHost()))
        return null;
      URI fixed = new URI(baseUrl.getScheme(), baseUrl.getUserInfo(), baseUrl.getHost(), baseUrl.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
      LogHelper.debug("Trying to download from fixed URL", argument, fixed);
      argument = fixed.toString();
    } catch (URISyntaxException e) {
      LogHelper.debug("Failed to fix URL", argument, e);
      return null;
    }
    return session.perform(RestSession.GetDelete.get(argument, RestSession.getDebugName(argument)), RequestPolicy.SAFE_TO_RETRY);
  }
}
