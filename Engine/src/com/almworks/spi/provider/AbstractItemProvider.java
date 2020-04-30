package com.almworks.spi.provider;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ItemProvider;
import com.almworks.api.engine.ItemProviderState;
import com.almworks.api.engine.ProviderDisabledException;
import com.almworks.api.http.HttpUtils;
import com.almworks.api.platform.ProductInformation;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBResult;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.util.SyncAttributes;
import com.almworks.spi.provider.wizard.ConnectionWizard;
import com.almworks.util.GlobalLogPrivacy;
import com.almworks.util.LogPrivacyPolizei;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.threads.CanBlock;
import org.almworks.util.Collections15;
import org.almworks.util.StringUtil;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractItemProvider implements ItemProvider {
  private static String ourUserAgent;

  protected final BasicScalarModel<ItemProviderState> myState = BasicScalarModel.createWithValue(ItemProviderState.NOT_STARTED, true);
  private final Configuration myConnectionConfigs;

  private final Set<String> myEditedConenctions = Collections15.hashSet();

  public AbstractItemProvider(Configuration connectionsConfig) {
    myConnectionConfigs = connectionsConfig;
  }

  @CanBlock
  protected static void setPrimaryTypeFor(final DBItemType dbType, SyncManager syncMan) {
    //noinspection ConstantConditions
    syncMan.writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        drain.changeItem(dbType).setValue(SyncAttributes.IS_PRIMARY_TYPE, true);
      }
      @Override
      public void onFinished(DBResult<?> result) {}
    }).waitForCompletion();
  }

  public ScalarModel<ItemProviderState> getState() throws ProviderDisabledException {
    return myState;
  }

  public Configuration createDefaultConfiguration(String itemUrl) {
    return null;
  }

  public boolean isItemUrl(String url) {
    return false;
  }

  public static String getUserAgent() {
    if (ourUserAgent == null) {
      String userAgent = HttpUtils.getEngineVersion();
      ProductInformation product = Context.get(ProductInformation.class);
      if (product != null) {
        String build = product.getBuildNumber().toDisplayableString();
        userAgent += " (" + product.getName() + "/" + product.getVersion() + "." + build + ")";
      }
      ourUserAgent = userAgent;
    }
    return ourUserAgent;
  }

  public Configuration getConnectionConfig(String connectionID) {
    return myConnectionConfigs.getOrCreateSubset(connectionID);
  }

  static {
    GlobalLogPrivacy.installPolizei(new HTTPAuthorizationPolizei());
  }

  private static class HTTPAuthorizationPolizei implements LogPrivacyPolizei {
    private static final Pattern AUTH_PATTERN = Pattern.compile("(Authorization: \\w+ )([^\\s]+)\\s*");
    private static final Pattern PROXY_AUTH_PATTERN = Pattern.compile("(Proxy-Authorization: \\w+ )([^\\s]+)\\s*");
    private static final Pattern BASIC_AUTH = Pattern.compile("Authorization: Basic ([^\\s]+==)(\\s|$).*");

    @NotNull
    public String examine(@NotNull String message) {
      if (message == null) {
        assert false;
        return message;
      }
      if (!message.startsWith("Authorization: ") && !message.startsWith("Proxy-Authorization: "))
        return message;

      String r = replaceBasicAuth(message);
      if (r != null)
        return r;
      r = repl(message, AUTH_PATTERN);
      if (r != null)
        return r;
      r = repl(message, PROXY_AUTH_PATTERN);
      if (r != null)
        return r;
      return message;
    }

    /**
     * Tries to decode BasicAuth Base64 token and split it into [userName] and password length.
     * @return null if failed to parse, or message like "Authorization: Basic dyoma@almworks.com:24-chars"
     */
    private String replaceBasicAuth(String message) {
      Matcher m = BASIC_AUTH.matcher(message);
      if (!m.matches()) return null;
      String encoded = m.group(1);
      try {
        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
        String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
        int index = decoded.indexOf(':');
        if (index < 0) return null;
        String username = decoded.substring(0, index);
        int passwordLength = decoded.length() - index - 1;
        return String.format("%s%s-chars:%s-chars", message.substring(0, m.start(1)), username.length(), passwordLength);
      } catch (Exception e) {
        return null;
      }
    }

    private String repl(String message, Pattern pattern) {
      Matcher matcher = pattern.matcher(message);
      if (!matcher.matches())
        return null;
      StringBuilder b = new StringBuilder();
      b.append(matcher.group(1));
      b.append(StringUtil.repeatCharacter('x', matcher.group(2).length()));
      return b.toString();
    }
  }

  @Override
  public void showNewConnectionWizard() {
    createNewConnectionWizard().showWizard(null);
  }

  protected abstract ConnectionWizard createNewConnectionWizard();

  @Override
  public void showEditConnectionWizard(Connection connection) {
    synchronized(myEditedConenctions) {
      final String id = connection.getConnectionID();
      if(!myEditedConenctions.contains(id)) {
        final ConnectionWizard wizard = createEditConnectionWizard(connection);
        if(wizard != null) {
          myEditedConenctions.add(id);
          wizard.showWizard(new Detach() {
            @Override
            protected void doDetach() throws Exception {
              synchronized(myEditedConenctions) {
                myEditedConenctions.remove(id);
              }
            }
          });
        }
      }
    }
  }

  protected abstract ConnectionWizard createEditConnectionWizard(Connection connection);

  @Override
  public boolean isEditingConnection(Connection connection) {
    synchronized(myEditedConenctions) {
      return myEditedConenctions.contains(connection.getConnectionID());
    }
  }
}