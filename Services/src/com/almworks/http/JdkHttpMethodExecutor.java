package com.almworks.http;

import com.almworks.api.http.HttpReportAcceptor;
import com.almworks.api.http.HttpUtils;
import com.almworks.util.Pair;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.StringUtil;
import org.almworks.util.Util;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.*;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ProtocolException;
import java.net.*;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"OverlyComplexClass"})
class JdkHttpMethodExecutor extends HttpMethodExecutor {
  private static final Header[] EMPTY_HEADERS = {};
  private static final String COOKIE_HEADER = "Cookie";
  private static final String AUTH_RESPONSE = "Authorization";

  @Nullable
  private HttpURLConnection myConnection;
  private HostConfiguration myHostConfig;
  private Proxy myProxy;
  private int myResponseCode;
  private String myResponseMessage;
  private InputStream myInputStream;
  private AuthChallengeProcessor myAuthChallengeProcessor;
  private Header[] myRequestHeaders = EMPTY_HEADERS;
  private final List<Header> myAdditionalRequestHeaders = Collections15.arrayList();
  private final List<Pair<AuthScope, Credentials>> myTriedCredentials = Collections15.arrayList();
  private final List<Pair<AuthScope, Credentials>> myTriedProxyCredentials = Collections15.arrayList();
  private int myRequestContentLength = Integer.MIN_VALUE;


  public JdkHttpMethodExecutor(HttpMethodBase method, HttpClient client) {
    super(method, client);
    MyAuthenticator.install();
  }

  @Override
  public String toString() {
    return "JHME[" + HttpUtils.toString(myMethod) + "]";
  }

  public void execute() throws IOException, RuntimeException {
    try {
      MyAuthenticator.process(this);
      createConnection();
      setConnectionParameters();
      addHeaders();
      addCookies();
      setProxyAuth();
      setHostAuth();
      addPostHeaders();

      // must come after all preparations
      saveRequestHeaders();

      // connection may be opened by write body
      writeBody();
      readResponse();

      // update state
      updateCookies();
      updateAuthState(myMethod.getProxyAuthState(), HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED, "Proxy-Authenticate");
      updateAuthState(myMethod.getHostAuthState(), HttpStatus.SC_UNAUTHORIZED, "WWW-Authenticate");

      // done
    } finally {
      MyAuthenticator.unprocess(this);
    }
  }

  protected int getMethodStatusCode() {
    return myResponseCode;
  }

  public String getStatusText() {
    return myResponseMessage;
  }

  @Nullable
  public Header getResponseHeader(String name) {
    HttpURLConnection con = myConnection;
    if (name == null || con == null)
      return null;
    Map<String, List<String>> map = con.getHeaderFields();
    for (Map.Entry<String, List<String>> e : map.entrySet()) {
      String key = e.getKey();
      if (name.equalsIgnoreCase(key)) {
        List<String> list = e.getValue();
        if (list.isEmpty())
          return null;
        else
          return new Header(key, list.get(0));
      }
    }
    return null;
  }

  public AuthState getProxyAuthState() {
    return myMethod.getProxyAuthState();
  }

  public AuthState getHostAuthState() {
    return myMethod.getHostAuthState();
  }

  public InputStream getResponseBodyAsStream() throws IOException {
    InputStream stream = myInputStream;
    if (stream == null)
      throw new IOException(this + ": no input");
    return stream;
  }

  public Header[] getResponseHeaders() {
    HttpURLConnection con = myConnection;
    if (con == null)
      return EMPTY_HEADERS;
    return convertHeaders(con.getHeaderFields());
  }

  private static Header[] convertHeaders(Map<String, List<String>> map) {
    List<Header> r = Collections15.arrayList();
    for (Map.Entry<String, List<String>> e : map.entrySet()) {
      String name = e.getKey();
      if (name == null)
        continue;
      for (String value : e.getValue()) {
        r.add(new Header(name, value));
      }
    }
    return r.toArray(new Header[r.size()]);
  }

  public long getResponseContentLength() {
    HttpURLConnection con = myConnection;
    if (con == null)
      return -1;
    return con.getContentLength();
  }

