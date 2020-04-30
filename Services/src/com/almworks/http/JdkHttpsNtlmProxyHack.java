package com.almworks.http;

import com.almworks.util.Env;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import sun.net.www.http.HttpClient;
import sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection;
import sun.net.www.protocol.https.Handler;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class provides work-around for http://bugs.sun.com/view_bug.do?bug_id=6206466,
 * which is claimed to be fixed but it is not.
 * <p/>
 * The problem is that when using JDK connection classes with
 * a) HTTPS connection
 * b) HTTP proxy
 * c) proxy being authorized with NTLM (multi-step authorization)
 * the code does not send "Proxy-Connection: keep-alive", which makes it fail during authorizing CONNECT method.
 * <p/>
 * <p/>
 * The hack is fragile, and backs out when something is wrong. But it's better than nothing.
 * <p/>
 * <p/>
 * Use {@link #adjust} method right after you have opened a connection
 */
public class JdkHttpsNtlmProxyHack {
  public static final boolean DISABLED = Env.getBoolean("disable.https.proxy.hack");

  // set to true if something is
  public static boolean fault = false;

  /**
   * Hacks the connection. Returns either the adjusted connection, or the original if hack is not applicable.
   *
   * @param connection the original connection
   * @param proxy      proxy parameter
   */
  @NotNull
  public static URLConnection adjust(@NotNull URLConnection connection, Proxy proxy) {
    if (DISABLED || proxy == null || proxy == Proxy.NO_PROXY || connection == null) {
      return connection;
    }
    if (fault) {
      // somehow this hack is detected to be not working
      return connection;
    }

    // check if we need to patch the class without risking getting NoClassDefFoundError
    String className = connection.getClass().getName();
    if ("sun.net.www.protocol.https.HttpsURLConnectionImpl".equals(className)) {
      URLConnection c = patchProxiedHttpsConnection((sun.net.www.protocol.https.HttpsURLConnectionImpl) connection);
      if (c != null) {
        Log.debug("JHNPH: using patched connection " + connection);
        return c;
      }
    } else {
      if (className.toUpperCase(Locale.US).indexOf("HTTPS") >= 0) {
        Log.warn("JHNPH: unknown HTTPS impl " + className);
      }
    }
    return connection;
  }

  private static HttpsURLConnectionImpl patchProxiedHttpsConnection(
    sun.net.www.protocol.https.HttpsURLConnectionImpl connection)
  {
    try {
      Field delegateField = sun.net.www.protocol.https.HttpsURLConnectionImpl.class.getDeclaredField("delegate");
      delegateField.setAccessible(true);

      Object delegate = delegateField.get(connection);
      String delegateClassName = delegate.getClass().getName();
      if (!"sun.net.www.protocol.https.DelegateHttpsURLConnection".equals(delegateClassName)) {
        fault("unknown delegate " + delegateClassName, null);
        return null;
      }

      try {
        sun.net.www.protocol.https.DelegateHttpsURLConnection d =
          (sun.net.www.protocol.https.DelegateHttpsURLConnection) delegate;
        URL url = d.getURL();
        Proxy p = (Proxy) getField(d, "instProxy");
        sun.net.www.protocol.https.Handler handler = (sun.net.www.protocol.https.Handler) getField(d, "handler");
        HttpsURLConnectionImpl r = new HttpsURLConnectionImpl(url, p, handler);
        return r;
      } catch (IOException e) {
        Log.warn("JHNPH: weird IO", e);
        return null;
      } catch (ClassCastException e) {
        fault("", e);
        return null;
      }
    } catch (NoSuchFieldException e) {
      fault("", e);
      return null;
    } catch (IllegalAccessException e) {
      fault("", e);
      return null;
    }
  }

  private static void fault(String message, Throwable e) {
    Log.warn("JHNPH: fault " + message, e);
    fault = true;
  }

  public static Object getField(sun.net.www.protocol.http.HttpURLConnection object, String fieldName)
    throws NoSuchFieldException, IllegalAccessException
  {
    Field field = sun.net.www.protocol.http.HttpURLConnection.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(object);
  }

  // copied verbatim from sun.net.www.protocol.https


  public static class HttpsURLConnectionImpl extends javax.net.ssl.HttpsURLConnection {
// public class HttpsURLConnectionOldImpl
//      extends com.sun.net.ssl.HttpsURLConnection {

    // NOTE: made protected for plugin so that subclass can set it.
    protected DelegateHttpsURLConnection delegate;

// For both copies of the file, uncomment one line and comment the other

    HttpsURLConnectionImpl(URL u, Handler handler) throws IOException {
//    HttpsURLConnectionOldImpl(URL u, Handler handler) throws IOException {
      this(u, null, handler);
    }

// For both copies of the file, uncomment one line and comment the other

    HttpsURLConnectionImpl(URL u, Proxy p, Handler handler) throws IOException {
//    HttpsURLConnectionOldImpl(URL u, Proxy p, Handler handler) throws IOException {
      super(u);
      delegate = new DelegateHttpsURLConnection(url, p, handler, this);
    }

    // NOTE: introduced for plugin
    // subclass needs to overwrite this to set delegate to
    // the appropriate delegatee

    protected HttpsURLConnectionImpl(URL u) throws IOException {
      super(u);
    }

    /**
     * Create a new HttpClient object, bypassing the cache of
     * HTTP client objects/connections.
     *
     * @param url the URL being accessed
     */
    protected void setNewClient(URL url) throws IOException {
      delegate.setNewClient(url, false);
    }

    /**
     * Obtain a HttpClient object. Use the cached copy if specified.
     *
     * @param url      the URL being accessed
     * @param useCache whether the cached connection should be used
     *                 if present
     */
    protected void setNewClient(URL url, boolean useCache) throws IOException {
      delegate.setNewClient(url, useCache);
    }

    /**
     * Create a new HttpClient object, set up so that it uses
     * per-instance proxying to the given HTTP proxy.  This
     * bypasses the cache of HTTP client objects/connections.
     *
     * @param url       the URL being accessed
     * @param proxyHost the proxy host to use
     * @param proxyPort the proxy port to use
     */
    protected void setProxiedClient(URL url, String proxyHost, int proxyPort) throws IOException {
      delegate.setProxiedClient(url, proxyHost, proxyPort);
    }

    /**
     * Obtain a HttpClient object, set up so that it uses per-instance
     * proxying to the given HTTP proxy. Use the cached copy of HTTP
     * client objects/connections if specified.
     *
     * @param url       the URL being accessed
     * @param proxyHost the proxy host to use
     * @param proxyPort the proxy port to use
     * @param useCache  whether the cached connection should be used
     *                  if present
     */
    protected void setProxiedClient(URL url, String proxyHost, int proxyPort, boolean useCache) throws IOException {
      delegate.setProxiedClient(url, proxyHost, proxyPort, useCache);
    }

    /**
     * Implements the HTTP protocol handler's "connect" method,
     * establishing an SSL connection to the server as necessary.
     */
    public void connect() throws IOException {
      delegate.connect();
    }

    /**
     * Used by subclass to access "connected" variable.  Since we are
     * delegating the actual implementation to "delegate", we need to
     * delegate the access of "connected" as well.
     */
    protected boolean isConnected() {
      return delegate.isConnected();
    }

    /**
     * Used by subclass to access "connected" variable.  Since we are
     * delegating the actual implementation to "delegate", we need to
     * delegate the access of "connected" as well.
     */
    protected void setConnected(boolean conn) {
      delegate.setConnected(conn);
    }

    /**
     * Returns the cipher suite in use on this connection.
     */
    public String getCipherSuite() {
      return delegate.getCipherSuite();
    }

    /**
     * Returns the certificate chain the client sent to the
     * server, or null if the client did not authenticate.
     */
    public java.security.cert.Certificate[] getLocalCertificates() {
      return delegate.getLocalCertificates();
    }

    /**
     * Returns the server's certificate chain, or throws
     * SSLPeerUnverified Exception if
     * the server did not authenticate.
     */
    public java.security.cert.Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
      return delegate.getServerCertificates();
    }

    /**
     * Returns the server's X.509 certificate chain, or null if
     * the server did not authenticate.
     * <p/>
     * NOTE: This method is not necessary for the version of this class
     * implementing javax.net.ssl.HttpsURLConnection, but provided for
     * compatibility with the com.sun.net.ssl.HttpsURLConnection version.
     */
    public javax.security.cert.X509Certificate[] getServerCertificateChain() {
      try {
        return delegate.getServerCertificateChain();
      } catch (SSLPeerUnverifiedException e) {
        // this method does not throw an exception as declared in
        // com.sun.net.ssl.HttpsURLConnection.
        // Return null for compatibility.
        return null;
      }
    }

    /**
     * Returns the principal with which the server authenticated itself,
     * or throw a SSLPeerUnverifiedException if the server did not authenticate.
     */
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
      return (Principal) callMethod("getPeerPrincipal");
    }

    /**
     * Returns the principal the client sent to the
     * server, or null if the client did not authenticate.
     */
    public Principal getLocalPrincipal() {
      return (Principal) callMethod("getLocalPrincipal");
    }

    private Object callMethod(String name) {
      try {
        Method method = AbstractDelegateHttpsURLConnection.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(delegate);
      } catch (NoSuchMethodException e) {
        fault("calling " + name, e);
      } catch (IllegalAccessException e) {
        fault("calling " + name, e);
      } catch (InvocationTargetException e) {
        Throwable t = e.getTargetException();
        if (t instanceof RuntimeException)
          throw (RuntimeException) t;
        Log.warn(e);
      }
      return null;
    }

    /*
     * Allowable input/output sequences:
     * [interpreted as POST/PUT]
     * - get output, [write output,] get input, [read input]
     * - get output, [write output]
     * [interpreted as GET]
     * - get input, [read input]
     * Disallowed:
     * - get input, [read input,] get output, [write output]
     */

    public synchronized OutputStream getOutputStream() throws IOException {
      return delegate.getOutputStream();
    }

    public synchronized InputStream getInputStream() throws IOException {
      return delegate.getInputStream();
    }

    public InputStream getErrorStream() {
      return delegate.getErrorStream();
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
      delegate.disconnect();
    }

    public boolean usingProxy() {
      return delegate.usingProxy();
    }

    /**
     * Returns an unmodifiable Map of the header fields.
     * The Map keys are Strings that represent the
     * response-header field names. Each Map value is an
     * unmodifiable List of Strings that represents
     * the corresponding field values.
     *
     * @return a Map of header fields
     * @since 1.4
     */
    public Map<String, List<String>> getHeaderFields() {
      return delegate.getHeaderFields();
    }

    /**
     * Gets a header field by name. Returns null if not known.
     *
     * @param name the name of the header field
     */
    public String getHeaderField(String name) {
      return delegate.getHeaderField(name);
    }

    /**
     * Gets a header field by index. Returns null if not known.
     *
     * @param n the index of the header field
     */
    public String getHeaderField(int n) {
      return delegate.getHeaderField(n);
    }

    /**
     * Gets a header field by index. Returns null if not known.
     *
     * @param n the index of the header field
     */
    public String getHeaderFieldKey(int n) {
      return delegate.getHeaderFieldKey(n);
    }

    /**
     * Sets request property. If a property with the key already
     * exists, overwrite its value with the new value.
     *
     * @param value the value to be set
     */
    public void setRequestProperty(String key, String value) {
      delegate.setRequestProperty(key, value);
    }

    /**
     * Adds a general request property specified by a
     * key-value pair.  This method will not overwrite
     * existing values associated with the same key.
     *
     * @param key   the keyword by which the request is known
     *              (e.g., "<code>accept</code>").
     * @param value the value associated with it.
     * @see #getRequestProperties(java.lang.String)
     * @since 1.4
     */
    public void addRequestProperty(String key, String value) {
      delegate.addRequestProperty(key, value);
    }

    /**
     * Overwrite super class method
     */
    public int getResponseCode() throws IOException {
      return delegate.getResponseCode();
    }

    public String getRequestProperty(String key) {
      return delegate.getRequestProperty(key);
    }

    /**
     * Returns an unmodifiable Map of general request
     * properties for this connection. The Map keys
     * are Strings that represent the request-header
     * field names. Each Map value is a unmodifiable List
     * of Strings that represents the corresponding
     * field values.
     *
     * @return a Map of the general request properties for this connection.
     * @throws IllegalStateException if already connected
     * @since 1.4
     */
    public Map<String, List<String>> getRequestProperties() {
      return delegate.getRequestProperties();
    }

    /*
     * We support JDK 1.2.x so we can't count on these from JDK 1.3.
     * We override and supply our own version.
     */

    public void setInstanceFollowRedirects(boolean shouldFollow) {
      delegate.setInstanceFollowRedirects(shouldFollow);
    }

    public boolean getInstanceFollowRedirects() {
      return delegate.getInstanceFollowRedirects();
    }

    public void setRequestMethod(String method) throws ProtocolException {
      delegate.setRequestMethod(method);
    }

    public String getRequestMethod() {
      return delegate.getRequestMethod();
    }

    public String getResponseMessage() throws IOException {
      return delegate.getResponseMessage();
    }

    public long getHeaderFieldDate(String name, long Default) {
      return delegate.getHeaderFieldDate(name, Default);
    }

    public Permission getPermission() throws IOException {
      return delegate.getPermission();
    }

    public URL getURL() {
      return delegate.getURL();
    }

    public int getContentLength() {
      return delegate.getContentLength();
    }

    public String getContentType() {
      return delegate.getContentType();
    }

    public String getContentEncoding() {
      return delegate.getContentEncoding();
    }

    public long getExpiration() {
      return delegate.getExpiration();
    }

    public long getDate() {
      return delegate.getDate();
    }

    public long getLastModified() {
      return delegate.getLastModified();
    }

    public int getHeaderFieldInt(String name, int Default) {
      return delegate.getHeaderFieldInt(name, Default);
    }

    public Object getContent() throws IOException {
      return delegate.getContent();
    }

    public Object getContent(Class[] classes) throws IOException {
      return delegate.getContent(classes);
    }

    public String toString() {
      return delegate.toString();
    }

    public void setDoInput(boolean doinput) {
      delegate.setDoInput(doinput);
    }

    public boolean getDoInput() {
      return delegate.getDoInput();
    }

    public void setDoOutput(boolean dooutput) {
      delegate.setDoOutput(dooutput);
    }

    public boolean getDoOutput() {
      return delegate.getDoOutput();
    }

    public void setAllowUserInteraction(boolean allowuserinteraction) {
      delegate.setAllowUserInteraction(allowuserinteraction);
    }

    public boolean getAllowUserInteraction() {
      return delegate.getAllowUserInteraction();
    }

    public void setUseCaches(boolean usecaches) {
      delegate.setUseCaches(usecaches);
    }

    public boolean getUseCaches() {
      return delegate.getUseCaches();
    }

    public void setIfModifiedSince(long ifmodifiedsince) {
      delegate.setIfModifiedSince(ifmodifiedsince);
    }

    public long getIfModifiedSince() {
      return delegate.getIfModifiedSince();
    }

    public boolean getDefaultUseCaches() {
      return delegate.getDefaultUseCaches();
    }

    public void setDefaultUseCaches(boolean defaultusecaches) {
      delegate.setDefaultUseCaches(defaultusecaches);
    }

    /*
     * finalize (dispose) the delegated object.  Otherwise
     * sun.net.www.protocol.http.HttpURLConnection's finalize()
     * would have to be made public.
     */

    protected void finalize() throws Throwable {
      delegate.dispose();
    }

    public boolean equals(Object obj) {
      return delegate.equals(obj);
    }

    public int hashCode() {
      return delegate.hashCode();
    }

    public void setConnectTimeout(int timeout) {
      delegate.setConnectTimeout(timeout);
    }

    public int getConnectTimeout() {
      return delegate.getConnectTimeout();
    }

    public void setReadTimeout(int timeout) {
      delegate.setReadTimeout(timeout);
    }

    public int getReadTimeout() {
      return delegate.getReadTimeout();
    }

    public void setFixedLengthStreamingMode(int contentLength) {
      delegate.setFixedLengthStreamingMode(contentLength);
    }

    public void setChunkedStreamingMode(int chunklen) {
      delegate.setChunkedStreamingMode(chunklen);
    }
  }


  public static class DelegateHttpsURLConnection extends AbstractDelegateHttpsURLConnection {

    // we need a reference to the HttpsURLConnection to get
    // the properties set there
    // we also need it to be public so that it can be referenced
    // from sun.net.www.protocol.http.HttpURLConnection
    // this is for ResponseCache.put(URI, URLConnection)
    // second parameter needs to be cast to javax.net.ssl.HttpsURLConnection
    // instead of AbstractDelegateHttpsURLConnection
    public javax.net.ssl.HttpsURLConnection httpsURLConnection;

    private boolean hacking;
    private PrintStream savedStream;
    private PatcherPrintStream patcherStream;

    DelegateHttpsURLConnection(URL url, sun.net.www.protocol.http.Handler handler,
      javax.net.ssl.HttpsURLConnection httpsURLConnection) throws IOException
    {
      this(url, null, handler, httpsURLConnection);
    }

    DelegateHttpsURLConnection(URL url, Proxy p, sun.net.www.protocol.http.Handler handler,
      javax.net.ssl.HttpsURLConnection httpsURLConnection) throws IOException
    {
      super(url, p, handler);
      this.httpsURLConnection = httpsURLConnection;
    }

    protected javax.net.ssl.SSLSocketFactory getSSLSocketFactory() {
      return httpsURLConnection.getSSLSocketFactory();
    }

    protected javax.net.ssl.HostnameVerifier getHostnameVerifier() {
      return httpsURLConnection.getHostnameVerifier();
    }

    /*
     * Called by layered delegator's finalize() method to handle closing
     * the underlying object.
     */

    protected void dispose() throws Throwable {
      super.finalize();
    }


    @Override
    public synchronized void doTunneling() throws IOException {
      hacking = true;
      hackStream();
      try {
        super.doTunneling();
      } finally {
        hacking = false;
        HttpClient h = http;
        PatcherPrintStream ps = patcherStream;
        if (h != null && ps != null && h.serverOutput == ps) {
          h.serverOutput = savedStream;
        }
        patcherStream = null;
        savedStream = null;
      }
    }

    @Override
    protected void proxiedConnect(URL url, String proxyHost, int proxyPort, boolean useCache) throws IOException {
      super.proxiedConnect(url, proxyHost, proxyPort, useCache);
      if (hacking) {
        hackStream();
      }
    }

    private void hackStream() {
      HttpClient h = http;
      if (h == null)
        return;
      PrintStream out = h.serverOutput;
      if (out == null || out instanceof PatcherPrintStream) {
        return;
      }
      savedStream = out;
      patcherStream = new PatcherPrintStream(savedStream);
      if (h.serverOutput == out) {
        h.serverOutput = patcherStream;
      }
    }


    static class PatcherPrintStream extends PrintStream {
      private final PrintStream myDelegate;

      public PatcherPrintStream(PrintStream delegate) {
        super(delegate);
        myDelegate = delegate;
      }

      public void flush() {
        myDelegate.flush();
      }

      public void close() {
        myDelegate.close();
      }

      public boolean checkError() {
        return myDelegate.checkError();
      }

      public void write(int b) {
        myDelegate.write(b);
      }

      public void write(byte[] buf, int off, int len) {
        myDelegate.write(buf, off, len);
      }

      public void print(boolean b) {
        myDelegate.print(b);
      }

      public void print(char c) {
        myDelegate.print(c);
      }

      public void print(int i) {
        myDelegate.print(i);
      }

      public void print(long l) {
        myDelegate.print(l);
      }

      public void print(float f) {
        myDelegate.print(f);
      }

      public void print(double d) {
        myDelegate.print(d);
      }

      public void print(char[] s) {
        String str = preprocess(new String(s));
        myDelegate.print(str);
      }

      public void print(String s) {
        s = preprocess(s);
        myDelegate.print(s);
      }

      // 0 - waiting CONNECT
      // 1 - CONNECT, waiting \r\n
      int state = 0;

      // if true, last portion did not end with \r\n
      boolean hangingLine = false;

      String preprocess(String s) {
        int i = 0;
        do {
          String sub = s.substring(i);
          boolean found = false;
          if (!hangingLine) {
            if (state == 0 && sub.startsWith("CONNECT")) {
              state = 1;
            } else if (state == 1) {
              if (sub.startsWith("Proxy-Connection: ")) {
                found = true;
              }
            }
          }
          // end of line
          int k = s.indexOf('\r', i);
          // start of next line
          int p = -1;
          if (k >= 0) {
            p = k + 1;
            if (p < s.length() && s.charAt(p) == '\n')
              p++;
            else
              Log.warn("JHNPH: \\r without \\n");
          }

          if (!hangingLine && state == 1 && (found || k == i)) {
            // if found - replace (with the \r\n), if inserting - keep
            state = 0;
            String rest = found ? s.substring(p) : s.substring(i);
            return s.substring(0, i) + "Proxy-Connection: keep-alive\r\n" + rest;
          }

          i = p;
        } while (i >= 0 && i < s.length());
        hangingLine = i < 0;
        return s;
      }

      public void print(Object obj) {
        myDelegate.print(obj);
      }

      public void println() {
        myDelegate.println();
      }

      public void println(boolean x) {
        myDelegate.println(x);
      }

      public void println(char x) {
        myDelegate.println(x);
      }

      public void println(int x) {
        myDelegate.println(x);
      }

      public void println(long x) {
        myDelegate.println(x);
      }

      public void println(float x) {
        myDelegate.println(x);
      }

      public void println(double x) {
        myDelegate.println(x);
      }

      public void println(char[] x) {
        myDelegate.println(x);
      }

      public void println(String x) {
        myDelegate.println(x);
      }

      public void println(Object x) {
        myDelegate.println(x);
      }

      public PrintStream printf(String format, Object... args) {
        return myDelegate.printf(format, args);
      }

      public PrintStream printf(Locale l, String format, Object... args) {
        return myDelegate.printf(l, format, args);
      }

      public PrintStream format(String format, Object... args) {
        return myDelegate.format(format, args);
      }

      public PrintStream format(Locale l, String format, Object... args) {
        return myDelegate.format(l, format, args);
      }

      public PrintStream append(CharSequence csq) {
        return myDelegate.append(csq);
      }

      public PrintStream append(CharSequence csq, int start, int end) {
        return myDelegate.append(csq, start, end);
      }

      public PrintStream append(char c) {
        return myDelegate.append(c);
      }

      public void write(byte[] b) throws IOException {
        myDelegate.write(b);
      }
    }
  }
}
