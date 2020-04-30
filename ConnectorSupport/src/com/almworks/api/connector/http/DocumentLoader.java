package com.almworks.api.connector.http;

import com.almworks.api.connector.CancelledException;
import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.ConnectorLoaderException;
import com.almworks.api.http.*;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.files.FileUtil;
import com.almworks.util.io.IOUtils;
import com.almworks.util.io.XMLCharValidator;
import com.almworks.util.xml.JDOMUtils;
import com.almworks.util.xml.ZeroEntityResolver;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Utility class for building XML documents from URLs.
 *
 * @author sereda
 */
public class DocumentLoader {
  private final File myFile;
  @Nullable
  private final HttpDumper myDumper;
  private final HttpMaterial myHttpMaterial;
  private final String myEscapedUrl;

  private HttpResponseData myRawResponse;
  private String myResponse;

  private List<RedirectURIHandler> myRedirectUriHandlers;
  private boolean myNoRetries;

  public DocumentLoader(HttpMaterial material, String escapedUrl, List<HttpDumper.DumpSpec> specs) {
    myHttpMaterial = material;
    myEscapedUrl = escapedUrl;
    myFile = null;
    if (specs != null && !specs.isEmpty()) {
      myDumper = new HttpDumper(material, specs);
      myDumper.setUrl(escapedUrl);
    } else
      myDumper = null;
  }

  public DocumentLoader(HttpMaterial material, File file) {
    myHttpMaterial = material;
    myEscapedUrl = null;
    myFile = file;
    myDumper = null;
  }

  // for tests
  DocumentLoader(HttpMaterial material) {
    myHttpMaterial = material;
    myEscapedUrl = "http://almworks.com/testing";
    myFile = null;
    myDumper = null;
  }

  public synchronized DocumentLoader noRetries() {
    myNoRetries = true;
    return this;
  }

  public synchronized boolean hasResponse() {
    return myResponse != null || myRawResponse != null;
  }

  public synchronized DocumentLoader httpGET() throws ConnectorException {
    if (myEscapedUrl == null) {
      assert false : this;
      return this;
    }
    assert !hasResponse();
    if (hasResponse())
      return this;
    try {
      myRawResponse = doGET();
      assert hasResponse();
      return this;
    } catch (IOException e) {
      Log.debug("connection failure", e);
      throw new ConnectionException(myEscapedUrl, "connection failure", e);
    }
  }

  // for testing
  synchronized DocumentLoader setResponse(HttpResponseData response) throws ConnectorException {
    if (myEscapedUrl == null) {
      assert false : this;
      return this;
    }
    assert !hasResponse();
    if (hasResponse())
      return this;
    myRawResponse = response;
    assert hasResponse();
    return this;
  }

  public synchronized DocumentLoader httpPOST(Collection<NameValuePair> parameters) throws ConnectorException {
    return httpPOST(parameters, false);
  }

  public synchronized DocumentLoader httpPOST(MultiMap<String, String> parameters) throws ConnectorException {
    return httpPOST(parameters, false);
  }

  public synchronized DocumentLoader httpPOST(MultiMap<String, String> parameters, boolean useGetOnRedirect)
    throws ConnectorException
  {
    List<NameValuePair> list = HttpUtils.convertToNVP(parameters);
    return httpPOST(list, useGetOnRedirect);
  }

  public synchronized DocumentLoader httpPOST(Collection<NameValuePair> parameters, boolean useGetOnRedirect)
    throws ConnectorException
  {
    assert myEscapedUrl != null;
    assert !hasResponse();
    if (hasResponse())
      return this;
    try {
      myRawResponse = doPOST(parameters, useGetOnRedirect);
      assert hasResponse();
      return this;
    } catch (IOException e) {
      Log.debug("connection failure", e);
      throw new ConnectionException(myEscapedUrl, "connection failure", e);
    }
  }