  /**
   * Copied from {@link HttpMethodBase#getContentCharSet}
   */
  public String getResponseCharSet() {
    Header contentheader = getResponseHeader("content-type");
    String charset = null;
    if (contentheader != null) {
      HeaderElement[] values = contentheader.getElements();
      // I expect only one header element to be there
      // No more. no less
      if (values.length == 1) {
        NameValuePair param = values[0].getParameterByName("charset");
        if (param != null) {
          // If I get anything "funny"
          // UnsupportedEncondingException will result
          charset = param.getValue();
        }
      }
    }
    if (charset == null) {
      charset = myMethod.getParams().getContentCharset();
    }
    return charset;
  }

  public void reportTo(HttpReportAcceptor acceptor) throws Exception {
    HttpURLConnection con = myConnection;
    if (con == null)
      return;
    assert acceptor != null;
    if (myResponseCode != 0) {
      List<String> list = myConnection.getHeaderFields().get(null);
      String line;
      if (list != null && list.size() == 1)
        line = list.get(0);
      else
        line = "HTTP/1.1 " + myResponseCode + " " + myResponseMessage;
      StatusLine statusLine;
      try {
        statusLine = new StatusLine(line);
      } catch (HttpException e) {
        Log.warn(e);
        throw e;
      }
      String name = myMethod.getName();
      URI uri = myMethod.getURI();
      Header[] headers = myRequestHeaders;
      if (!myAdditionalRequestHeaders.isEmpty()) {
        List<Header> nh = Collections15.arrayList(headers);
        nh.addAll(myAdditionalRequestHeaders);
        headers = nh.toArray(new Header[nh.size()]);
      }
      Header[] responseHeaders = convertHeaders(con.getHeaderFields());
      acceptor.report(name, uri, new HttpVersion(1, 1), headers, statusLine, responseHeaders);
    }
  }

  public void dispose() {
    InputStream in = myInputStream;
    if (in != null) {
      IOUtils.closeStreamIgnoreExceptions(in);
    }
    myInputStream = null;
    myConnection = null;
  }

  private void addPostHeaders() throws IOException {
    if (!(myMethod instanceof EntityEnclosingMethod))
      return;
    HttpURLConnection con = myConnection;
    if (con == null)
      return;

    if ((con.getRequestProperty("content-length") == null) && (con.getRequestProperty("Transfer-Encoding") == null)) {
      int len = getRequestContentLength();
      if (len < 0) {
        con.addRequestProperty("Transfer-Encoding", "chunked");
      } else {
        con.addRequestProperty("Content-Length", String.valueOf(len));
      }
    }

    if (con.getRequestProperty("Content-Type") == null) {
      RequestEntity requestEntity = getRequestEntity();
      if (requestEntity != null && requestEntity.getContentType() != null) {
        con.addRequestProperty("Content-Type", requestEntity.getContentType());
      }
    }
  }

  private void addHeaders() throws URIException {
    HttpURLConnection con = myConnection;
    if (con == null)
      return;
    HostConfiguration config = getHostConfig();
    addHostHeader(con, config);
    addProxyConnectionHeader(con, config);
    addHeadersFromMethod(con);
    addUserAgentHeader();
  }

  private void addHeadersFromMethod(HttpURLConnection con) {
    Header[] headers = myMethod.getRequestHeaders();
    for (Header header : headers) {
      if (header.isAutogenerated())
        continue;
      String name = header.getName();
      if (name == null)
        continue;
      if (COOKIE_HEADER.equalsIgnoreCase(name))
        continue;
      con.addRequestProperty(name, Util.NN(header.getValue()));
    }
  }

  private void addHostHeader(HttpURLConnection con, HostConfiguration config) {
    String host = myMethod.getParams().getVirtualHost();
    if (host == null) {
      host = config.getHost();
    }
    int port = config.getPort();
    //appends the port only if not using the default port for the protocol
    if (port > 0 && config.getProtocol().getDefaultPort() != port) {
      host += (":" + port);
    }
    con.addRequestProperty("Host", host);
  }

  private static void addProxyConnectionHeader(HttpURLConnection con, HostConfiguration config) {
    // add proxy control; assume non-transparent
    if (config.getProxyHost() != null)
      con.addRequestProperty("Proxy-Connection", "Keep-Alive");
  }

  private void updateAuthState(AuthState authState, int expectedCode, String challengeHeaders) {
    assert myResponseCode != 0;
    authState.setAuthRequested(myResponseCode == expectedCode);
    if (!authState.isAuthRequested())
      return;
    //noinspection OverlyBroadCatchBlock,CatchGenericClass
    try {
      Map proxyChallenges = AuthChallengeParser.parseChallenges(getResponseHeaders(challengeHeaders));
      if (proxyChallenges.isEmpty())
        return;
      AuthScheme authscheme = getAuthProcessor().processChallenge(authState, proxyChallenges);
      Log.debug(this + ": " + expectedCode + " auth " + authscheme);
    } catch (Exception e) {
      Log.debug(e);
    }
  }

