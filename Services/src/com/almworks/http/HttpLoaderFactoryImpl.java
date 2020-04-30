package com.almworks.http;

import com.almworks.api.http.*;
import com.almworks.util.LogHelper;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import java.util.Collection;

public class HttpLoaderFactoryImpl implements HttpLoaderFactory {
  private final SingOnProvider[] myRedirectProviders;

  public HttpLoaderFactoryImpl(SingOnProvider... redirectProviders) {
    myRedirectProviders = redirectProviders;
  }

  public HttpLoader createLoader(HttpMaterial httpMaterial, HttpMethodFactory methodFactory, String escapedUrl) {
    HttpLoaderImpl loader = new HttpLoaderImpl(httpMaterial, methodFactory, escapedUrl);
    URI uri;
    try {
      uri = new URI(escapedUrl, true);
    } catch (URIException e) {
      LogHelper.error(e);
      uri = null;
    }
    for (SingOnProvider provider : myRedirectProviders) {
      Collection<? extends RedirectURIHandler> handlers = provider.getHandlers();
      if (uri != null) provider.prepareHttpClient(httpMaterial.getHttpClient(), uri);
      if (handlers != null && !handlers.isEmpty()) for (RedirectURIHandler handler : handlers) loader.addRedirectUriHandler(handler);
    }
    return loader;
  }
}
