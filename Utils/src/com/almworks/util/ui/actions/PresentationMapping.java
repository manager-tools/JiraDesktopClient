package com.almworks.util.ui.actions;

import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.AActionComponent;
import com.almworks.util.ui.EmptyIcon;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Map;

/**
 * @author dyoma
 */
public abstract class PresentationMapping<S> {
  @Nullable
  public abstract S getSwingValue(@NotNull String swingKey, @NotNull Map<PresentationKey<?>, Object> values);

  public static final EmptyIcon EMPTY_ICON = new EmptyIcon(16, 16);
  public static final Single<String, String> GET_NAME = new GetName();
  public static final PresentationMapping<String> GET_NAME_PLUS_DESCRIPTION = new GetNamePlusDescription();
  public static final PresentationMapping<String> GET_SHORT_DESCRIPTION = copyValue(PresentationKey.SHORT_DESCRIPTION);
  public static final Single<Icon, Icon> GET_SMALL_ICON = new GetSmallIcon();
  public static final Single<Integer, String> GET_MNEMONIC = new GetMnemonic();
  public static final PresentationMapping<Boolean> ENABLING = new Enabling();
  public static final PresentationMapping<Boolean> VISIBILITY = new Visibility();
  public static final PresentationMapping<Boolean> ALWAYS_VISIBLE = new AlwaysVisible();
  public static final Single<Boolean, EnableState> VISIBLE_IF_ENABLED =
    new Single<Boolean, EnableState>(PresentationKey.ENABLE) {
      protected Boolean convertValue(String swingKey, EnableState value) {
        assert PresentationKey.ACTION_KEY_VISIBLE.equals(swingKey) : swingKey;
        return value == EnableState.ENABLED;
      }
    };

  public static final Map<String, PresentationMapping<?>> VISIBLE_NONAME;
  static {
    Map<String, PresentationMapping<?>> visibleToolbarButton = Collections15.hashMap();
    visibleToolbarButton.put(PresentationKey.ACTION_KEY_VISIBLE, ALWAYS_VISIBLE);
    visibleToolbarButton.put(Action.NAME, PresentationMapping.<String>constant(""));
    visibleToolbarButton.put(Action.SHORT_DESCRIPTION, GET_NAME);
    VISIBLE_NONAME = Collections.unmodifiableMap(visibleToolbarButton);
  }

  public static final Map<String, PresentationMapping<?>> ENABLED_NONAME_PLUS_DESCRIPTION;
  static {
    Map<String, PresentationMapping<?>> visibleToolbarButton = Collections15.hashMap();
    visibleToolbarButton.put(PresentationKey.ACTION_KEY_VISIBLE, VISIBLE_IF_ENABLED);
    visibleToolbarButton.put(Action.NAME, PresentationMapping.<String>constant(""));
    visibleToolbarButton.put(Action.SHORT_DESCRIPTION, GET_NAME_PLUS_DESCRIPTION);
    ENABLED_NONAME_PLUS_DESCRIPTION = Collections.unmodifiableMap(visibleToolbarButton);
  }

  public static final Map<String, PresentationMapping<?>> NONAME;
  static {
    Map<String, PresentationMapping<?>> visibleToolbarButton = Collections15.hashMap();
    visibleToolbarButton.put(Action.NAME, PresentationMapping.<String>constant(""));
    visibleToolbarButton.put(Action.SHORT_DESCRIPTION, GET_NAME);
    NONAME = Collections.unmodifiableMap(visibleToolbarButton);
  }

  public static final Map<String, PresentationMapping<?>> VISIBLE_EMPTY_ICON;
  static {
    Map<String, PresentationMapping<?>> visibleToolbarButton = Collections15.hashMap();
    visibleToolbarButton.put(PresentationKey.ACTION_KEY_VISIBLE, ALWAYS_VISIBLE);
    visibleToolbarButton.put(Action.SMALL_ICON, PresentationMapping.<Icon>constant(EMPTY_ICON));
    VISIBLE_EMPTY_ICON = Collections.unmodifiableMap(visibleToolbarButton);
  }

  public static final Map<String, PresentationMapping<?>> VISIBLE_NULL_ICON;
  static {
    Map<String, PresentationMapping<?>> visibleToolbarButton = Collections15.hashMap();
    visibleToolbarButton.put(PresentationKey.ACTION_KEY_VISIBLE, ALWAYS_VISIBLE);
    visibleToolbarButton.put(Action.SMALL_ICON, PresentationMapping.<Icon>constant(null));
    VISIBLE_NULL_ICON = Collections.unmodifiableMap(visibleToolbarButton);
  }

  public static final Map<String, PresentationMapping<?>> VISIBLE_ONLY_IF_ENABLED;
  static {
    Map<String, PresentationMapping<?>> map = Collections15.hashMap();
    map.put(PresentationKey.ACTION_KEY_VISIBLE, VISIBLE_IF_ENABLED);
    VISIBLE_ONLY_IF_ENABLED = Collections.unmodifiableMap(map);
  }

  public static final Map<String, PresentationMapping<?>> DEFAULT;
  static {
    Map<String, PresentationMapping<?>> map = Collections15.hashMap();
    map.put(Action.NAME, GET_NAME);
    map.put(Action.SHORT_DESCRIPTION, GET_SHORT_DESCRIPTION);
    map.put(Action.SMALL_ICON, GET_SMALL_ICON);
    DEFAULT = Collections.unmodifiableMap(map);
  }

