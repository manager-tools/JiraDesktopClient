package com.almworks.api.http;

import org.apache.commons.httpclient.URI;
import org.jetbrains.annotations.Nullable;

public interface RedirectURIHandler {
  /**
   * @return null if redirect not handled. Not null value if the handle successfully handle the redirect, may return same or other URI to redirect
   * @throws HttpLoaderException if the handler attempted to handle but the exception has occurred
   */
  @Nullable
  URI approveRedirect(HttpMaterial httpMaterial, URI initialUri, URI redirectUri) throws HttpLoaderException;
}
