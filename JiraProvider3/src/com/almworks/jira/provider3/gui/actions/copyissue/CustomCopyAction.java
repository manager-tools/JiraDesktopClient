package com.almworks.jira.provider3.gui.actions.copyissue;

import com.almworks.actions.CopyIdAndSummaryAction;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Connection;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.BranchSource;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Function2;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomCopyAction extends SimpleAction {
  private static final String CUSTOM_COPY_PATTERN = "action.customcopy.pattern";
  private static final String CUSTOM_COPY_NAME = "action.customcopy.name";

  public CustomCopyAction() {
    super("Copy Issue Fields");
    watchRole(ItemWrapper.ITEM_WRAPPER);
  }

  @Override
  protected void customUpdate(UpdateContext context) throws CantPerformException {
    context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, true);
    context.setEnabled(EnableState.INVISIBLE);
    context.putPresentationProperty(PresentationKey.NAME, "");
    String pattern = Env.getString(CUSTOM_COPY_PATTERN);
    if (pattern == null || pattern.trim().isEmpty()) return;
    String name = Util.NN(Env.getString(CUSTOM_COPY_NAME)).trim();
    if (name.isEmpty()) name = "Copy Issue Fields";
    context.putPresentationProperty(PresentationKey.NOT_AVALIABLE, false);
    context.putPresentationProperty(PresentationKey.ENABLE, EnableState.DISABLED);
    context.putPresentationProperty(PresentationKey.NAME, name);
    List<ItemWrapper> wrappers = context.getSourceCollection(ItemWrapper.ITEM_WRAPPER);
    if (!wrappers.isEmpty()) context.setEnabled(true);
  }

  @Override
  protected void doPerform(ActionContext context) throws CantPerformException {
    String pattern = Env.getString(CUSTOM_COPY_PATTERN);
    if (pattern == null || pattern.trim().isEmpty()) return;
    List<ItemWrapper> wrappers = CantPerformException.ensureNotEmpty(context.getSourceCollection(ItemWrapper.ITEM_WRAPPER));
    final Map<Connection,LongList> issues = CopyIdAndSummaryAction.getItemsGroupedByConnection(context);
    final List<Function2<ItemVersion, Connection, String>> copyProcedure = parse(pattern);
    context.getSourceObject(SyncManager.ROLE).enquireRead(DBPriority.FOREGROUND, new ReadTransaction<String>() {
      @Override
      public String transaction(DBReader reader) throws DBOperationCancelledException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Connection, LongList> entry : issues.entrySet()) {
          Connection connection = entry.getKey();
          for (ItemVersion issue : BranchSource.trunk(reader).readItems(entry.getValue())) {
            if (builder.length() > 0) builder.append("\n");
            for (Function2<ItemVersion, Connection, String> function : copyProcedure) {
              String str = function.invoke(issue, connection);
              if (str != null) builder.append(str);
            }
          }
        }
        return builder.toString();
      }
    }).onSuccess(ThreadGate.AWT, new Procedure<String>() {
      public void invoke(String arg) {
        if (arg.length() == 0)
          return;
        UIUtil.copyToClipboard(arg);
        Toolkit.getDefaultToolkit().beep();
      }
    });
  }

  private static List<Function2<ItemVersion, Connection, String>> parse(String pattern) {
    StringBuilder message = new StringBuilder();
    pattern = processEscape(pattern, message);
    List<Function2<ItemVersion, Connection, String>> result = processFields(pattern, message);
    if (message.length() > 0) LogHelper.warning("Wrong custom copy pattern", message);
    return result;
  }

  private static final Pattern FIELD_REF = Pattern.compile("\\$([^$]*)\\$");
  private static final RawString DOLLAR = new RawString("$");
  private static List<Function2<ItemVersion, Connection, String>> processFields(String pattern, StringBuilder message) {
    Matcher matcher = FIELD_REF.matcher(pattern);
    int start = 0;
    ArrayList<Function2<ItemVersion, Connection, String>> result = Collections15.arrayList();
    while (matcher.find(start)) {
      if (start < matcher.start()) result.add(new RawString(pattern.substring(start, matcher.start())));
      String reference = matcher.group(1);
      Function2<ItemVersion, Connection, String> function;
      if (reference.isEmpty()) function = DOLLAR;
      else function = getFieldFunction(Util.lower(reference));
      if (function == null) {
        if (message.length() > 0) message.append("\n");
        message.append("Unknown reference '").append(reference).append("' at ").append(matcher.start());
        function = new RawString(matcher.group());
      }
      result.add(function);
      start = matcher.end();
    }
    if (start < pattern.length()) result.add(new RawString(pattern.substring(start)));
    return result;
  }

  private static final Map<String, Function2<ItemVersion, Connection, String>> STATIC_FIELDS;
  static {
    HashMap<String,Function2<ItemVersion, Connection, String>> map = Collections15.hashMap();
    map.put("summary", new StaticField(Issue.SUMMARY));
    map.put("key", new StaticField(Issue.KEY));
    map.put("url", new Function2<ItemVersion, Connection, String>() {
      @Override
      public String invoke(ItemVersion itemVersion, Connection connection) {
        return connection.getItemUrl(itemVersion);
      }
    });
    STATIC_FIELDS = Collections.unmodifiableMap(map);
  }
  private static Function2<ItemVersion, Connection, String> getFieldFunction(String id) {
    return STATIC_FIELDS.get(id);
  }

  private static final char[] ESCAPED = new char[]{'\\', 'n', 't'};
  private static final char[] REPLACEMENT = new char[] {'\\', '\n', '\t'};
  private static String processEscape(String pattern, StringBuilder message) {
    boolean escaped = false;
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < pattern.length(); i++) {
      char c = pattern.charAt(i);
      if (escaped) {
        int index = ArrayUtil.indexOf(ESCAPED, c);
        if (index < 0) {
          result.append('\\').append('c');
          if (message.length() > 0) message.append("\n");
          message.append("Unknown escape char '").append(c).append("' at ").append(i);
        } else result.append(REPLACEMENT[index]);
        escaped = false;
      } else  if (c == '\\') escaped = true;
      else result.append(c);
    }
    if (escaped) {
      if (message.length() > 0) message.append("\n");
      message.append("Missing escape char at end");
      result.append("\\");
    }
    return result.toString();
  }

  private static class StaticField implements Function2<ItemVersion, Connection, String> {
    private final DBAttribute<String> myAttribute;

    private StaticField(DBAttribute<String> attribute) {
      myAttribute = attribute;
    }

    @Override
    public String invoke(ItemVersion issue, Connection connection) {
      return issue.getValue(myAttribute);
    }
  }

  private static class RawString implements Function2<ItemVersion, Connection, String> {
    private final String myString;

    private RawString(String string) {
      myString = string;
    }

    @Override
    public String invoke(ItemVersion itemVersion, Connection connection) {
      return myString;
    }
  }
}
