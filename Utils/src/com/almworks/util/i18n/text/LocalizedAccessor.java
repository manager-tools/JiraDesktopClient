package com.almworks.util.i18n.text;

import com.almworks.util.LogHelper;
import com.almworks.util.commons.Factory;
import org.jetbrains.annotations.NotNull;

import java.text.Format;
import java.text.MessageFormat;
import java.util.Locale;

public abstract class LocalizedAccessor {
  @NotNull
  public abstract String getString(String suffix);

  public abstract Locale getCurrentLocale();

  public final Value getFactory(String suffix) {
    getString(suffix); // ensure full path is known
    return new Value(this, suffix, false);
  }

  public final MessageStr messageStr(String suffix) {
    MessageStrImpl message = new MessageStrImpl(this, suffix);
    message.check();
    return message;
  }

  public final MessageInt messageInt(String suffix) {
    MessageIntImpl message = new MessageIntImpl(this, suffix);
    message.check();
    return message;
  }

  public final Message2 message2(String suffix) {
    Message2 message = new Message2(this, suffix);
    message.check();
    return message;
  }

  public final Message3 message3(String suffix) {
    Message3 message = new Message3(this, suffix);
    message.check();
    return message;
  }

  public final Message4 message4(String suffix) {
    Message4 message = new Message4(this, suffix);
    message.check();
    return message;
  }

  public final MessageIntStr messageIntStr(String suffix) {
    MessageIntStr message = new MessageIntStr(this, suffix);
    message.check();
    return message;
  }

  public LocalizedAccessor sub(String prefix) {
    return new Sub(this, prefix);
  }

  public static class Fixed extends LocalizedAccessor {
    private final Locale myLocale;
    private final I18NAccessor myAccessor;

    public Fixed(I18NAccessor accessor, Locale locale) {
      myAccessor = accessor;
      myLocale = locale;
    }

    @Override
    public String toString() {
      return myAccessor.toString();
    }

    @NotNull
    @Override
    public String getString(String suffix) {
      return myAccessor.getString(suffix, myLocale);
    }

    @Override
    public Locale getCurrentLocale() {
      return myLocale;
    }
  }

  public static class Sub extends LocalizedAccessor {
    private final LocalizedAccessor myParent;
    private final String myPrefix;

    public Sub(LocalizedAccessor parent, String prefix) {
      myParent = parent;
      myPrefix = prefix;
    }

    @NotNull
    @Override
    public String getString(String suffix) {
      return myParent.getString(myPrefix + "." + suffix);
    }

    @Override
    public Locale getCurrentLocale() {
      return myParent.getCurrentLocale();
    }

    @Override
    public String toString() {
      return myParent + "." + myPrefix;
    }
  }

  public class Value implements Factory<String> {
    private final LocalizedAccessor myAccessor;
    private final String mySuffix;
    private final boolean myFormat;

    public Value(LocalizedAccessor accessor, String suffix, boolean format) {
      myAccessor = accessor;
      mySuffix = suffix;
      myFormat = format;
    }

    @Override
    public String create() {
      String value = myAccessor.getString(mySuffix);
      if (myFormat) value = String.format(value, myAccessor.getCurrentLocale());
      return value;
    }
  }

  private static abstract class Message {
    private final LocalizedAccessor myAccessor;
    private final String mySuffix;

    public Message(LocalizedAccessor accessor, String suffix) {
      myAccessor = accessor;
      mySuffix = suffix;
    }

    protected final String _formatMessage(Object ... arguments) {
      String pattern = getPattern();
      if (pattern.isEmpty()) return "";
      return MessageFormat.format(pattern, arguments);
    }

    protected final String getPattern() {
      return myAccessor.getString(mySuffix);
    }

    protected final MessageFormat getFormat() {
      try {
        return new MessageFormat(getPattern());
      } catch (IllegalArgumentException e) {
        LogHelper.error(e, myAccessor.sub(mySuffix));
        throw e;
      }
    }

    abstract void check();

    @Override
    public String toString() {
      return "Message " + mySuffix + " in " + myAccessor;
    }

    protected final void checkArguments(int expectedCount) {
      String pattern = getPattern();
      if (pattern.isEmpty()) {
        LogHelper.error("Missing pattern", this);
        return;
      }
      MessageFormat format = getFormat();
      Format[] formats = format.getFormatsByArgumentIndex();
      LogHelper.assertError(formats.length == expectedCount, this, pattern);
    }
  }

  public static interface MessageStr {
    String formatMessage(String arg);
  }

  private static class MessageStrImpl extends Message implements MessageStr {
    public MessageStrImpl(LocalizedAccessor accessor, String suffix) {
      super(accessor, suffix);
    }

    public String formatMessage(String argument) {
      MessageFormat format = getFormat();
      return format.format(new Object[]{argument});
    }

    @Override
    void check() {
      checkArguments(1);
    }
  }

  public static interface MessageInt {
    String formatMessage(int argument);
  }

  private static class MessageIntImpl extends Message implements MessageInt {
    public MessageIntImpl(LocalizedAccessor accessor, String suffix) {
      super(accessor, suffix);
    }

    public String formatMessage(int argument) {
      MessageFormat format = getFormat();
      return format.format(new Object[]{argument});
    }

    @Override
    void check() {
      checkArguments(1);
    }
  }

  public static class Message2 extends Message {
    public Message2(LocalizedAccessor accessor, String suffix) {
      super(accessor, suffix);
    }

    public String formatMessage(String arg1, String arg2) {
      MessageFormat format = getFormat();
      return format.format(new Object[]{arg1, arg2});
    }

    @Override
    void check() {
      checkArguments(2);
    }
  }

  public static class Message3 extends Message {
    public Message3(LocalizedAccessor accessor, String suffix) {
      super(accessor, suffix);
    }

    public String formatMessage(String arg1, String arg2, String arg3) {
      MessageFormat format = getFormat();
      return format.format(new Object[]{arg1, arg2, arg3});
    }

    @Override
    void check() {
      checkArguments(3);
    }
  }

  public static class Message4 extends Message {
    public Message4(LocalizedAccessor accessor, String suffix) {
      super(accessor, suffix);
    }

    public String formatMessage(String arg1, String arg2, String arg3, String arg4) {
      MessageFormat format = getFormat();
      return format.format(new Object[]{arg1, arg2, arg3, arg4});
    }

    @Override
    void check() {
      checkArguments(4);
    }
  }

  public static class MessageIntStr extends Message {
    public MessageIntStr(LocalizedAccessor accessor, String suffix) {
      super(accessor, suffix);
    }

    public String formatMessage(int intValue, String stringValue) {
      MessageFormat format = getFormat();
      return format.format(new Object[]{intValue, stringValue});
    }

    @Override
    void check() {
      checkArguments(2);
    }

    public MessageInt applyStr(final String strArg) {
      return new MessageInt() {
        @Override
        public String formatMessage(int argument) {
          return MessageIntStr.this.formatMessage(argument, strArg);
        }
      };
    }
  }
}
