package com.almworks.http;

import com.almworks.api.http.*;
import com.almworks.api.http.auth.HttpAuthChallengeData;
import com.almworks.api.http.auth.HttpAuthCredentials;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.AuthState;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

@SuppressWarnings({"OverlyComplexClass"})
public class HttpLoaderImpl implements HttpLoader {
  private static final boolean STRICT_REALM_MATCHING = Env.getBoolean(GlobalProperties.STRICT_REALM_MATCHING);
  private static final boolean NO_PREEMPTIVE_AUTH = Env.getBoolean(GlobalProperties.NO_PREEMPTIVE_AUTH);
  private static final boolean JDK_DEFLECT_DISABLED = Env.getBoolean(GlobalProperties.DISABLE_HTTP_JRE_EXECUTOR);
  private static final boolean JDK_DEFLECT_FORCED = Env.getBoolean(GlobalProperties.FORCE_HTTP_JRE_EXECUTOR);
  private static final boolean FORCE_HTTP10 = Env.getBoolean("force.http10");
  private static final boolean FORCE_HTTP11 = Env.getBoolean("force.http11");
  private static final boolean HTTP_NO_KEEPALIVE = Env.getBoolean("http.no.keepalive");
  private static final String FORCE_USER_AGENT = Env.getString(GlobalProperties.FORCE_HTTP_USER_AGENT);

  private static final List<String> AUTH_SCHEME_PRIORITY_LIST = getAuthSchemePriorityList();

  @Nullable
  private static List<String> getAuthSchemePriorityList() {
    String userPriority = Env.getString(GlobalProperties.HTTP_AUTH_SCHEME_PRIORITY);
    if (userPriority == null || userPriority.trim().length() == 0)
      return null;
    List<String> knownSchemes = Collections15.arrayList(AuthPolicy.getDefaultAuthPrefs());
    List<String> r = Collections15.arrayList();
    String[] tokens = userPriority.split("[,\\s]+");
    for (String token : tokens) {
      String scheme = Util.lower(token);
      if (knownSchemes.remove(scheme)) {
        r.add(scheme);
      } else {
        Log.warn("auth scheme priority skipped [" + token + "]");
      }
    }
    for (String leftover : knownSchemes) {
      r.add(leftover);
    }
    Log.debug("HLI: set auth scheme priority: " + r);
    return r;
  }

  private static final int DEFAULT_MAXIMUM_REDIRECTS = 10;
  private static final int DEFAULT_MAXIMUM_RETRIES = 3;
  private static final String ACCEPT_ENCODING = "Accept-Encoding";

  // kludge : needed to enable preemptive auth for URLs that are known to have basic auth
  private static final Map<AuthScope, String> KNOWN_AUTHTYPES =
    Collections.synchronizedMap(Collections15.<AuthScope, String>hashMap());

  private final HttpMethodFactory myMethodFactory;
  private final List<HttpLoaderImpl> myRedirectTrace;
  private final HttpMaterial myHttpMaterial;
  private final String myEscapedUrl;

  private int myMaximumRedirects = DEFAULT_MAXIMUM_REDIRECTS;
  private int myRetries = DEFAULT_MAXIMUM_RETRIES;
  private HttpMethodFactory myRedirectMethodFactory;
  private List<RedirectURIHandler> myRedirectUriHandlers;
  private Condition<Integer> myFailedStatusCodeApprover;
  private HttpReportAcceptor myReportAcceptor;
  private Function<String, Boolean> myFollowRedirect;
  private boolean myCopyQueryOnRedirect = true; // Default value keeps legacy behaviour

  private boolean myPreemptiveAuth = !NO_PREEMPTIVE_AUTH;
  private boolean myJdkExecutorTried;

  public HttpLoaderImpl(HttpMaterial httpMaterial, HttpMethodFactory methodFactory, String escapedUrl) {
    assert httpMaterial != null;
    assert methodFactory != null;
    assert escapedUrl != null;
    myHttpMaterial = httpMaterial;
    myMethodFactory = methodFactory;
    myEscapedUrl = escapedUrl;
    myRedirectTrace = Collections15.emptyList();
  }

  private HttpLoaderImpl(HttpLoaderImpl redirector, HttpMethodFactory redirectFactory, String escapedRedirectUrl) {
    assert redirector != null;
    assert redirectFactory != null;
    assert escapedRedirectUrl != null;

    myHttpMaterial = redirector.getMaterial();
    myEscapedUrl = escapedRedirectUrl;
    myMethodFactory = redirectFactory;
    myRedirectTrace = Collections15.arrayList(redirector.myRedirectTrace);
    myRedirectTrace.add(redirector);

    myMaximumRedirects = redirector.getMaximumRedirects();
    myRetries = redirector.getRetries();
    myRedirectMethodFactory = redirector.myRedirectMethodFactory;
    myRedirectUriHandlers = redirector.myRedirectUriHandlers;
    myFailedStatusCodeApprover = redirector.myFailedStatusCodeApprover;
    myReportAcceptor = redirector.myReportAcceptor;
  }