  /**
   * Instead of httpGet, load response from a file. Useful for testing.
   */
  public synchronized DocumentLoader fileLoad() throws ConnectorException {
    assert myFile != null;
    assert !hasResponse();
    FileInputStream stream = null;
    try {
      String charset = myHttpMaterial.getCharset();
      if (charset == null)
        myResponse = FileUtil.readFile(myFile);
      else
        myResponse = FileUtil.readFile(myFile.getPath(), charset);
      return this;
    } catch (IOException e) {
      throw new ConnectorException("cannot load " + myFile, e, "", "");
    } finally {
      IOUtils.closeStreamIgnoreExceptions(stream);
    }
  }

  @NotNull
  public synchronized Document loadHTML() throws ConnectorException {
    assert hasResponse();
    if (!hasResponse())
      return new Document();
    String response = null;
    try {
      response = getStringResponse();
//    Reader reader = new XML10CorrectingStringReader(myResponse);
      return parseHTML(new InputSource(new StringReader(response)));
    } catch (IOException e) {
      Log.debug("load failure", e);
      throw new ConnectionException(myEscapedUrl, "load failure", e);
    } catch (SAXException e) {
      Log.warn("cannot parse html output:\n----------[ " + myEscapedUrl + " ]----------\n" + response +
        "\n-------------------------------------");
      Log.warn(e);
      throw new CannotParseException(myEscapedUrl, "cannot parse html output", e);
    }
  }

  public synchronized Document loadXML() throws ConnectorException {
    assert hasResponse();
    if (!hasResponse())
      return new Document();
    String response = null;
    try {
      response = getStringResponse();
//    Reader reader = new XML10CorrectingStringReader(myResponse);
      return parseXML(new InputSource(new StringReader(response)));
    } catch (IOException e) {
      Log.warn("connection failure", e);
      throw new ConnectionException(myEscapedUrl, "connection failure", e);
    } catch (JDOMException e) {
      Log.warn("cannot parse xml output:\n----------[ " + myEscapedUrl + " ]----------\n" + response +
        "\n-------------------------------------");
      Log.warn(e);
      throw new CannotParseException(myEscapedUrl, "cannot parse xml", e);
    }
  }

  public synchronized String loadString() throws ConnectorException {
    assert hasResponse() : this;
    if (!hasResponse())
      return "";
    try {
      return getStringResponse();
    } catch (IOException e) {
      Log.warn("connection failure", e);
      throw new ConnectionException(myEscapedUrl, "connection failure", e);
    }
  }

  private String getStringResponse() throws IOException {
    assert myRawResponse != null || myResponse != null;
    if (myResponse == null) {
      myResponse = transferString(myRawResponse);
    }
    return myResponse;
  }

  private HttpResponseData doGET() throws IOException, ConnectorException {
    HttpLoader loader = myHttpMaterial.createLoader(myEscapedUrl, new HttpMethodFactory() {
      public HttpMethodBase create() throws HttpMethodFactoryException {
        GetMethod method;
        method = HttpUtils.createGet(myEscapedUrl);
        fixUrl(method, myEscapedUrl);
        return method;
      }
    });
    return requestRaw(loader);
  }

  private static void fixUrl(GetMethod method, String url) {
    try {
      URI uri = new URI(url, true);
      boolean hasIllegal = uri.getHost().indexOf('_') >= 0;
      if (hasIllegal) {
        int schemeEnd = url.indexOf("://");
        int hostStart = schemeEnd < 0 ? 0 : schemeEnd + 3;
        int hostEnd = url.indexOf("/", hostStart);
        String hostAndPort = hostEnd > 0 ? url.substring(hostStart, hostEnd) : url.substring(hostStart);
        int colon = hostAndPort.indexOf(':');
        String host = colon < 0 ? hostAndPort : hostAndPort.substring(0, colon);
        String portString = colon < 0 ? null : hostAndPort.substring(colon + 1);
        if (host.length() > 0) {
          final int overridePort = Util.toInt(portString, -1);
          URI newUri = new URI(url, true) {
            public int getPort() {
              return overridePort;
            }
          };
          method.setURI(newUri);
        }
      }
    } catch (URIException e) {
      // ignore
      Log.debug(e);
    }
  }

