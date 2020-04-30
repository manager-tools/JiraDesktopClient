package com.almworks.api.http;

import com.almworks.util.properties.Role;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface HttpLoaderFactory {
  Role<HttpLoaderFactory> ROLE = Role.role(HttpLoaderFactory.class);
  HttpLoaderFactory SIMPLE = new DumbHttpLoaderFactory();

  HttpLoader createLoader(HttpMaterial httpMaterial, HttpMethodFactory methodFactory, String escapedUrl);

  /**
   * Container components that implement this interface is asked to provide {@link #getHandlers() redirect handlers}. All these handlers are added as
   * {@link HttpLoader#addRedirectUriHandler(RedirectURIHandler) redirect handler} to each {@link HttpLoader HTTP loader} created by {@link HttpLoaderFactory container HTTP loader factory}.<br>
   * Also container components that implements this interface can {@link #prepareHttpClient(org.apache.commons.httpclient.HttpClient, org.apache.commons.httpclient.URI) set up HTTP client}
   * when new loader is going to visit an URI.<br><br>
   * <b>Use cases</b><br>
   * Single-sing-on support may add session cookies to HTTP client (when {@link #prepareHttpClient(org.apache.commons.httpclient.HttpClient, org.apache.commons.httpclient.URI) before new loader is created}
   * to avoid use of anonymous sessions.<br>
   * It may provide redirect handler to detect login request from the server (and perform login if required).
   *
   */
  interface SingOnProvider {
    SingOnProvider DUMMY = new SingOnProvider() {
      @Override
      public void prepareHttpClient(HttpClient client, URI target) {
      }

      @Override
      public Collection<RedirectURIHandler> getHandlers() {
        return Collections.emptyList();
      }
    };

    /**
     * @return collection of redirect handlers for newly created HTTP loader.<br>
     * Null return value means empty collection - no redirect handlers are provided by this implementation.
     */
    @Nullable
    Collection<? extends RedirectURIHandler> getHandlers();

    /**
     * Called to set up HTTP client when new loader is created for an URI
     * @param client client of just created HTTP loader
     * @param target URI the loader is going to visit
     */
    void prepareHttpClient(HttpClient client, URI target);
  }
}