  public String toString() {
    return "HLI[" + myEscapedUrl + "]";
  }

  public HttpMaterial getMaterial() {
    return myHttpMaterial;
  }

  public void setRedirectMethodFactory(HttpMethodFactory factory) {
    myRedirectMethodFactory = factory;
  }

  @Override
  public void setFollowRedirect(Function<String, Boolean> followRedirect) {
    myFollowRedirect = followRedirect;
  }

  @Override
  public void setCopyQueryOnRedirect(boolean copyQueryOnRedirect) {
    myCopyQueryOnRedirect = copyQueryOnRedirect;
  }

  public void setFailedStatusApprover(Condition<Integer> failedStatusCodeApprover) {
    myFailedStatusCodeApprover = failedStatusCodeApprover;
  }

  public int getMaximumRedirects() {
    return myMaximumRedirects;
  }

  public void setReportAcceptor(HttpReportAcceptor reportAcceptor) {
    myReportAcceptor = reportAcceptor;
  }

  public HttpMethodFactory getMethodFactory() {
    return myMethodFactory;
  }

  @SuppressWarnings({"ReturnOfCollectionOrArrayField"})
  public List<HttpLoaderImpl> getRedirectTrace() {
    return myRedirectTrace;
  }

  public int getRetries() {
    return myRetries;
  }

  public void setRetries(int retries) {
//    assert myRetries >= 3;
    myRetries = retries;
  }

  public String getEscapedUrl() {
    return myEscapedUrl;
  }

  @SuppressWarnings({"OverlyNestedMethod", "OverlyComplexMethod", "OverlyLongMethod"})
  public HttpResponseData load() throws IOException, HttpLoaderException {
    assert checkNetworkDisabledForTesting();
    checkRedirectLevel();
    IOException lastException = null;
    try {
      int allowedNumberOfCredentialsChange = 4;
      for (int i = 0; i < myRetries; i++) {
        if (i > 0)
          Log.debug("retrying [" + myEscapedUrl + "]");
        myHttpMaterial.checkCancelled();
        try {
          HttpMethodBase method = createMethod();
          HttpClient httpClient = getClient();
          if (i == 0) {
            loadPreliminaryCredentials(httpClient, method, false);
            loadPreliminaryCredentials(httpClient, method, true);
          }

          HttpMethodExecutor executor;
          if (shouldUseJdk(method, httpClient)) {
            myJdkExecutorTried = true;
            executor = new JdkHttpMethodExecutor(method, httpClient);
          } else {
            executor = new ApacheHttpMethodExecutor(method, httpClient);
          }

          Log.debug("HLI: " + executor + " executing");
          int statusCode = executeMethod(executor);

          AuthResult auth;
          try {
            auth = handleAuthRequest(executor, statusCode);
          } catch (AbortLoading abortLoading) {
            auth = AuthResult.NOT_REQUIRED; // Allow caller to handle 401 status code
          }
          if (auth != AuthResult.NOT_REQUIRED) {
            if (auth == AuthResult.AVAILABLE_NEW) {
              // credentials changed, do not count as attempt, unless it happens all the time
              if (allowedNumberOfCredentialsChange > 0) {
                allowedNumberOfCredentialsChange--;
                i--;
              }
            }
            if (i == myRetries - 1) {
              // the last try
              handleFailedStatus(statusCode, executor.getStatusText());
            }
            continue;
          }

          HttpResponseData data = handleRedirect(executor, statusCode);
          if (data != null)
            return data;

          handleFailedStatus(statusCode, executor.getStatusText());

          HttpResponseData reply = createReply(executor);
          if (reply == null)
            continue;

          return reply;
        } catch (IOException e) {
          myHttpMaterial.checkCancelled();
          lastException = e;
          if (lastException instanceof TooManyRedirectsException) {
            // "unwind redirect stack"
            if (!myRedirectTrace.isEmpty()) {
              break;
            }
          }
        }
      }
    } catch (AbortLoading e) {
      Log.debug("retrieval aborted", e);
      lastException = new IOException("aborted load");
    }
    Log.warn("failed to retrieve [" + myEscapedUrl + "] in " + myRetries + " attempts");
    if (lastException == null) {
      lastException = new IOException("failed to load: " + myEscapedUrl);
    }
    throw lastException;
  }

