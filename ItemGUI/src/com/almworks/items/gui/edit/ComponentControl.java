package com.almworks.items.gui.edit;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * Represents a editor component allows get properties and control.
 */
public interface ComponentControl {
  Convertor<ComponentControl, JComponent> GET_COMPONENT = new Convertor<ComponentControl, JComponent>() {
    @Override
    public JComponent convert(ComponentControl value) {
      return value != null ? value.getComponent() : null;
    }
  };

  /**
   * @return the component to be placed in Swing tree
   */
  @NotNull
  JComponent getComponent();

  /**
   * @return layout preferences
   */
  @NotNull
  Dimensions getDimension();

  /**
   * @return enabled state of the component. Controls the default state and possible user actions.<br>
   * Disabled component does not actually edit anything.
   */
  @NotNull
  Enabled getEnabled();

  /**
   * Makes the component enabled or disabled.
   * @param enabled true to enable component
   */
  void setEnabled(boolean enabled);

  NameMnemonic getLabel();

  public enum Dimensions {
    /**
     * The component is constant height
     */
    SINGLE_LINE,
    /**
     * The component is variable height
     */
    TALL,
    /**
     * The component is variable height and requires more horizontal space
     */
    WIDE,
    /**
     * The component is fixed height and requires more horizontal space
     */
    WIDE_LINE;

    public boolean isFixedHeight() {
      return this == SINGLE_LINE || this == WIDE_LINE;
    }

    public boolean isWide() {
      return this == WIDE || this == WIDE_LINE;
    }
  }

  /**
   * Enabled state of component
   */
  public enum Enabled {
    /**
     * The component enabled state cannot be controlled by user
     */
    NOT_APPLICABLE,
    /**
     * For the particular use the component cannot be made disabled (because of it edits mandatory value)
     */
    ALWAYS_ENABLED,
    /**
     * By default the component is enabled but user may disable it
     */
    ENABLED,
    /**
     * By default the component is disabled but user may enable it.
     */
    DISABLED;

    public boolean isEnabled() {
      return this == ALWAYS_ENABLED || this == ENABLED;
    }

    public boolean isDisabled() {
      return this == DISABLED;
    }
  }

  public class EnableWrapper implements ComponentControl {
    private final ComponentControl myControl;
    private final boolean myEnabled;

    public EnableWrapper(ComponentControl control, boolean enabled) {
      myControl = control;
      myEnabled = enabled;
    }

    @NotNull
    @Override
    public JComponent getComponent() {
      return myControl.getComponent();
    }

    @NotNull
    @Override
    public Dimensions getDimension() {
      return myControl.getDimension();
    }

    @NotNull
    @Override
    public Enabled getEnabled() {
      return myEnabled ? Enabled.ALWAYS_ENABLED : Enabled.NOT_APPLICABLE;
    }

    @Override
    public void setEnabled(boolean enabled) {
      LogHelper.assertError(enabled == myEnabled, "Cannot control enable state", myEnabled, enabled, myControl.getLabel());
    }

    @Override
    public NameMnemonic getLabel() {
      return myControl.getLabel();
    }

    public static List<ComponentControl> disableAll(List<? extends ComponentControl> components) {
      return wrapAll(false, components);
    }

    public static List<ComponentControl> wrapAll(boolean enable, List<? extends ComponentControl> components) {
      List<ComponentControl> result = Collections15.arrayList(components.size());
      for (ComponentControl component : components) {
        EnableWrapper wrapper = Util.castNullable(EnableWrapper.class, component);
        if (wrapper != null) LogHelper.assertError(wrapper.myEnabled == enable, "Component already wrapped", enable);
        else wrapper = wrap(enable, component);
        result.add(wrapper);
      }
      return result;
    }

    public static EnableWrapper wrap(boolean enable, ComponentControl component) {
      component.setEnabled(enable);
      return new EnableWrapper(component, enable);
    }
  }
}
