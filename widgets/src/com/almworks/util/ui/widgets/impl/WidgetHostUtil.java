package com.almworks.util.ui.widgets.impl;

import com.almworks.util.LogHelper;
import com.almworks.util.components.ScrollableAware;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.widgets.Widget;
import org.almworks.util.TypedKey;

import javax.swing.*;

public class WidgetHostUtil<T> {
  private final WidgetHostComponent myHost = new WidgetHostComponent();
  private final HostComponentState<T> myState;

  public WidgetHostUtil() {
    myState = myHost.createState();
    myHost.setState(myState);
  }

  public static <T> WidgetHostUtil<T> create() {
    return new WidgetHostUtil<T>();
  }

  public static <T> WidgetHostUtil<T> create(Widget<T> root) {
    WidgetHostUtil<T> util = create();
    util.myState.setWidget(root);
    return util;
  }

  public static <T> WidgetHostUtil<T> create(Widget<T> root, T value) {
    WidgetHostUtil<T> util = create(root);
    util.setValue(value);
    return util;
  }

  public WidgetHostComponent getComponent() {
    return myHost;
  }

  public HostComponentState<T> getState() {
    return myState;
  }

  public void setValue(T value) {
    myState.setValue(value);
  }

  public JScrollPane wrapWithScrollPane() {
    LogHelper.assertError(myHost.getParent() == null, "Already has parent", myHost.getParent());
    ScrollableAware.COMPONENT_PROPERTY.putClientValue(myHost, ScrollableAware.FILL_HEIGHT);
    myHost.setListenMouseWheel(false);
    final ScrollablePanel scrollable = new ScrollablePanel(myHost);
    scrollable.setBackground(DocumentFormAugmentor.backgroundColor());
    return new JScrollPane(scrollable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

  }

  public <T> void putWidgetData(TypedKey<T> key, T value) {
    myState.putWidgetData(key, value);
  }
}