  private static boolean shouldUseJdk(HttpMethodBase method, HttpClient httpClient) {
    if (JDK_DEFLECT_DISABLED) {
      Log.debug("HLI: JHME disabled");
      return false;
    }
    if (JDK_DEFLECT_FORCED) {
      Log.debug("HLI: JHME forced");
      return true;
    }
    @Nullable AuthScope servScope = getServerAuthScope(method);
    @Nullable String servScheme = KNOWN_AUTHTYPES.get(servScope);
    if (servScheme != null) {
      Log.debug("HLI: scope " + servScope + " type " + servScheme);
    }

    @Nullable AuthScope proxyScope = getProxyAuthScope(httpClient);
    @Nullable String proxyScheme = KNOWN_AUTHTYPES.get(proxyScope);
    if (proxyScheme != null) {
      Log.debug("HLI: proxy scope " + proxyScope + " type " + proxyScheme);
    }

    if (!isNTLM(servScheme) && !isNTLM(proxyScheme)) {
      return false;
    }
    if (Env.isWindows()) {
      // will try local user even if credentials set
      return true;
    }

    HttpState httpState = httpClient.getState();
    if (servScheme != null && httpState.getCredentials(servScope) != null) {
      return true;
    }
    if (proxyScheme != null && httpState.getProxyCredentials(proxyScope) != null) {
      return true;
    }
    return false;
  }

  private static boolean isNTLM(String scheme) {
    return "ntlm".equalsIgnoreCase(scheme);
  }

  private static boolean isNTLM(AuthScheme scheme) {
    return isNTLM(scheme == null ? null : scheme.getSchemeName());
  }

  private static AuthScope getProxyAuthScope(HttpClient httpClient) {
    HostConfiguration hc = httpClient != null ? httpClient.getHostConfiguration() : null;
    if (hc == null) {
      return null;
    }
    String host = hc.getProxyHost();
    int port = hc.getProxyPort();
    if (host == null) {
      return null;
    }
    return new AuthScope(host, port);
  }

  private static AuthScope getServerAuthScope(HttpMethodBase method) {
    try {
      URI uri = method.getURI();
      String host = uri.getHost();
      int port = uri.getPort();
      if (host == null) {
        return null;
      }
      return new AuthScope(host, port);
    } catch (URIException e) {
      Log.warn(e);
      return null;
    }
  }

  private static boolean checkNetworkDisabledForTesting() throws IOException {
    if (Env.getBoolean("debug.disable.network"))
      throw new IOException("Network is disabled (emulation)");
    return true;
  }

  private void checkRedirectLevel() throws TooManyRedirectsException {
    int redirectLevel = myRedirectTrace.size();
    if (redirectLevel > myMaximumRedirects)
      throw new TooManyRedirectsException(redirectLevel);
  }

  private HttpMethodBase createMethod() throws HttpMethodFactoryException {
    HttpMethodBase method = myMethodFactory.create();
    method.setFollowRedirects(false);
    HttpMethodParams params = method.getParams();
    if (FORCE_HTTP10) {
      params.setVersion(HttpVersion.HTTP_1_0);
    } else if (FORCE_HTTP11) {
      params.setVersion(HttpVersion.HTTP_1_1);
    }
    if (!FORCE_HTTP10 && !HTTP_NO_KEEPALIVE) {
      setKeepAlive(method);
    }
    setHeaderIfNotSet(method, "Cache-Control", "max-age=0");
    params.setBooleanParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
    setRequestCharset(method);
    setAcceptEncoding(method);
    String userAgent = FORCE_USER_AGENT == null ? myHttpMaterial.getUserAgent() : FORCE_USER_AGENT;
    if (userAgent != null) {
      // todo clear parameter
      params.setParameter(HttpMethodParams.USER_AGENT, userAgent);
    }
    return method;
  }

  private static void setAcceptEncoding(HttpMethod method) {
    if (Env.getBoolean(GlobalProperties.DISABLE_HTTP_COMPRESSION))
      return;
    setHeaderIfNotSet(method, ACCEPT_ENCODING, "gzip, deflate");
  }

  private static void setKeepAlive(HttpMethod method) {
    setHeaderIfNotSet(method, "Connection", "keep-alive");
  }

  private static void setHeaderIfNotSet(HttpMethod method, String name, String value) {
    Header[] headers = method.getRequestHeaders();
    for (Header header : headers) {
      if (name.equalsIgnoreCase(header.getName())) {
        // already have this header - skip
        return;
      }
    }
    method.addRequestHeader(name, value);
  }

  private HttpClient getClient() {
    HttpClient httpClient = myHttpMaterial.getHttpClient();
    assert httpClient != null;
    if (AUTH_SCHEME_PRIORITY_LIST != null) {
      HttpClientParams params = httpClient.getParams();
      params.setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, AUTH_SCHEME_PRIORITY_LIST);
    }

