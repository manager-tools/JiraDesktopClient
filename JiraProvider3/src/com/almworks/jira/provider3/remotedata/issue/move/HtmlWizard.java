package com.almworks.jira.provider3.remotedata.issue.move;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.connector.http.ExtractFormParameters;
import com.almworks.api.http.HttpLoaderException;
import com.almworks.api.http.HttpMaterial;
import com.almworks.api.http.HttpUtils;
import com.almworks.api.http.RedirectURIHandler;
import com.almworks.jira.provider3.services.upload.UploadProblem;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.collections.MultiMap;
import com.almworks.util.text.TextUtil;
import com.almworks.util.xml.JDOMUtils;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URI;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

class HtmlWizard {
  private final RestSession mySession;
  private String myLastUrl;
  private Document myDocument;

  private HtmlWizard(RestSession session) {
    mySession = session;
  }

  public static HtmlWizard start(RestSession session, String path) throws ConnectorException {
    HtmlWizard wizard = new HtmlWizard(session);
    wizard.start(path);
    return wizard;
  }

  private void start(String path) throws ConnectorException {
    myLastUrl = mySession.getBaseUrl() + path;
    RestResponse response = mySession.doGet(path, RequestPolicy.SAFE_TO_RETRY);
    nextLoaded(response);
  }

  @NotNull
  public Document nextLoaded(RestResponse response) throws ConnectorException {
    myLastUrl = response.getLastUrl();
    myDocument = null;
    response.ensureSuccessful();
    myDocument = response.getHtml();
    return myDocument;
  }

  @NotNull
  public FormWrapper findMandatoryForm(String action) throws NoFormException {
    FormWrapper form = findForm(action);
    if (form == null) throw new NoFormException("action: " + action);
    return form;
  }

  @NotNull
  public FormWrapper findMandatoryForm(String ... actionParts) throws NoFormException {
    FormWrapper form = findForm(actionParts);
    if (form == null) throw new NoFormException("parts: " + TextUtil.separate(actionParts, ", "));
    return form;
  }

  @Nullable
  public FormWrapper findForm(String action) {
    Element form = FormWrapper.findFormElement(myDocument, action);
    return form == null ? null : createFormWrapper(form, action);
  }

  private FormWrapper createFormWrapper(Element form, String action) {
    ExtractFormParameters extract = new ExtractFormParameters(form, true);
    extract.perform();
    MultiMap<String, String> parameters = MultiMap.create(extract.getParameters());
    return new FormWrapper(form, parameters, action, extract);
  }

  @Nullable
  public FormWrapper findForm(String ... actionParts) {
    Pair<Element, String> elementAction = FormWrapper.findFormElement(myDocument, actionParts);
    return elementAction != null ? createFormWrapper(elementAction.getFirst(), elementAction.getSecond()) : null;
  }

  @NotNull
  public Document submit(FormWrapper form) throws UploadProblem.Thrown, ConnectorException {
    RestResponse response = submitGetResponse(form);
    return nextLoaded(response);
  }

  public RestResponse submitGetResponse(FormWrapper form) throws UploadProblem.Thrown, ConnectorException {
    @SuppressWarnings("NullableProblems")
    String action = JDOMUtils.getAttributeValue(form.getElement(), "action", null, false);
    if (action == null) {
      LogHelper.error("Submit url not found");
      throw UploadProblem.internalError().toException();
    }
    myLastUrl = HttpUtils.getAbsoluteUrl(action, myLastUrl);
    MultiMap<String,String> parameters = form.getParameters();
    parameters.replaceAll("decorator", "none");
    final List<NameValuePair> parametersList = HttpUtils.convertToNVP(parameters);
    RestSession.Request request = new RestSession.UrlEncodedPost(HtmlWizard.this.myLastUrl, "postForm_" + action, null, parametersList, Collections.singleton(DecoratorNone.INSTANCE));
    request.addRequestHeader("X-Atlassian-Token", "no-check");
    return mySession.perform(request, RequestPolicy.SAFE_TO_RETRY);
  }

  public Element getRootElement() {
    if (myDocument == null) {
      LogHelper.error("No document loaded");
      return null;
    }
    return myDocument.getRootElement();
  }

  public static class DecoratorNone implements RedirectURIHandler {
    public static final DecoratorNone INSTANCE = new DecoratorNone();

    @Nullable
    public URI approveRedirect(HttpMaterial httpMaterial, URI initialUri, URI redirectUri) throws HttpLoaderException {
      return HttpUtils.addGetParameterIfMissing(redirectUri, "decorator", "none");
    }
  }

  public static class NoFormException extends Exception {
    public NoFormException(String message) {
      super(message);
    }
  }
}
