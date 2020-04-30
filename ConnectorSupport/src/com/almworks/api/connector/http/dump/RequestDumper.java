package com.almworks.api.connector.http.dump;

import com.almworks.api.connector.http.HttpDumper;
import com.almworks.api.http.HttpLoader;
import com.almworks.api.http.HttpMaterial;
import com.almworks.api.http.HttpResponseData;
import com.almworks.api.http.HttpUtils;
import com.almworks.util.LogPrivacyPolizei;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.NameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public abstract class RequestDumper {
  private static final RequestDumper DUMMY = new RequestDumper() {
    @Override
    public void finishWithException(Throwable e) {}

    @NotNull
    @Override
    public ResponseDumper responseObtained(@NotNull HttpResponseData response) {
      return new ResponseDumper(response);
    }

    @Override
    public void addRequestHeaders(Map<String, String> requestHeaders) {
    }

    @Override
    public void setRawRequest(byte[] bytes) {
    }

    @Override
    public void setPostParameters(List<NameValuePair> parametersList) {
    }

    @Override
    public void addMessage(String message) {
    }
  };
  @NotNull
  public static RequestDumper prepare(List<HttpDumper.DumpSpec> dumperSpec, HttpLoader loader, String script, @Nullable LogPrivacyPolizei polizei, String url) {
    if (dumperSpec == null || dumperSpec.isEmpty()) return DUMMY;
    HttpMaterial material = loader.getMaterial();
    Cookie[] cookies = material.getHttpClient().getState().getCookies();
    FileRequestDumper dumper = new FileRequestDumper(dumperSpec, material, script, polizei, url, HttpUtils.cookieDump(cookies));
    loader.setReportAcceptor(dumper);
    return dumper;
  }

  public abstract void finishWithException(@Nullable Throwable e);

  @NotNull
  public abstract ResponseDumper responseObtained(@NotNull HttpResponseData response);

  public abstract void addRequestHeaders(Map<String, String> requestHeaders);

  public abstract void setRawRequest(byte[] bytes);

  public abstract void setPostParameters(List<NameValuePair> parametersList);

  public abstract void addMessage(String message);
}