  private String transferString(@NotNull HttpResponseData response) throws IOException {
    String result = HttpUtils.transferToString(response, XMLCharValidator.INSTANCE);
    if (myDumper != null) myDumper.setResponse(result);
    return result;
  }

  private HttpResponseData requestRaw(HttpLoader loader)
    throws IOException, CancelledException, HttpFailureConnectionException
  {
    try {
      if (myRedirectUriHandlers != null) {
        for (RedirectURIHandler redirectUriHandler : myRedirectUriHandlers) {
          loader.addRedirectUriHandler(redirectUriHandler);
        }
      }
      if (myDumper != null) {
        loader.setReportAcceptor(myDumper);
        myDumper.saveCookiesBeforeRequest();
      }
      if (myNoRetries) {
        loader.setRetries(1);
      }
      HttpResponseData response = loader.load();
      if (myDumper != null) {
        Map<String, String> headers = response.getResponseHeaders();
        if (headers.size() > 0) {
          myDumper.setResponseHeaders(headers);
        }
      }
      return response;
    } catch (HttpCancelledException e) {
      throw new CancelledException(e);
    } catch (HttpConnectionException e) {
      throw new HttpFailureConnectionException(myEscapedUrl, e.getStatusCode(), e.getStatusText());
    } catch (HttpLoaderException e) {
      throw new ConnectorLoaderException(e);
    }
  }


  private HttpResponseData doPOST(final Collection<NameValuePair> parameters, boolean useGetOnRedirect)
    throws ConnectorException, IOException
  {
    if (myDumper != null) {
      myDumper.setPostParameters(parameters);
    }
    HttpLoader loader = myHttpMaterial.createLoader(myEscapedUrl, new HttpMethodFactory() {
      public HttpMethodBase create() throws HttpMethodFactoryException {
        PostMethod post = HttpUtils.createPost(myEscapedUrl);
        post.addParameters(parameters.toArray(new NameValuePair[parameters.size()]));
        return post;
      }
    });
    if (useGetOnRedirect) {
      loader.setRedirectMethodFactory(new HttpMethodFactory() {
        public HttpMethodBase create() throws HttpMethodFactoryException {
          return HttpUtils.createGet(myEscapedUrl);
        }
      });
    }
    return requestRaw(loader);
  }

  @NotNull
  private Document parseHTML(InputSource content) throws IOException, SAXException, CancelledException {
    try {
      myHttpMaterial.checkCancelled();
      Document document = HtmlUtils.buildHtmlDocument(content);
      myHttpMaterial.checkCancelled();
      return document;
    } catch (HttpCancelledException e) {
      throw new CancelledException();
    }
  }

  private Document parseXML(InputSource source) throws CancelledException, IOException, JDOMException {
    try {
      myHttpMaterial.checkCancelled();
      SAXBuilder builder = JDOMUtils.createBuilder();
      builder.setEntityResolver(new ZeroEntityResolver());
      Document document = builder.build(source);
      myHttpMaterial.checkCancelled();
      return document;
    } catch (HttpCancelledException e) {
      throw new CancelledException();
    }
  }

  public synchronized void setSuccess(boolean success) {
    if (myDumper != null)
      myDumper.setSuccess(success);
  }

  public synchronized void finish() {
    if (myDumper != null) {
      myDumper.dump();
      myDumper.clear();
    }
    // todo any cleanup?
  }

  public synchronized void setScriptOverride(String script) {
    if (myDumper != null) {
      myDumper.setScriptOverride(script);
    }
  }

  public synchronized void addRedirectUriHandler(RedirectURIHandler handler) {
    List<RedirectURIHandler> handlers = myRedirectUriHandlers;
    if (handlers == null)
      handlers = myRedirectUriHandlers = Collections15.arrayList();
    handlers.add(handler);
  }
}