  private AuthChallengeProcessor getAuthProcessor() {
    if (myAuthChallengeProcessor == null)
      myAuthChallengeProcessor = new AuthChallengeProcessor(myClient.getParams());
    return myAuthChallengeProcessor;
  }


  private Header[] getResponseHeaders(String name) {
    assert myConnection != null;
    Map<String, List<String>> headers = myConnection.getHeaderFields();
    for (Map.Entry<String, List<String>> e : headers.entrySet()) {
      String key = e.getKey();
      if (name.equalsIgnoreCase(key)) {
        List<String> values = e.getValue();
        Header[] result = new Header[values.size()];
        for (int i = 0; i < values.size(); i++) {
          result[i] = new Header(key, values.get(i));
        }
        return result;
      }
    }
    return EMPTY_HEADERS;
  }

  private void readResponse() throws IOException {
    HttpURLConnection connection = myConnection;
    if (connection == null)
      return;
    try {
      myInputStream = connection.getInputStream();
    } catch (IOException e) {
      myInputStream = connection.getErrorStream();
      if (myInputStream == null)
        throw e;
    }
    myResponseCode = connection.getResponseCode();
    myResponseMessage = connection.getResponseMessage();
  }

  private void saveRequestHeaders() {
    HttpURLConnection connection = myConnection;
    if (connection == null)
      return;
    myRequestHeaders = convertHeaders(connection.getRequestProperties());
  }