  public static abstract class Single<S, T> extends PresentationMapping<S> {
    private final PresentationKey<T> myKey;

    public Single(PresentationKey<T> key) {
      myKey = key;
    }

    public S getSwingValue(@NotNull String swingKey, Map<PresentationKey<?>, Object> values) {
      T value = (T) values.get(myKey);
      return value != null ? convertValue(swingKey, value) : null;
    }

    protected abstract S convertValue(@NotNull String swingKey, @NotNull T value);
  }


  public static class Copy<T> extends Single<T, T> {
    public Copy(PresentationKey<T> key) {
      super(key);
    }

    protected T convertValue(String swingKey, T value) {
      return value;
    }
  }

  public static <T> PresentationMapping<T> copyValue(PresentationKey<T> key) {
    return new Copy<T>(key);
  }

  public static <T> PresentationMapping<T> constant(final T value) {
    return new PresentationMapping<T>() {
      public T getSwingValue(String swingKey, Map<PresentationKey<?>, Object> values) {
        return value;
      }
    };
  }


  public static void setupDefaultMapping(Map<String, PresentationMapping<?>> mapping) {
    mapping.put(Action.NAME, GET_NAME);
    mapping.put(Action.MNEMONIC_KEY, (PresentationMapping<?>) GET_MNEMONIC);
    setCopyPresentationMapping(PresentationKey.SHORT_DESCRIPTION, Action.SHORT_DESCRIPTION, mapping);
    setCopyPresentationMapping(PresentationKey.LONG_DESCRIPTION, Action.LONG_DESCRIPTION, mapping);
    mapping.put(Action.SMALL_ICON, GET_SMALL_ICON);
    setCopyPresentationMapping(PresentationKey.SMALL_ICON, Action.SMALL_ICON, mapping);
    setCopyPresentationMapping(PresentationKey.SHORTCUT, Action.ACCELERATOR_KEY, mapping);
    setCopyPresentationMapping(PresentationKey.TOGGLED_ON, PresentationKey.ACTION_KEY_TOGGLED_ON, mapping);
    mapping.put(PresentationKey.ACTION_KEY_VISIBLE, PresentationMapping.VISIBILITY);
    mapping.put(PresentationKey.ACTION_KEY_ENABLE, PresentationMapping.ENABLING);
  }

  public static void clearMnemonic(AActionComponent<?> component) {
    component.setPresentationMapping(Action.MNEMONIC_KEY, constant((int) 0));
  }

  private static void setCopyPresentationMapping(PresentationKey<?> key, String swingValue,
    Map<String, PresentationMapping<?>> mapping)
  {
    mapping.put(swingValue, PresentationMapping.copyValue(key));
  }

  public static void setupNotNullIcon(AActionComponent component) {
    component.setPresentationMapping(Action.SMALL_ICON, new PresentationMapping<Icon>() {
      public Icon getSwingValue(String swingKey, Map<PresentationKey<?>, Object> values) {
        return null;
      }
    });
  }

  private static class GetName extends Single<String, String> {
    public GetName() {
      super(PresentationKey.NAME);
    }

    protected String convertValue(String swingKey, String value) {
      return NameMnemonic.parseString(value).getText();
    }
  }

  private static class GetNamePlusDescription extends PresentationMapping<String> {
    public String getSwingValue(@NotNull String swingKey, @NotNull Map<PresentationKey<?>, Object> values) {
      String name = PresentationKey.NAME.getFrom(values);
      if (name != null) name = NameMnemonic.parseString(name).getText();
      String desc = PresentationKey.SHORT_DESCRIPTION.getFrom(values);
      if (name == null) return desc;
      if (desc == null) return name;
      return name + " \u2014 " + desc;
    }
  }


  private static class GetSmallIcon extends Single<Icon, Icon> {
    public GetSmallIcon() {
      super(PresentationKey.SMALL_ICON);
    }

    protected Icon convertValue(String swingKey, Icon value) {
      return value != null ? value : EMPTY_ICON;
    }
  }


  private static class GetMnemonic extends Single<Integer, String> {
    public GetMnemonic() {
      super(PresentationKey.NAME);
    }

    protected Integer convertValue(String swingKey, String value) {
      return NameMnemonic.parseString(value).getMnemonicCharInteger();
    }
  }


  private static class Enabling extends Single<Boolean, EnableState> {
    public Enabling() {
      super(PresentationKey.ENABLE);
    }

    protected Boolean convertValue(@NotNull String swingKey, EnableState value) {
      return EnableState.ENABLED == value;
    }
  }


  private static class Visibility extends Single<Boolean, EnableState> {
    public Visibility() {
      super(PresentationKey.ENABLE);
    }

    protected Boolean convertValue(String swingKey, EnableState value) {
      assert PresentationKey.ACTION_KEY_VISIBLE.equals(swingKey) : swingKey;
      return EnableState.ENABLED == value || EnableState.DISABLED == value;
    }
  }


  private static class AlwaysVisible extends PresentationMapping<Boolean> {
    public Boolean getSwingValue(String swingKey, Map<PresentationKey<?>, Object> values) {
      assert PresentationKey.ACTION_KEY_VISIBLE.equals(swingKey);
      Boolean notAvaliable = PresentationKey.NOT_AVALIABLE.getFrom(values);
      return notAvaliable == null || !notAvaliable;
    }
  }
}
