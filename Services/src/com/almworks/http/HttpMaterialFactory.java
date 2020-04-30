package com.almworks.http;

import com.almworks.api.http.*;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpMaterialFactory {
  public static final Role<HttpMaterialFactory> ROLE = Role.role(HttpMaterialFactory.class);

  private final HttpClientProvider myClientProvider;
  private final HttpLoaderFactory myLoaderFactory;

  public HttpMaterialFactory(HttpClientProvider clientProvider, HttpLoaderFactory loaderFactory) {
    myClientProvider = clientProvider;
    myLoaderFactory = loaderFactory;
  }

  @NotNull
  public HttpMaterial create(@Nullable FeedbackHandler feedbackHandler, boolean ignoreProxy, String userAgent) {
    HttpClientProvider clientProvider = myClientProvider;
    if (clientProvider == null)
      clientProvider = HttpClientProvider.SIMPLE;
    HttpLoaderFactory loaderFactory = myLoaderFactory;
    if (loaderFactory == null)
      loaderFactory = HttpLoaderFactory.SIMPLE;
    DefaultHttpMaterial material = new DefaultHttpMaterial(clientProvider, loaderFactory);
    if (feedbackHandler != null)
      material.setFeedbackHandler(feedbackHandler);
    // todo configure charset as in jira
    String charset = Env.getString(GlobalProperties.FORCE_OVERRIDE_CHARSET);
    if (charset == null || charset.length() == 0) charset = "UTF-8";
    material.setCharset(charset);
    if (ignoreProxy)
      material.setIgnoreProxy(true);
    material.setUserAgent(userAgent);
    return material;
  }
}
