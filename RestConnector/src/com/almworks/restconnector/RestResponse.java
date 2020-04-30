package com.almworks.restconnector;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.CannotParseException;
import com.almworks.api.connector.http.ConnectionException;
import com.almworks.api.connector.http.HtmlUtils;
import com.almworks.api.connector.http.HttpFailureConnectionException;
import com.almworks.api.connector.http.dump.ResponseDumper;
import com.almworks.api.http.HttpResponseData;
import com.almworks.api.http.HttpUtils;
import com.almworks.jira.connector2.JiraException;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.sax.LocationHandler;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.ProcedureE;
import com.almworks.util.io.CharValidator;
import com.almworks.util.io.StreamTransferTracker;
import com.almworks.util.io.XMLCharValidator;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.apache.commons.httpclient.URI;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestResponse {
  /**
   * HTTP Header name. The value of this header contains logged-in username. However, it may contain other (arbitrary) username.
   * Seems wrong username is provided for cacheable resources.
   */
  public static final String X_AUSERNAME = "X-AUSERNAME";
  public static final String ANONYMOUS_USER = "anonymous";

  private final String myUrl;
  private final HttpResponseData myData;
  private final RestSession.Request myRequest;
  private int myStatusCode = 0;
  private String myStringContent;

  public RestResponse(String url, ResponseDumper response, RestSession.Request request) {
    myUrl = url;
    myData = response;
    myRequest = request;
  }

  public RestSession.Request getRequest() {
    return myRequest;
  }

  public String getString() throws ConnectionException {
    if (myStringContent != null) return myStringContent;
    return getString(CharValidator.ALL_VALID);
  }

  /**
   * This method returns null if the content is not loaded yet or was loaded via stream processing methods.<br>
   * This method differs from {@link #getString()}:<br>
   * 1. It never fails due to network problems.<br>
   * 2. It doesn't fail if the content stream is already processed read (via {@link HttpResponseData#readStream(com.almworks.util.commons.ProcedureE) readStream method}<br>
   * This method is designed for logging
   * @return loaded string content if is was loaded by {@link #getString()} method.
   */
  @Nullable("When not loaded yet")
  public String getLoadedString() {
    return myStringContent;
  }

  public String getString(CharValidator validator) throws ConnectionException {
    if (myStringContent != null) {
      LogHelper.warning("String is already loaded. Validator ignored");
      return myStringContent;
    }
    try {
      myStringContent = HttpUtils.transferToString(myData, validator);
    } catch (IOException e) {
      throw new ConnectionException(getLastUrl(), "connection failure", e);
    }
    return myStringContent;
  }

  @Nullable
  public Object getJSON() throws ConnectionException, ParseException {
    String text = getString();
    if (text == null || text.isEmpty()) return null;
    try {
      return new JSONParser().parse(text);
    } catch (ParseException e) {
      if (text.length() > 200) text = text.substring(0, 200);
      LogHelper.warning(getDebugTarget(), e, getStatusCode(), text);
      throw e;
    }
  }

  public int getStatusCode() {
    if (myStatusCode > 0) return myStatusCode;
    int code = myData.getStatusCode();
    LogHelper.assertError(code != 0, "Not executed", getDebugTarget());
    myStatusCode = code;
    return code;
  }

  public boolean isSuccessful() {
    return getStatusCode() / 100 == 2;
  }

  public String getLastUrl() {
    URI lastURI = myData.getLastURI();
    return lastURI != null ? lastURI.toString() : myUrl;
  }

  public String getStatusText() {
    return myData.getStatusText();
  }

  public void ensureSuccessful() throws ConnectorException {
    if (!isSuccessful()) throw createErrorResponse().toException();
  }

  public ErrorResponse createErrorResponse() {
    if ("application/json".equals(myData.getContentType())) {
      try {
        JSONObject errorObj = getJSONObject();
        String message = Util.castNullable(String.class, errorObj.get("errorMessages"));
        List messages;
        if (message != null) messages = Collections.singletonList(message);
        else messages = Util.castNullable(List.class, errorObj.get("errorMessages"));
        Map errors = Util.castNullable(Map.class, errorObj.get("errors"));
        if ((messages != null && !messages.isEmpty()) || (errors != null && !errors.isEmpty())) {
          return ErrorResponse.errorMessages(messages, errors);
        }
      } catch (ConnectionException | ParseException e) {
        // ignore
      }
    }
    return ErrorResponse.noDetails(new HttpFailureConnectionException(getLastUrl(), getStatusCode(), getStatusText()));
  }

  public String getResponseHeader(String header) {
    Map<String,String> headers = myData.getResponseHeaders();
    return headers.get(header);
  }

  @NotNull
  public JSONObject getJSONObject() throws ConnectionException, ParseException {
    Object json = getJSON();
    JSONObject object = JSONKey.ROOT_OBJECT.getValue(json);
    if (object == null) {
      LogHelper.debug("Expected JSON object", json);
      LogHelper.error("Failed to parse JSON object", getDebugTarget());
      throw new ParseException(ParseException.ERROR_UNEXPECTED_EXCEPTION);
    }
    return object;
  }

  public void parseJSON(final LocationHandler handler) throws ConnectionException, CannotParseException {
    parseJSON(handler, null);
  }

  public void parseJSON(final LocationHandler handler, @Nullable final StreamTransferTracker tracker) throws ConnectionException, CannotParseException {
    class Parser implements ProcedureE<InputStream, IOException> {
      private ParseException myException;

      @Override
      public void invoke(InputStream stream) throws IOException {
        stream = StreamTransferTracker.TrackStream.wrap(stream, tracker);
        InputStreamReader reader = new InputStreamReader(stream, myData.getCharset());
        JSONParser parser = new JSONParser();
        try {
          parser.parse(reader, new LocationHandler.ContentAdapter(handler));
        } catch (ParseException e) {
          myException = e;
        }
      }
    }
    try {
      Parser reader = new Parser();
      myData.readStream(reader);
      if (reader.myException != null) throw new CannotParseException(getLastUrl(), "cannot parse JSON output", reader.myException);
    } catch (IOException e) {
      throw new ConnectionException(getLastUrl(), "parser IO failure", e);
    }
  }

  @NotNull
  public Document getHtml() throws ConnectionException, CannotParseException {
    String string = getString(XMLCharValidator.INSTANCE);
    try {
      return HtmlUtils.buildHtmlDocument(new InputSource(new StringReader(string)));
    } catch (IOException e) {
      LogHelper.debug("load failure", e, getDebugTarget());
      throw new ConnectionException(getLastUrl(), "load failure", e);
    } catch (SAXException e) {
      Log.warn("cannot parse html output:\n----------[ " + getDebugTarget() + " ]----------\n" + string +
        "\n-------------------------------------", e);
      throw new CannotParseException(getLastUrl(), "cannot parse html output", e);
    }
  }

  public String getDebugTarget() {
    URI lastURI = myData.getLastURI();
    if (lastURI == null) return myUrl;
    String strUri = lastURI.toString();
    if (Util.equals(strUri, myUrl)) return myUrl;
    return myUrl + " -> " + lastURI;
  }

  void releaseConnection() {
    myData.releaseConnection();
  }

  public void dumpResponse() {
    HttpUtils.dumpResponse(myData);
  }

  public HttpResponseData getHttpResponse() {
    return myData;
  }

  public static class ErrorResponse {
    private final List<String> myMessages;
    private final Map<String, String> myErrors;
    private final ConnectorException myException;

    public ErrorResponse(List<String> messages, Map<String, String> errors, ConnectorException exception) {
      myMessages = messages;
      myErrors = errors;
      myException = exception;
    }

    public static ErrorResponse noDetails(HttpFailureConnectionException e) {
      return new ErrorResponse(Collections.emptyList(), Collections.emptyMap(), e);
    }

    public static ErrorResponse errorMessages(List<?> messages, Map errors) {
      messages = Util.NN(messages, Collections.emptyList());
      errors = Util.NN(errors, Collections.emptyMap());
      ArrayList<String> strMessages = new ArrayList<>();
      StringBuilder stringBuilder = new StringBuilder();
      for (Object m : messages) {
        String str = String.valueOf(m);
        strMessages.add(str);
        if (stringBuilder.length() > 0) stringBuilder.append(String.format("%n"));
        stringBuilder.append(str);
      }
      Map<String, String> strErrors = new HashMap<>();
      //noinspection unchecked
      for (Map.Entry o : ((Collection<Map.Entry>) errors.entrySet())) {
        String key = String.valueOf(o.getKey());
        String value = String.valueOf(o.getValue());
        strErrors.put(key, value);
        if (stringBuilder.length() > 0) stringBuilder.append(String.format("%n"));
        stringBuilder.append(key).append(": ").append(value);
      }
      String message = stringBuilder.toString();
      return new ErrorResponse(strMessages, strErrors, new JiraException(message, message, message, JiraException.JiraCause.COMPATIBILITY));
    }

    public ConnectorException toException() {
      return myException;
    }

    @Nullable
    public Matcher findMessage(Pattern pattern) {
      for (String message : myMessages) {
        Matcher m = pattern.matcher(message);
        if (m.find()) return m;
      }
      for (String error : myErrors.values()) {
        Matcher m = pattern.matcher(error);
        if (m.find()) return m;
      }
      return null;
    }

    public boolean hasDetails() {
      return !myMessages.isEmpty() || !myErrors.isEmpty();
    }

    public String getFullMessage() {
      return myException.getLongDescription();
    }
  }
}
