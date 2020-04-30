package com.almworks.api.http;

import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.ProcedureE;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.apache.commons.httpclient.URI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public class DumbHttpLoaderFactory implements HttpLoaderFactory {
  public HttpLoader createLoader(HttpMaterial httpMaterial, HttpMethodFactory methodFactory, final String escapedUrl) {
    return new HttpLoader() {

      public void setRetries(int retries) {
      }

      public HttpMaterial getMaterial() {
        return null;
      }

      public void setRedirectMethodFactory(HttpMethodFactory factory) {
      }

      public void setFailedStatusApprover(Condition<Integer> failedStatusCodeApprover) {
      }

      public void setReportAcceptor(HttpReportAcceptor reportAcceptor) {
      }

      public void addRedirectUriHandler(RedirectURIHandler handler) {
      }

      @Override
      public void setFollowRedirect(Function<String, Boolean> followRedirect) {
      }

      @Override
      public void setCopyQueryOnRedirect(boolean copyQueryOnRedirect) {
      }

      public HttpResponseData load() {
        return new HttpResponseData() {
          private InputStream myStream;

          @Override
          public void readStream(ProcedureE<InputStream, IOException> reader) throws IOException {
            InputStream stream = getContentStream();
            try {
              reader.invoke(stream);
            } finally {
              IOUtils.closeStreamIgnoreExceptions(stream);
              myStream = null;
            }
          }

          public InputStream getContentStream() {
            try {
              if (myStream == null)
                myStream = new URL(escapedUrl).openStream();
              return myStream;
            } catch (IOException e) {
              throw new Failure(e);
            }
          }

          public String getContentType() {
            return null;
          }

          public String getFullContentType() {
            return null;
          }

          public String getContentFilename() {
            return null;
          }

          public long getContentLength() {
            return -1;
          }

          @Nullable
          public URI getLastURI() {
            return null;
          }

          @Override
          public int getStatusCode() {
            return 0;
          }

          @NotNull
          @Override
          public String getStatusText() {
            return "";
          }

          @Override
          public String getCharset() {
            return IOUtils.DEFAULT_CHARSET;
          }

          @Override
          public void releaseConnection() {
          }

          @NotNull
          public Map<String, String> getResponseHeaders() {
            return Collections15.emptyMap();
          }
        };
      }
    };
  }
}
