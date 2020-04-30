package com.almworks.restconnector;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.api.connector.http.HttpDumper;
import com.almworks.api.connector.http.HttpFailureConnectionException;
import com.almworks.api.connector.http.dump.RequestDumper;
import com.almworks.api.http.*;
import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.jira.connector2.JiraCredentialsRequiredException;
import com.almworks.jira.connector2.JiraEnv;
import com.almworks.util.LogHelper;
import com.almworks.util.LogPrivacyPolizei;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Condition;
import org.almworks.util.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RestSession {
  public static final TypedKey<List<Cookie>> SESSION_COOKIES = TypedKey.create("sessionIdCookie");

  private final String myBaseUrl;
  private final HttpMaterial myMaterial;
  @NotNull
  private final List<HttpDumper.DumpSpec> myDumperSpec;
  private final SNIErrorHandler mySNIErrorHandler;
  private final UserDataHolder myUserData = new UserDataHolder();
  private JiraCredentials myCredentials;
  private final UserDataHolder mySessionData;
  private boolean myInitStarted = false;

  public RestSession(String baseUrl, HttpMaterial material, @Nullable List<HttpDumper.DumpSpec> dumperSpec,
                     @NotNull JiraCredentials credentials, UserDataHolder sessionData, SNIErrorHandler sniErrorHandler) {
    myCredentials = credentials;
    mySessionData = sessionData != null ? sessionData : new UserDataHolder();
    mySNIErrorHandler = sniErrorHandler;
    if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
    myBaseUrl = baseUrl;
    myMaterial = material;
    myDumperSpec = Util.NN(dumperSpec, Collections.<HttpDumper.DumpSpec>emptyList());
  }

  void updateSessionCookies() {
    List<Cookie> sessionIdCookies = mySessionData.getUserData(SESSION_COOKIES);
    if (sessionIdCookies != null) {
      HttpState state = getHttpClient().getState();
      state.clearCookies();
      for (Cookie cookie : sessionIdCookies)
        state.addCookie(new Cookie(cookie.getDomain(), cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getExpiryDate(), cookie.getSecure()));
    }
  }

  public void setSessionCookies(List<Cookie> cookies) {
    mySessionData.putUserData(SESSION_COOKIES, cookies);
    updateSessionCookies();
  }

  public static RestSession create(String baseUrl, JiraCredentials credentials, HttpMaterial material, @Nullable UserDataHolder sessionData, SNIErrorHandler sniErrorHandler) {
    return new RestSession(baseUrl, material, JiraEnv.getHttpDumperSpecs(), credentials, sessionData, sniErrorHandler);
  }

  public SNIErrorHandler getSNIErrorHandler() {
    return mySNIErrorHandler;
  }

  @NotNull
  public UserDataHolder getSessionData() {
    return mySessionData;
  }

  public Cookie[] getCookies() {
    return getHttpClient().getState().getCookies();
  }

  @NotNull
  public HttpClient getHttpClient() {
    return myMaterial.getHttpClient();
  }

  public String getBaseUrl() {
    return myBaseUrl;
  }

  public UserDataHolder getUserData() {
    return myUserData;
  }

  public String getRestResourcePath(String path) {
    return myBaseUrl + "rest/" + path;
  }

  @NotNull
  public RestResponse restPostJson(String path, @Nullable JSONObject request, RequestPolicy policy) throws ConnectorException {
    return postString(path, request != null ? request.toJSONString() : null, policy);
  }

  public RestResponse postString(String path, String rawString, RequestPolicy policy) throws ConnectorException {
    String url = getRestResourcePath(path);
    return perform(PutPost.postJSON(url, getDebugName(path), null, rawString), policy);
  }

  /**
   * Performs GET request for REST API resource
   * @param path remove resource path relative to BASE_URL/rest
   * @see #doGet(String, RequestPolicy)
   */
  public RestResponse restGet(String path, RequestPolicy policy) throws ConnectorException {
    String url = getRestResourcePath(path);
    return perform(GetDelete.get(url, getDebugName(path)), policy);
  }

  public RestResponse restDelete(String path, RequestPolicy policy) throws ConnectorException {
    String url = getRestResourcePath(path);
    return perform(GetDelete.delete(url, getDebugName(path)), policy);
  }

  @NotNull
  public RestResponse restPut(String path, @NotNull JSONObject request, RequestPolicy policy) throws ConnectorException {
    return putString(path, request.toJSONString(), policy);
  }

  @NotNull
  public RestResponse putString(String path, @NotNull String request, RequestPolicy policy) throws ConnectorException {
    String url = getRestResourcePath(path);
    return perform(PutPost.putJSON(url, request, getDebugName(path), null), policy);
  }


  /**
   * Performs GET request for an arbitrary resource.
   * @param path remote resource path relative to JIRA base URL.
   * @param policy the request policy
   * @see #restGet(String, RequestPolicy)
   */
  public RestResponse doGet(String path, RequestPolicy policy) throws ConnectorException {
    String url = myBaseUrl + path;
    return perform(GetDelete.get(url, getDebugName(path)), policy);
  }

  public static String getDebugName(String path) {
    int end = path.indexOf("?");
    String debugName = end < 0 ? path : path.substring(0, end);
    return "rest." + debugName.replace('/', '_');
  }

  @NotNull
  public RestResponse perform(Request request, RequestPolicy policy) throws ConnectorException {
    return perform(new Job(request, policy, false)).ensureHasResponse();
  }

  private final ThreadLocal<Job> myCurrentJob = new ThreadLocal<>();
  @Contract("!null -> !null")
  public Job perform(Job job) {
    if (job == null) return null;
    preparePerform(job);
    try {
      doPerform(job);
    } catch (ConnectorException e) {
      if (job.myFailure == null) job.myFailure = e;
      job.releaseResponse();
    } finally {
      if (myCurrentJob.get() == job) myCurrentJob.set(null);
    }
    validatePerform(job);
    return job;
  }

  private void validatePerform(Job job) {
    if (job.myResponse == null && job.myFailure == null) {
      LogHelper.error("Missing job result", job);
      throw new Failure();
    }
    if (job.getSession() == null) {
      LogHelper.error("Missing session", job);
      throw new Failure();
    }
  }

  private void preparePerform(Job job) {
    if (job.myRequest == null) {
      LogHelper.error("Missing request", job);
      throw new Failure();
    }
    if (!job.isAuxiliary() && job.myPolicy == null) {
      LogHelper.error("Missing policy for primary job", job);
      throw new Failure();
    }
    Job primaryJob = myCurrentJob.get();
    if (primaryJob != null) {
      if (!job.isAuxiliary()) {
        LogHelper.error("Another primary job is running", primaryJob, job);
        throw new Failure();
      }
      primaryJob.releaseResponse();
    }
    if (job.mySession != null) {
      LogHelper.error("Already performed job", job, this);
      throw new Failure();
    } else job.mySession = this;
    if (!job.isAuxiliary()) myCurrentJob.set(job);
  }

  private void ensureInitialized() throws ConnectorException {
    if (myInitStarted) return;
    updateSessionCookies();
    myInitStarted = true;
    myCredentials.initNewSession(this);
  }

  private void doPerform(Job job) throws ConnectorException {
    ensureInitialized();
    if (job.isNeedsLogin()) myCredentials.ensureLoggedIn(this, true);
    int attempt = 0;
    boolean forcedLogin = false;
    while (true) {
      ensureInitialized();
      RestResponse response;
      try {
        job.load();
        response = job.ensureHasResponse();
      } catch (ConnectionException e) {
        int httpCode = HttpFailureConnectionException.findHttpCode(e);
        if (!shouldRetry(attempt, httpCode, job)) throw e;
        LogHelper.warning("Waiting before retry", attempt, httpCode, job);
        try {
          Thread.sleep(2000); // Wait before retry, see https://jira.almworks.com/browse/JCO-1537
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          LogHelper.warning(interrupted);
          throw e;
        }
        attempt++;
        continue;
      }
      if (!job.isAuxiliary()) {
        JiraCredentials.ResponseCheck check = myCredentials.checkResponse(this, job, response);
        if (check.isFailed()) {
          LogHelper.warning("Response check failed:", check);
          job.releaseResponse();
          if (forcedLogin) throw new JiraCredentialsRequiredException();
          forcedLogin = true;
          myCredentials.ensureLoggedIn(this, false);
          continue;
        }
      }
      if (shouldRetry(attempt, response.getStatusCode(), job)) {
        if (job.isAttemptsExceeded(attempt)) return;
        attempt++;
        job.releaseResponse();
        continue;
      }
      return;
    }
  }

  @NotNull
  public JiraCredentials getCredentials() {
    return myCredentials;
  }

  /**
   * {@link JiraCredentials} implementation may need to replace itself with actual credentials.
   * Must be called only by {@link JiraCredentials} implementation during login check.
   * @param credentials updated credentials
   */
  public void updateCredentials(@NotNull JiraCredentials credentials) {
    //noinspection ConstantConditions
    if (credentials != null && credentials != myCredentials) {
      LogHelper.warning("Credentials updated", myBaseUrl, myCredentials, credentials);
      myCredentials = credentials;
      HttpState state = getHttpClient().getState();
      state.clearCookies();
      myUserData.putUserData(SESSION_COOKIES, null);
      myInitStarted = false;
    }
  }

  /**
   * @param httpCode HTTP status code (positive value). Zero value means ignore status code
   * @return true if the policy requires to perform retry in the current state. false - terminate request with current failure.
   */
  private boolean shouldRetry(int attemptCount, int httpCode, Job job) {
    if (httpCode > 0 && !RequestPolicy.HTTP_FAILURES.contains(httpCode)) return false;
    if (job.isAttemptsExceeded(attemptCount)) return false;
    LogHelper.warning("Retrying after", httpCode, attemptCount, job);
    return true;
  }

  public void dispose() {
    myMaterial.dispose();
  }

  public static abstract class Request implements HttpMethodFactory {
    private final String myUrl;
    private final String myDebugName;
    @Nullable
    private final LogPrivacyPolizei myPolizei;
    private final Map<String, String> myRequestHeaders = Collections15.hashMap();
    private Boolean myCopyQueryOnRedirect = null;

    protected Request(String url, String debugName, @Nullable LogPrivacyPolizei polizei) {
      myUrl = url;
      myDebugName = debugName;
      myPolizei = polizei;
    }

    public String getUrl() {
      return myUrl;
    }

    @Override
    public final HttpMethodBase create() throws HttpMethodFactoryException {
      HttpMethodBase method = create(myUrl);
      for (Map.Entry<String, String> entry : myRequestHeaders.entrySet()) method.addRequestHeader(entry.getKey(), entry.getValue());
      return method;
    }

    public void addRequestHeader(String name, String value) {
      myRequestHeaders.put(name, value);
    }

    /**
     * @see HttpLoader#setCopyQueryOnRedirect(boolean)
     */
    public void setCopyQueryOnRedirect(boolean copyQueryOnRedirect) {
      myCopyQueryOnRedirect = copyQueryOnRedirect;
    }

    protected abstract HttpMethodBase create(String url) throws HttpMethodFactoryException;

    protected Collection<? extends RedirectURIHandler> getRedirectHandlers() { return Collections.emptyList(); }

    private RestResponse load(RestSession session) throws ConnectionException {
      HttpLoader loader = session.myMaterial.createLoader(myUrl, this);
      loader.setFailedStatusApprover(new Condition<Integer>() {
        @Override
        public boolean isAccepted(Integer value) {
          return false;
        }
      });
      loader.setRetries(1);
      if (myCopyQueryOnRedirect != null) loader.setCopyQueryOnRedirect(myCopyQueryOnRedirect);
      getRedirectHandlers().forEach(loader::addRedirectUriHandler);
      RequestDumper requestDumper = RequestDumper.prepare(session.myDumperSpec, loader, myDebugName, myPolizei, myUrl);
      dumpRequest(requestDumper);
      HttpResponseData response;
      boolean success = false;
      try {
        response = loader.load();
        success = true;
      } catch (IOException e) {
        session.mySNIErrorHandler.checkException(e, myUrl);
        requestDumper.finishWithException(e);
        Log.debug("connection failure", e);
        throw new ConnectionException(myUrl, "connection failure", e);
      } catch (HttpCancelledException e) {
        requestDumper.finishWithException(e);
        throw new ConnectionException(myUrl, "cancelled", e);
      } catch (HttpConnectionException e) {
        session.mySNIErrorHandler.checkException(e, myUrl);
        requestDumper.finishWithException(e);
        throw new HttpFailureConnectionException(myUrl, e.getStatusCode(), e.getStatusText());
      } catch (HttpLoaderException e) {
        session.mySNIErrorHandler.checkException(e, myUrl);
        requestDumper.finishWithException(e);
        Log.debug("connection failure", e);
        throw new ConnectionException(myUrl, "connection failure", e);
      } finally {
        if (!success) requestDumper.finishWithException(null);
      }
      return new RestResponse(myUrl, requestDumper.responseObtained(response), this);
    }

    private void dumpRequest(RequestDumper dumper) {
      dumper.addRequestHeaders(myRequestHeaders);
      doDumpRequest(dumper);
    }

    protected abstract void doDumpRequest(RequestDumper dumper);

    @Override
    public String toString() {
      return myUrl + (myDebugName != null && !myDebugName.isEmpty() ? " (" + myDebugName + ")" : "");
    }
  }

  public static class PutPost extends Request {
    @Nullable
    private final RequestEntity myRequestEntity;
    private final boolean myPost;

    private PutPost(String url, @Nullable RequestEntity requestEntity, String debugName, LogPrivacyPolizei polizei, boolean post) {
      super(url, debugName, polizei);
      myPost = post;
      LogHelper.assertError(requestEntity == null || requestEntity.isRepeatable(), requestEntity); // Can not dump not repeatable requests
      myRequestEntity = requestEntity;
      addRequestHeader("Content-Type", "application/json;charset=UTF-8");
    }

    public static Request postJSON(String url, String debugName, @Nullable LogPrivacyPolizei polizei, String requestString) {
      try {
        return new PutPost(url, requestString != null ? new ByteArrayRequestEntity(requestString.getBytes("UTF-8")) : null, debugName, polizei, true);
      } catch (UnsupportedEncodingException e) {
        LogHelper.error(e);
        throw new RuntimeException(e); // Should not happen
      }
    }

    public static Request putJSON(String url, String request, String debugName, @Nullable LogPrivacyPolizei polizei) {
      try {
        return new PutPost(url, new ByteArrayRequestEntity(request.getBytes("UTF-8")), debugName, polizei, false);
      } catch (UnsupportedEncodingException e) {
        LogHelper.error(e);
        throw new RuntimeException(e); // Should not happen
      }
    }

    @Override
    protected void doDumpRequest(RequestDumper dumper) {
      if (myRequestEntity != null && myRequestEntity.isRepeatable()) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
          myRequestEntity.writeRequest(out);
          dumper.setRawRequest(out.toByteArray());
        } catch (IOException e) {
          LogHelper.error(e);
        }
      }
    }

    @Override
    protected HttpMethodBase create(String url) throws HttpMethodFactoryException {
      EntityEnclosingMethod method = myPost ? createPost(url) : createPut(url);
      if (myRequestEntity != null) method.setRequestEntity(myRequestEntity);
      return method;
    }

    private EntityEnclosingMethod createPut(String url) throws HttpMethodFactoryException {
      return HttpUtils.createPut(url);
    }

    private EntityEnclosingMethod createPost(String url) throws HttpMethodFactoryException {
      return HttpUtils.createPost(url);
    }
  }

  public static class GetDelete extends Request {
    private final boolean myGet;

    public GetDelete(String url, boolean get, String debugName, LogPrivacyPolizei polizei) {
      super(url, debugName, polizei);
      myGet = get;
    }

    public static Request get(String url, String debugName) {
      return new GetDelete(url, true, debugName, null);
    }

    public static Request delete(String url, String debugName) {
      return new GetDelete(url, false, debugName, null);
    }

    @Override
    protected void doDumpRequest(RequestDumper dumper) {
    }

    @Override
    protected HttpMethodBase create(String url) throws HttpMethodFactoryException {
      return myGet ? HttpUtils.createGet(url) : HttpUtils.createDelete(url);
    }
  }

  public static class UrlEncodedPost extends Request {
    private final List<NameValuePair> myParametersList;
    private final Collection<? extends RedirectURIHandler> myRedirects;

    public UrlEncodedPost(String url, String debugName, @Nullable LogPrivacyPolizei logPrivacyPolizei, List<NameValuePair> parametersList,
      @Nullable Collection<? extends RedirectURIHandler> redirects) {
      super(url, debugName, logPrivacyPolizei);
      myParametersList = parametersList;
      myRedirects = redirects != null ? redirects : Collections.emptySet();
    }

    @Override
    protected HttpMethodBase create(String url) throws HttpMethodFactoryException {
      PostMethod method = HttpUtils.createPost(url);
      method.addParameters(myParametersList.toArray(new NameValuePair[myParametersList.size()]));
      return method;
    }

    @Override
    protected Collection<? extends RedirectURIHandler> getRedirectHandlers() {
      return myRedirects;
    }

    @Override
    protected void doDumpRequest(RequestDumper dumper) {
      dumper.setPostParameters(myParametersList);
    }
  }

  public static class Job {
    /**
     * The session which performs the Job. Caller creates the job without session. This value is set during execution.
     */
    private RestSession mySession;
    /**
     * The request being performed
     */
    private Request myRequest;
    /**
     * The request's policy. Not applicable to {@link #myAuxiliary auxiliary} jobs
     */
    private RequestPolicy myPolicy = RequestPolicy.FAILURE_ONLY;
    /**
     * Result of the job (if successful)
     */
    private RestResponse myResponse;
    /**
     * Marked as auxiliary. The auxiliary jobs does not perform any session checks.<br>
     * The session performs only one primary job at a time. And may perform one nested auxiliary job.
     */
    private boolean myAuxiliary;
    /** Reason why the job is failed */
    private ConnectorException myFailure;

    @NotNull
    public static Job auxiliary(Request request) {
      return new Job(request, RequestPolicy.FAILURE_ONLY, true);
    }

    public Job(Request request, RequestPolicy policy, boolean auxiliary) {
      setRequest(request);
      setPolicy(policy);
      setAuxiliary(auxiliary);
    }

    public final void setRequest(Request request) {
      if (mySession != null) LogHelper.error("Already running", mySession);
      else myRequest = request;
    }

    public final void setPolicy(RequestPolicy policy) {
      if (mySession != null) LogHelper.error("Already running", mySession);
      else myPolicy = policy;
    }

    public boolean isAuxiliary() {
      return myAuxiliary;
    }

    public final void setAuxiliary(boolean auxiliary) {
      if (mySession != null) LogHelper.error("Already running", mySession);
      else myAuxiliary = auxiliary;
    }

    public RestSession getSession() {
      return mySession;
    }

    public Request getRequest() {
      return myRequest;
    }

    public RequestPolicy getPolicy() {
      return myPolicy;
    }

    @NotNull
    public RestResponse ensureHasResponse() throws ConnectorException {
      if (myResponse != null) return myResponse;
      if (myFailure != null) throw myFailure;
      if (mySession == null) LogHelper.error("Not performed", this);
      else LogHelper.error("Job performed without result");
      throw new Failure();
    }

    private void releaseResponse() {
      if (myResponse != null) myResponse.releaseConnection();
      myResponse = null;
    }

    private boolean isNeedsLogin() {
      return !myAuxiliary && getPolicy().isNeedsLogin();
    }

    private void load() throws ConnectionException {
      releaseResponse();
      try {
        myResponse = myRequest.load(mySession);
      } catch (ConnectorException e) {
        myFailure = e;
        throw e;
      }
    }

    public boolean isAttemptsExceeded(int attemptCount) {
      return attemptCount >= myPolicy.getRetryOnServerFailure();
    }
  }
}
