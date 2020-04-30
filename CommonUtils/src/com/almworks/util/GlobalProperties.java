package com.almworks.util;

/**
 * This class contains universal environment properties that could be passed to Deskzilla
 * or other util-based application.
 */
public class GlobalProperties extends PropertiesDictionary {
  public static final String RUN_FROM_IDE = register("from.ide");
  public static final String IS_DEBUGGING = register("is.debugging");
  public static final String NO_WINLAF = register("no.winlaf");
  public static final String USE_WINLAF = register("use.winlaf");
  public static final String USE_METAL = register("use.metal");
  public static final String SINGLE_WORKER = register("single.worker");
  public static final String SOCKET_TIMEOUT = register("socket.timeout");
  public static final String INTERNAL_ACTIONS = register("i.a");
  public static final String DEBUG_THREADS = register("debug.threads");
  public static final String DEBUG_HTTPCLIENT = register("debug.httpclient");
  public static final String DEBUG_PROCESSING_LOCKS = register("debug.plock");
  public static final String STRICT_COOKIE_SECURITY = register("strict.cookie.security");
  public static final String DISABLE_HTTP_COMPRESSION = register("disable.http.compression");
  public static final String SHOW_MEM = register("show.mem");
  public static final String STRICT_REALM_MATCHING = register("strict.realm.matching");
  public static final String NO_PREEMPTIVE_AUTH = register("no.preemptive.auth");

  // jira client 1.4.1 hf2x
  public static final String DISABLE_HTTP_JRE_EXECUTOR = register("disable.http.jre.executor");
  public static final String FORCE_HTTP_JRE_EXECUTOR = register("force.http.jre.executor");
  public static final String HTTP_AUTH_SCHEME_PRIORITY = register("http.auth.scheme.priority");
  public static final String ALLOW_ICON_AUTH = register("allow.icon.auth");

  // hack for friends
  public static final String HACK_DISABLE_CLIPBOARD_IN_POPUPS = register("disable.clipboard.in.popups");

  // jira client 2.1
  public static final String FORCE_HTTP_USER_AGENT = register("force.http.user.agent");

  // jira client 2.3.5
  /**
   * Override charset provided by HTTP response.<br>
   * Not null valus forces {@link com.almworks.http.HttpLoaderImpl} to use own charset instead of provided by server.
   * The setting can be used to configure default overriding chartset: by default setup HttpLoader charset.
   */
  public static final String FORCE_OVERRIDE_CHARSET = register("force.override.charset");

  // jira client 3.8
  public static final String WEB_VIEW_USER_AGENT = register("webview.user.agent");

  private GlobalProperties() {
  }
}