    return httpClient;
  }

  private int executeMethod(HttpMethodExecutor executor) throws IOException, HttpCancelledException {
    try {
      executor.execute();
    } catch (RuntimeException e) {
      // may access any exception from http client if we concurrently cancel the download
      if (myHttpMaterial.isCancelled()) {
        //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
        throw new HttpCancelledException();
      } else if (e instanceof IllegalStateException && myPreemptiveAuth) {
        Log.debug("turning off preemptive auth", e);
        myPreemptiveAuth = false;
      } else {
        Log.debug("runtime exception while executing " + executor, e);
        throw new TransferRuntimeWrapperException(e);
      }
    } finally {
      if (myReportAcceptor != null) {
        try {
          executor.reportTo(myReportAcceptor);
        } catch (Exception e) {
          // ignore
        }
      }
    }
    return executor.getStatusCode();
  }


  /**
   * @return 0 - authentication is not available; 1 - auth available, credentials changed; 2 - auth available,
   *         credentials did not change.
   *         todo remove magic codes
   */
  private AuthResult handleAuthRequest(HttpMethodExecutor executor, int statusCode)
    throws IOException, AbortLoading, HttpCancelledException
  {
    if (statusCode == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
      return handleProxyAuthentication(executor);
    } else if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
      return handleHostAuthentication(executor);
    }
    return AuthResult.NOT_REQUIRED;
  }

  private AuthResult handleProxyAuthentication(HttpMethodExecutor executor)
    throws AbortLoading, IOException, HttpCancelledException
  {
    Log.debug("HLI: proxy auth required");
    AuthResult result = AuthResult.NOT_REQUIRED;
    AuthState authState = executor.getProxyAuthState();
    if (shouldAskForCredentials(authState, executor)) return loadCredentials(executor, true);
    return result;
  }

  private static boolean shouldAskForCredentials(AuthState authState, HttpMethodExecutor executor) {
    // retry only if we have authentication request from server, or if it is a failed NTLM auth with Apache executor
    return authState.isAuthRequested() ||
      (isNTLM(authState.getAuthScheme()) && executor instanceof ApacheHttpMethodExecutor);
  }

  private AuthResult handleHostAuthentication(HttpMethodExecutor executor)
    throws IOException, AbortLoading, HttpCancelledException
  {
    Log.debug("HLI: auth required");
    AuthResult result = AuthResult.NOT_REQUIRED;
    AuthState authState = executor.getHostAuthState();
    if (shouldAskForCredentials(authState, executor)) return loadCredentials(executor, false);
    return result;
  }

  private enum AuthResult {
    NOT_REQUIRED,
    AVAILABLE_NEW,
    AVAILABLE_THESAME
  }

  @Nullable
  @SuppressWarnings({"OverlyLongMethod", "MethodWithMultipleReturnPoints"})
  private HttpResponseData handleRedirect(HttpMethodExecutor executor, int statusCode) throws HttpLoaderException, IOException {

    if (statusCode / 100 != 3) {
      // it's not redirect
      return null;
    }

    Header location = executor.getResponseHeader("Location");
    if (location == null)
      return null;
    String redirectUrl = location.getValue();
    if (redirectUrl == null)
      return null;
    if (myFollowRedirect != null && Boolean.FALSE.equals(myFollowRedirect.invoke(redirectUrl))) return createReply(executor);

    Log.debug("redirected to " + redirectUrl);
    URI currentURI;
    try {
      currentURI = new URI(myEscapedUrl, true);
    } catch (URIException e) {
      Log.warn("unparseable old url " + myEscapedUrl);
      return null;
    }
    URI redirectURI;
    try {
      redirectURI = adjustRedirectURI(new URI(redirectUrl, true), currentURI, myCopyQueryOnRedirect);
    } catch (URIException e) {
      Log.warn("server redirects to bad url: " + redirectUrl);
      return null;
    }

    HttpClient httpClient = getClient();
    httpClient.getHostConfiguration().setHost(redirectURI);
    executor.getHostAuthState().invalidate();
    try {
      migrateCookies(httpClient, executor.getMethod(), redirectURI);
    } catch (URIException e) {
      Log.warn("failed to migrate cookies");
    } catch (RuntimeException e) {
      // patch-release guardian, remove in the next version
      Log.error(e);
    }

    RedirectURIHandler[] redirectHandlers = getRedirectHandlers();
    if (redirectHandlers != null) {
      for (RedirectURIHandler redirectHandler : redirectHandlers) {
        URI converted = redirectHandler.approveRedirect(myHttpMaterial, currentURI, redirectURI);
        if (converted != null && !converted.equals(redirectURI)) {
          Log.debug("redirect url converted from " + redirectURI + " to " + converted + " (" + redirectHandler + ")");
          redirectURI = converted;
        }
      }
    }

    Log.debug("going to " + redirectURI);
    final URI finalRedirectURI = redirectURI;
    HttpMethodFactory redirectFactory = new HttpMethodFactory() {
      public HttpMethodBase create() throws HttpMethodFactoryException {
        HttpMethodBase method;
        if (myRedirectMethodFactory == null)
          method = myMethodFactory.create();
        else
          method = myRedirectMethodFactory.create();
        try {
          method.setURI(finalRedirectURI);
        } catch (URIException e) {
          Log.warn("no redirect", e);
        }
        return method;
      }
    };

    HttpLoaderImpl redirect = new HttpLoaderImpl(this, redirectFactory, redirectURI.getEscapedURI());
    return redirect.load();
  }

  @Nullable
  private synchronized RedirectURIHandler[] getRedirectHandlers() {
    List<RedirectURIHandler> handlers = myRedirectUriHandlers;
    if (handlers == null)
      return null;
    int count = handlers.size();
    if (count == 0)
      return null;
    return handlers.toArray(new RedirectURIHandler[count]);
  }

  public synchronized void addRedirectUriHandler(RedirectURIHandler handler) {
    List<RedirectURIHandler> handlers = myRedirectUriHandlers;
    if (handlers == null)
      myRedirectUriHandlers = handlers = Collections15.arrayList();
    handlers.add(handler);
  }

  private static URI adjustRedirectURI(URI redirectURI, URI currentURI, boolean copyQuery) throws URIException {
    if (redirectURI.isRelativeURI()) {
      // copied from HttpMethodDirector
      redirectURI = new URI(currentURI, redirectURI);
    }
    String query = Util.NN(redirectURI.getEscapedQuery());
    if (query.length() == 0) {
      if (copyQuery && Util.NN(currentURI.getEscapedQuery()).length() > 0) {
        // append query part - if this won't make a loop
        redirectURI.setEscapedQuery(currentURI.getEscapedQuery());
        if (Util.equals(redirectURI.getEscapedURI(), currentURI.getEscapedURI())) {
          redirectURI.setEscapedQuery(null);
        }
      }
    } else {
      String fixedQuery = fixServerOverEscapingQuery(query);
      if (fixedQuery != null) {
        Log.debug("server overescaped query, unescaping: " + fixedQuery);
        redirectURI.setEscapedQuery(fixedQuery);
      }
    }
    return redirectURI;
  }

  @Nullable
  static String fixServerOverEscapingQuery(String query) {
    // the server sometimes returns query string over-encoded, so %XX becomes %25XX.
    // fix for #960: convert %25XX to %XX
    int pos = query.indexOf('%');
    if (pos < 0)
      return null;
    int length = query.length();
    StringBuilder fixed = new StringBuilder(length);
    int last = 0;
    boolean changed = false;
    while (pos >= 0) {
      fixed.append(query.substring(last, pos));
      last = pos;
      if (last + 4 < length && query.charAt(last + 1) == '2' && query.charAt(last + 2) == '5' &&
        isHexadecimal(query.charAt(last + 3)) && isHexadecimal(query.charAt(last + 4)))
      {
        fixed.append('%');
        fixed.append(query.charAt(last + 3));
        fixed.append(query.charAt(last + 4));
        last += 5;
        changed = true;
      }
      pos = query.indexOf('%', last + 1);
    }
    if (!changed)
      return null;
    if (last < length)
      fixed.append(query.substring(last));
    return fixed.toString();
  }

  private static boolean isHexadecimal(char c) {
    return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private void handleFailedStatus(int statusCode, String statusText) throws HttpConnectionException {
    int firstDigit = statusCode / 100;
    if (firstDigit == 2)
      return;
    Condition<Integer> approver = myFailedStatusCodeApprover;
    if (approver != null) {
      if (!approver.isAccepted(statusCode)) {
        // error is not approved - treat as normal response
        return;
      }
    }
    throw new HttpConnectionException(myEscapedUrl, statusCode, statusText);
  }

  @NotNull
  private HttpResponseData createReply(HttpMethodExecutor executor) throws IOException {
    InputStream content = executor.getResponseBodyAsStream();
    HttpResponseDataImpl reply = new HttpResponseDataImpl(executor);
    setResponseCharset(reply, executor);
    setServerTime(executor);
    setContentType(reply, executor);
    setContentFilename(reply, executor);
    reply.setResponseHeaders(executor.getResponseHeaders());
    reply.setLastURI(executor.getURI());

    setContentStream(executor, content, reply);

    return reply;
  }

  private void setContentStream(HttpMethodExecutor executor, @Nullable InputStream content, HttpResponseDataImpl reply) throws IOException {
    long contentLength = executor.getResponseContentLength();
      Header encoding = executor.getResponseHeader("Content-Encoding");
    InputStream decoded = null;
    if (content == null) content = new ByteArrayInputStream(Const.EMPTY_BYTES);
    else if (encoding != null) {
      String value = encoding.getValue();
      try {
        if ("gzip".equalsIgnoreCase(value)) {
          decoded = new GZIPInputStream(content);
        } else if ("deflate".equalsIgnoreCase(value)) {
          decoded = new InflaterInputStream(content);
        }
        contentLength = -1;
      } catch (EOFException e) {
        if (contentLength != 0) LogHelper.warning("EOF reached", e, contentLength);
        decoded = new ByteArrayInputStream(Const.EMPTY_BYTES); // Empty stream
      }
    }
    reply.setContentStream(decoded == null ? content : decoded);
    reply.setContentLength(contentLength);
  }

  private static void setContentFilename(HttpResponseDataImpl reply, HttpMethodExecutor executor) {
    String name = HttpUtils.getHeaderAuxiliaryInfo(executor.getResponseHeader("content-type"), "name");
    if (name == null)
      name = HttpUtils.getHeaderAuxiliaryInfo(executor.getResponseHeader("content-disposition"), "filename");
    reply.setContentFilename(name);
  }

  private static void setContentType(HttpResponseDataImpl reply, HttpMethodExecutor executor) {
    Header type = executor.getResponseHeader("content-type");
    if (type != null) {
      String fullValue = type.getValue();
      if (fullValue != null) {
        fullValue = fullValue.trim();
        String value = fullValue;
        int k = value.indexOf(';');
        if (k >= 0)
          value = value.substring(0, k).trim();
        reply.setContentType(value);
        reply.setFullContentType(fullValue);
      }
    }
  }

  private void setRequestCharset(HttpMethod method) {
    String charset = getCharset();
    HttpMethodParams params = method.getParams();
    if (charset != null) {
      params.setContentCharset(charset);
      params.setCredentialCharset(charset);
    } else {
      params.setContentCharset("UTF-8");
      params.setCredentialCharset("UTF-8");
    }
    params.setHttpElementCharset("UTF-8");
  }

  @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
  private AuthResult loadCredentials(HttpMethodExecutor executor, boolean proxy)
    throws IOException, AbortLoading, HttpCancelledException
  {
    AuthState authState = proxy ? executor.getProxyAuthState() : executor.getHostAuthState();
    String schemeName = "unknown";
    String realm = "";
    if (authState != null) {
      AuthScheme authScheme = authState.getAuthScheme();
      if (authScheme != null) {
        schemeName = authScheme.getSchemeName();
      }
      realm = authState.getRealm();
    }
    Log.debug("HLI: auth required (" + proxy + ") (" + realm + ") (" + schemeName + ")");

    if (isNTLM(schemeName) && !proxy && !JDK_DEFLECT_DISABLED && executor instanceof ApacheHttpMethodExecutor &&
      !myJdkExecutorTried)
    {
      // auto-deflect to JdkExecutor even if there's no handler
      String host = executor.getURI().getHost();
      int port = executor.getURI().getPort();
      KNOWN_AUTHTYPES.put(new AuthScope(host, port), schemeName);
      return AuthResult.AVAILABLE_NEW;
    }

    FeedbackHandler handler = myHttpMaterial.getFeedbackHandler();
    if (handler == null) {
      throw new AbortLoading("authentication required (" + schemeName + ":" + realm + ")(" + proxy + ")");
    }

    HttpClient httpClient = myHttpMaterial.getHttpClient();
    HostConfiguration hc = httpClient != null ? httpClient.getHostConfiguration() : null;
    String host = (proxy && hc != null) ? hc.getProxyHost() : executor.getURI().getHost();
    int port = (proxy && hc != null) ? hc.getProxyPort() : executor.getURI().getPort();
    int statusCode = executor.getStatusCode();
    String statusText = executor.getStatusText();
    HttpAuthChallengeData data =
      new HttpAuthChallengeData(schemeName, realm, host, port, statusCode, statusText, proxy);

    HttpState state = myHttpMaterial.getHttpClient().getState();
    AuthScope authscope = new AuthScope(host, port, realm, schemeName);
    Credentials usedCreds = proxy ? state.getProxyCredentials(authscope) : state.getCredentials(authscope);

    HttpAuthCredentials auth =
      usedCreds == null ? null : new HttpAuthCredentials(getUsername(usedCreds), getPassword(usedCreds));
    try {
      Log.debug("HLI: calling FH requestCredentials(" + data + ", " + auth + ")");
      auth = handler.requestCredentials(data, auth, myHttpMaterial.isQuiet());
    } catch (InterruptedException e) {
      Log.debug("HLI: interrupted from FH", e);
      Thread.currentThread().interrupt();
      //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
      throw new IOException("interrupted while waiting for user's reply");
    }
    if (auth == null) {
      return AuthResult.NOT_REQUIRED;
      // Do not throw special exception when no new credentials available. Allow request to complete with 401 status code
//      throw new AbortLoading((proxy ? "proxy " : "") + "authentication required (" + schemeName + ":" + realm + ")");
    }

    Credentials newCreds = createCredentials(host, schemeName, auth);
    assert newCreds != null;
    boolean credentialsChanged = usedCreds == null || !newCreds.equals(usedCreds);
    AuthScope looseScope = new AuthScope(authscope.getHost(), authscope.getPort());
    if (!STRICT_REALM_MATCHING) {
      Log.debug("HLI: non-strict realm matching");
      // put auth for any realm/scheme, so the client will send them even if realm changes
      authscope = looseScope;
    }
    HttpState currentState = myHttpMaterial.getHttpClient().getState();

    if (myPreemptiveAuth && AuthState.PREEMPTIVE_AUTH_SCHEME.equalsIgnoreCase(schemeName)) {
      currentState.setAuthenticationPreemptive(true);
    }
    KNOWN_AUTHTYPES.put(looseScope, schemeName);

    Log.debug("HLI: setting creds for " + authscope + " (" + proxy + ")(" +
      (credentialsChanged ? "changed" : "same as before") + ")");
    if (proxy)
      currentState.setProxyCredentials(authscope, newCreds);
    else
      currentState.setCredentials(authscope, newCreds);
    return credentialsChanged ? AuthResult.AVAILABLE_NEW : AuthResult.AVAILABLE_THESAME;
  }

  @SuppressWarnings({"OverlyComplexMethod"})
  private void loadPreliminaryCredentials(HttpClient httpClient, HttpMethodBase method, boolean proxy)
    throws IOException, AbortLoading, HttpCancelledException
  {
    if (!myPreemptiveAuth)
      return;
    URI uri = method.getURI();
    FeedbackHandler handler = myHttpMaterial.getFeedbackHandler();
    if (handler == null) {
      return;
    }
    HostConfiguration hc = httpClient != null ? httpClient.getHostConfiguration() : null;
    if (proxy && (hc == null || hc.getProxyHost() == null)) {
      return;
    }
    String host = (proxy && hc != null) ? hc.getProxyHost() : uri.getHost();
    int port = (proxy && hc != null) ? hc.getProxyPort() : uri.getPort();
    if (host == null) {
      return;
    }
    AuthScope authscope = new AuthScope(host, port);
    String scheme = KNOWN_AUTHTYPES.get(authscope);
    if (scheme == null || !scheme.equalsIgnoreCase(AuthState.PREEMPTIVE_AUTH_SCHEME)) {
      Log.debug("[LC] preliminary credentials skipped: authtype is not known");
      return;
    }

    if (httpClient == null) {
      Log.debug("[LC] no httpclient");
      return;
    }

    HttpState httpState = httpClient.getState();
    Credentials currentCredentials =
      proxy ? httpState.getProxyCredentials(authscope) : httpState.getCredentials(authscope);
    if (currentCredentials != null) {
      Log.debug("[LC] preliminary credentials skipped: credentials already set");
      return;
    }

    Pair<HttpAuthCredentials, String> preliminary = handler.requestPreliminaryCredentials(host, port, proxy);
    if (preliminary == null) {
      return;
    }
    HttpAuthCredentials auth = preliminary.getFirst();
    if (auth == null)
      return;

    Log.debug("[LC] preliminary credentials: " + scheme + ":" + auth.getUsername());
    Credentials newCreds = createCredentials(host, scheme, auth);
    HttpState currentState = httpClient.getState();
    if (proxy) {
      currentState.setProxyCredentials(authscope, newCreds);
    } else {
      currentState.setCredentials(authscope, newCreds);
    }
    httpClient.getParams().setAuthenticationPreemptive(true);
  }

  private static Credentials createCredentials(String host, String scheme, HttpAuthCredentials auth) {
    Credentials newCreds;
    if (isNTLM(scheme)) {
      Pair<String, String> ntpair = HttpUtils.getNTDomainUsername(Util.NN(auth.getUsername()));
      newCreds =
        new NTCredentials(ntpair.getSecond(), auth.getPassword(), HttpUtils.getLocalHostname(), ntpair.getFirst());
    } else {
      newCreds = new UsernamePasswordCredentials(auth.getUsername(), auth.getPassword());
    }
    return newCreds;
  }

  private static void migrateCookies(HttpClient httpClient, HttpMethodBase method, URI redirectURI) throws URIException {
    int added = 0, removed = 0;
    HttpState httpState = httpClient.getState();
    Cookie[] cookies = httpState.getCookies();
    if (cookies == null || cookies.length == 0) return;

    CookieSpec spec = HttpUtils.getCookieSpec(httpClient.getState(), method);
    Cookie[] matchedBeforeRedirect = HttpUtils.matchCookies(spec, httpClient, method, cookies);
    if (matchedBeforeRedirect == null || matchedBeforeRedirect.length == 0) return;

    boolean secure = "https".equalsIgnoreCase(redirectURI.getScheme());
    String host = redirectURI.getHost();
    if (host == null) return;
    String path = redirectURI.getPath();
    if (path == null || path.length() == 0) path = "/";
    Cookie[] matchedAfterRedirect = spec.match(host, 80, path, secure, cookies);

    for (Cookie cookie : matchedBeforeRedirect) {
      String name = cookie.getName();
      String value = cookie.getValue();
      if (HttpUtils.hasCookie(matchedAfterRedirect, name, value)) {
        // will be sent anyway
        continue;
      }
      boolean solved = false;
      for (Cookie m : matchedAfterRedirect) {
        if (Util.equals(name, m.getName())) {
          // remove overriding cookie
          assert !Util.equals(value, m.getValue()) : name;
          Collection<Cookie> r = HttpUtils.removeCookies(httpState, Condition.isEqual(m));
          assert !r.isEmpty() : name;
          removed += r.size();
          cookies = httpState.getCookies();
          matchedAfterRedirect = spec.match(host, 80, path, secure, cookies);
          solved = HttpUtils.hasCookie(matchedAfterRedirect, name, value);
          break;
        }
      }
      if (solved) continue;
      
      // create replica cookie
      Date expiryDate = cookie.getExpiryDate();
      String cookiePath = cookie.getPath();
      String usePath;
      // if the cookie would have matched by path and domain only (not by port or security), use path from original cookie
      // otherwise use path from the target URI
      if (spec.domainMatch(host, cookie.getDomain()) && spec.pathMatch(path, cookiePath)) {
        usePath = cookiePath;
      } else {
        usePath = HttpUtils.adjustPathForCookie(path);
      }
      // the new cookie should be secure only if original is secure and the redirect goes through https
      boolean cookieSecure = cookie.getSecure() && secure;
      Cookie replica = new Cookie(host, name, value, usePath, expiryDate, cookieSecure);
      if (!spec.match(host, 80, path, secure, replica)) {
        Log.warn("migrated cookie " + replica + " does not match " + redirectURI);
      }
      httpState.addCookie(replica);
      added++;
    }
    if (added > 0 || removed > 0) {
      Log.debug("REDIRECT added " + added + " cookies, removed " + removed + " cookies");
    }
  }

  private void setResponseCharset(HttpResponseDataImpl reply, HttpMethodExecutor executor) {
    String overrideChartset = Env.getString(GlobalProperties.FORCE_OVERRIDE_CHARSET);
    String charset;
    if (overrideChartset == null) charset = executor.getResponseCharSet();
    else charset = getCharset();
    if (charset == null || charset.length() == 0) charset = getCharset();
    if (charset == null || charset.length() == 0) charset = overrideChartset;
    reply.setCharset(charset);
  }

  private void setServerTime(HttpMethodExecutor executor) {
    Header header = executor.getResponseHeader("Date");
    if (header == null)
      return;
    String value = header.getValue();
    Date date = null;
    if (value != null) {
      try {
        date = DateUtil.parseDate(value);
      } catch (NumberFormatException e) {
        Log.debug("server responded bad time [" + value + "] " + e);
      } catch (DateParseException e) {
        Log.debug("server responded bad time [" + value + "] " + e);
      }
    }
    if (date != null) {
      myHttpMaterial.setLastServerResponseTime(date.getTime());
    }
  }

  @Nullable
  private static String getUsername(Credentials creds) {
    if (!(creds instanceof UsernamePasswordCredentials))
      return null;
    String user = ((UsernamePasswordCredentials) creds).getUserName();
    if (creds instanceof NTCredentials) {
      String domain = ((NTCredentials) creds).getDomain();
      if (Util.NN(domain).length() > 0) {
        user = domain + "\\" + user;
      }
    }
    return user;
  }

  @Nullable
  private static String getPassword(Credentials creds) {
    return (creds instanceof UsernamePasswordCredentials) ? ((UsernamePasswordCredentials) creds).getPassword() : null;
  }

  private String getCharset() {
    String charset = myHttpMaterial.getCharset();
    if (charset != null) {
      try {
        if (!Charset.isSupported(charset)) {
          Log.warn("charset " + charset + " is not supported");
          myHttpMaterial.setCharset(null);
          charset = null;
        }
      } catch (java.nio.charset.IllegalCharsetNameException e) {
        Log.warn("illegal charset name " + charset, e);
        myHttpMaterial.setCharset(null);
        charset = null;
      }
    }
    return charset;
  }

  private static class AbortLoading extends Exception {
    public AbortLoading(String message) {
      super(message);
      //noinspection ThisEscapedInObjectConstruction
      Log.debug("aborting loading", this);
    }
  }
}