  private void updateCookies() throws URIException {
    HttpURLConnection con = myConnection;
    if (con == null)
      return;
    HostConfiguration hostconfig = getHostConfig();
    Header[] setcookies = getResponseHeaders("set-cookie2");
    if (setcookies.length == 0)
      setcookies = getResponseHeaders("set-cookie");
    if (setcookies.length == 0)
      return;
    HttpState state = myClient.getState();
    CookieSpec parser = HttpUtils.getCookieSpec(state, myMethod);

    String host = HttpUtils.getCookieHost(hostconfig, myMethod);
    int port = hostconfig.getPort();
    String path = myMethod.getPath();
    boolean secure = hostconfig.getProtocol().isSecure();

    for (Header header : setcookies) {
      Cookie[] cookies = null;
      try {
        cookies = parser.parse(host, port, path, secure, header);
      } catch (MalformedCookieException e) {
        Log.debug(e);
      }
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          try {
            parser.validate(host, port, path, secure, cookie);
            state.addCookie(cookie);
            Log.debug(this + ": cookie accepted: \"" + parser.formatCookie(cookie) + "\"");
          } catch (MalformedCookieException e) {
            Log.debug(this + ": cookie rejected: \"" + parser.formatCookie(cookie) + "\". " + e.getMessage());
          }
        }
      }
    }
  }

  private void writeBody() throws IOException {
    HttpURLConnection connection = myConnection;
    if (connection == null)
      return;

    // hack: replicating hierarchy
    assert !(myMethod instanceof MultipartPostMethod) : myMethod + " deprecated";
    boolean post = myMethod instanceof EntityEnclosingMethod;
    connection.setDoOutput(post);
    if (post) {
      writePost();
    }
  }

  /**
   * Copied from {@link org.apache.commons.httpclient.methods.EntityEnclosingMethod#writeRequestBody}
   */
  private void writePost() throws IOException {
    HttpURLConnection con = myConnection;
    if (con == null)
      return;
    // we need to hack into protected stuff :(
    try {
      Boolean b = hackCall(myMethod, "hasRequestContent", Boolean.class);
      if (b != null && !b)
        return;
      RequestEntity request = getRequestEntity();
      if (request == null)
        return;
      Field requestEntity = EntityEnclosingMethod.class.getDeclaredField("requestEntity");
      requestEntity.setAccessible(true);
      requestEntity.set(myMethod, request);
      int contentLength = getRequestContentLength();
/*
      // do not do this: this will make connection "streaming", which will make it impossible to retry authentication
      // or may be it would be right thing?
      
      if (contentLength < 0) {
        con.setChunkedStreamingMode(8192);
      } else {
        con.setFixedLengthStreamingMode(contentLength);
      }
*/

      OutputStream out = con.getOutputStream();
      try {
        OutputStream writeTo = contentLength >= 0 ? out : new ChunkedOutputStream(out);
        request.writeRequest(writeTo);
        if (writeTo instanceof ChunkedOutputStream) {
          ((ChunkedOutputStream) writeTo).finish();
        }
        writeTo.flush();
      } finally {
        IOUtils.closeStreamIgnoreExceptions(out);
      }
    } catch (NoSuchMethodException e) {
      Log.warn(e);
      throw new IOException(e.getMessage());
    } catch (InvocationTargetException e) {
      Log.warn(e);
      throw new IOException(e.getMessage());
    } catch (IllegalAccessException e) {
      Log.warn(e);
      throw new IOException(e.getMessage());
    } catch (NoSuchFieldException e) {
      Log.warn(e);
      throw new IOException(e.getMessage());
    } catch (ClassCastException e) {
      Log.warn(e);
      throw new IOException(e.getMessage());
    }
  }

  private RequestEntity getRequestEntity() throws IOException {
    //noinspection CatchGenericClass
    try {
      return hackCall(myMethod, "generateRequestEntity", RequestEntity.class);
    } catch (Exception e) {
      Log.warn(e);
      throw new IOException(this + ": " + e);
    }
  }

  private int getRequestContentLength() throws IOException {
    try {
      if (myRequestContentLength == Integer.MIN_VALUE) {
        int contentLength = -1;
        Long lo = hackCall(myMethod, "getRequestContentLength", Long.class);
        if (lo != null && lo <= Integer.MAX_VALUE) {
          contentLength = (int) (long) lo;
        }
        myRequestContentLength = contentLength;
      }
      return myRequestContentLength;
    } catch (NoSuchMethodException e) {
      Log.warn(e);
      throw new IOException(this + ": " + e);
    } catch (InvocationTargetException e) {
      Log.warn(e);
      throw new IOException(this + ": " + e);
    } catch (IllegalAccessException e) {
      Log.warn(e);
      throw new IOException(this + ": " + e);
    } catch (ClassCastException e) {
      Log.warn(e);
      throw new IOException(this + ": " + e);
    }
  }

  @Nullable
  private static <T> T hackCall(HttpMethodBase object, String method, Class<T> resultClass)
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassCastException
  {
    Method m = EntityEnclosingMethod.class.getDeclaredMethod(method);
    m.setAccessible(true);
    Object r = m.invoke(object);
    if (r == null)
      return null;
    if (!resultClass.isInstance(r))
      throw new ClassCastException(resultClass + " : " + r.getClass() + " : " + r);
    return (T) r;
  }

  private HostConfiguration getHostConfig() throws URIException {
    if (myHostConfig == null) {
      myHostConfig = HttpUtils.createHostConfiguration(myClient, myMethod);
    }
    return myHostConfig;
  }

  /**
   * Copied from {@link HttpMethodBase#addCookieRequestHeader}
   */
  private void addCookies() throws URIException {
    HttpURLConnection con = myConnection;
    if (con == null) {
      assert false;
      return;
    }
    Header[] cookieHeaders = myMethod.getRequestHeaders(COOKIE_HEADER);
    for (Header cookieHeader : cookieHeaders) {
      if (cookieHeader.isAutogenerated()) {
        myMethod.removeRequestHeader(cookieHeader);
      }
    }

    HttpState state = myClient.getState();
    CookieSpec matcher = HttpUtils.getCookieSpec(state, myMethod);
    HostConfiguration hostconfig = getHostConfig();
    String host = HttpUtils.getCookieHost(hostconfig, myMethod);
    Cookie[] cookies = matcher.match(host, hostconfig.getPort(), myMethod.getPath(),
      hostconfig.getProtocol().isSecure(), state.getCookies());
    if ((cookies != null) && (cookies.length > 0)) {
      if (myMethod.getParams().isParameterTrue(HttpMethodParams.SINGLE_COOKIE_HEADER)) {
        // In strict mode put all cookies on the same header
        String s = matcher.formatCookies(cookies);
        con.addRequestProperty(COOKIE_HEADER, s);
      } else {
        // In non-strict mode put each cookie on a separate header
        for (Cookie cookie : cookies) {
          String s = matcher.formatCookie(cookie);
          con.addRequestProperty(COOKIE_HEADER, s);
        }
      }
    }
  }

  private void addUserAgentHeader() {
    HttpURLConnection con = myConnection;
    if (con == null)
      return;
    Object agent = myMethod.getParams().getParameter(HttpMethodParams.USER_AGENT);
    if (agent instanceof String) {
      String s = (String) agent;
      String hackReplace = HttpUtils.getEngineVersion();
      if (s.startsWith(hackReplace)) {
        //noinspection HardcodedFileSeparator
        s = "Java/" + System.getProperty("java.version") + " JHME" + s.substring(hackReplace.length());
      }
      con.setRequestProperty("User-Agent", s);
    }
  }

  private void setConnectionParameters() throws ProtocolException {
    HttpURLConnection connection = myConnection;
    if (connection == null)
      return;
    connection.setRequestMethod(myMethod.getName());
    connection.setInstanceFollowRedirects(false);
    connection.setUseCaches(false);
    connection.setDoInput(true);
    connection.setAllowUserInteraction(false);
    long timeout = getConnectionTimeout();
    if (timeout > 0)
      connection.setConnectTimeout((int) timeout);
    Object t = myMethod.getParams().getParameter(HttpMethodParams.SO_TIMEOUT);
    if (t == null)
      t = myClient.getHttpConnectionManager().getParams().getSoTimeout();
    if ((t instanceof Number) && ((Number) t).intValue() > 0)
      connection.setReadTimeout(((Number) t).intValue());
  }

  private long getConnectionTimeout() {
    return myClient.getParams().getConnectionManagerTimeout();
  }


  private void setProxyAuth() throws AuthenticationException {
    HttpURLConnection con = myConnection;
    if (con == null)return;
/*
// this was copied from Http Client, but doesn't seem necessary, and may harm
    if (con instanceof HttpsURLConnection) {
      // will use CONNECT ?
      return;
    }
*/
    HostConfiguration hostConfig = myClient.getHostConfiguration();
    String proxyHost = hostConfig.getProxyHost();
    int proxyPort = hostConfig.getProxyPort();
    if (proxyHost != null && proxyPort > 0) {
      AuthState state = myMethod.getProxyAuthState();
      AuthScheme scheme = state.getAuthScheme();
      if (scheme != null) {
        if (state.isAuthRequested() || !scheme.isConnectionBased()) {
          AuthScope scope = new AuthScope(proxyHost, proxyPort, scheme.getRealm(), scheme.getSchemeName());
          Credentials credentials = myClient.getState().getProxyCredentials(scope);
          if (credentials != null) {
            String authstring = scheme.authenticate(credentials, myMethod);
            if (authstring != null) {
              con.addRequestProperty("Proxy-Authorization", authstring);
            }
          }
        }
      }
    }
  }

  private void setHostAuth() throws URIException, AuthenticationException {
    HttpURLConnection con = myConnection;
    if (con == null)
      return;
    if (con.getRequestProperty(AUTH_RESPONSE) != null)
      return;

    AuthState state = myMethod.getHostAuthState();
    AuthScheme scheme = state.getAuthScheme();
    if (scheme == null) {
      return;
    }
    HostConfiguration config = getHostConfig();
    if (state.isAuthRequested() || !scheme.isConnectionBased()) {
      if ("ntlm".equalsIgnoreCase(scheme.getSchemeName())) {
        // do not perform host authentication: we want to use JDK's uber-cool authentication
        // with NTLM support; that's the reason for deviation from HttpLoaderImpl
        return;
      }
      String host = myMethod.getParams().getVirtualHost();
      if (host == null) {
        host = config.getHost();
      }
      int port = config.getPort();
      AuthScope authscope = new AuthScope(host, port, scheme.getRealm(), scheme.getSchemeName());
      Credentials credentials = myClient.getState().getCredentials(authscope);
      if (credentials != null) {
        String authstring = scheme.authenticate(credentials, myMethod);
        if (authstring != null) {
          con.addRequestProperty(AUTH_RESPONSE, authstring);
        }
      }
    }
  }


  private void createConnection() throws IOException {
    String urlString = myMethod.getURI().getEscapedURI();
    URL url = new URL(urlString);
    URLConnection connection;
    Proxy proxy = getProxy();
    //noinspection CatchGenericClass
    try {
      connection = url.openConnection(proxy);
    } catch (Exception e) {
      Log.warn(e);
      throw new IOException(urlString + ": " + e);
    }
    if (!(connection instanceof HttpURLConnection))
      throw new IOException("connection of wrong type [" + connection + "]");

    connection = JdkHttpsNtlmProxyHack.adjust(connection, proxy);

    myConnection = (HttpURLConnection) connection;
  }

  private Proxy getProxy() {
    if (myProxy == null) {
      Proxy proxy = Proxy.NO_PROXY;
      HostConfiguration hostConfig = myClient.getHostConfiguration();
      String proxyHost = hostConfig.getProxyHost();
      int proxyPort = hostConfig.getProxyPort();
      if (proxyHost != null && proxyPort > 0) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
      }
      myProxy = proxy;
    }
    return myProxy;
  }


  private static class MyAuthenticator extends Authenticator {
    @SuppressWarnings({"StaticNonFinalField"})
    private static boolean installed;
    private static final ThreadLocal<JdkHttpMethodExecutor> PROCESSED = new ThreadLocal<JdkHttpMethodExecutor>();

    @Nullable
    protected PasswordAuthentication getPasswordAuthentication() {
      JdkHttpMethodExecutor executor = PROCESSED.get();
      if (executor == null) {
        Log.warn("call to JHME.MA out of context", new RuntimeException());
        return null;
      }
      int port = getRequestingPort();
      if (port <= 0)
        port = AuthScope.ANY_PORT;
      AuthScope scope = new AuthScope(getRequestingHost(), port, getRequestingPrompt(), getRequestingScheme());
      boolean proxy = getRequestorType() == RequestorType.PROXY;
      Log.debug(executor + ": MA: requesting creds for " + scope + "; proxy=" + proxy);
      HttpState state = executor.myClient.getState();
      Credentials creds = proxy ? state.getProxyCredentials(scope) : state.getCredentials(scope);
      if (!(creds instanceof UsernamePasswordCredentials))
        return null;
      UsernamePasswordCredentials up = (UsernamePasswordCredentials) creds;

      // verify we didn't try this credentials already
      List<Pair<AuthScope, Credentials>> credList =
        proxy ? executor.myTriedProxyCredentials : executor.myTriedCredentials;
      Pair<AuthScope, Credentials> pair = Pair.create(scope, creds);
      if (credList.contains(pair)) {
        Log.debug(executor + ": MA: already tried creds " + up.getUserName() + ":***, aborting");
        return null;
      }
      credList.add(pair);

      String username = Util.NN(up.getUserName());
      String password = Util.NN(up.getPassword());
      if (up instanceof NTCredentials) {
        String domain = ((NTCredentials) up).getDomain();
        if (domain != null && domain.length() > 0 && username.indexOf('\\') < 0) {
          username = domain + "\\" + username;
        }
      }
      executor.myAdditionalRequestHeaders
        .add(new Header(proxy ? "Proxy-Authorization*" : "Authorization*",
          username + ":" + StringUtil.repeatCharacter('*', password.length())));
      Log.debug(executor + ": MA: trying creds " + up.getUserName() + ":***");
      return new PasswordAuthentication(username, password.toCharArray());
    }

    public static synchronized void install() {
      if (installed)
        return;
      Authenticator.setDefault(new MyAuthenticator());
      checkJavaVersion();
      installed = true;
    }

    /**
     * There are important bugs in NTLM auth prior to 1.6.0_04.
     */
    private static void checkJavaVersion() {
      String javaVersion = System.getProperty("java.version");
      if (javaVersion == null)
        return;
      String[] tokens = javaVersion.split("[^\\d]+");
      if (tokens.length < 3)
        return;
      int[] version = new int[tokens.length];
      for (int i = 0; i < tokens.length; i++) {
        int v = Util.toInt(tokens[i], -1);
        if (v < 0)
          return;
        version[i] = v;
      }
      int v = version[0] * 1000000 + version[1] * 10000 + version[2] * 100;
      if (version.length > 3)
        v += version[3];
      if (v < 1060004) {
        // earlier than 1.6.0_04
        Log.warn("IMPORTANT: NTLM authentication in Java " + javaVersion + " has serious problems");
      }
    }

    public static void process(JdkHttpMethodExecutor executor) {
      JdkHttpMethodExecutor e = PROCESSED.get();
      assert e == null : e;
      PROCESSED.set(executor);
    }

    public static void unprocess(JdkHttpMethodExecutor executor) {
      JdkHttpMethodExecutor e = PROCESSED.get();
      PROCESSED.remove();
      assert e == null || e == executor : e + " " + executor;
    }
  }
}
