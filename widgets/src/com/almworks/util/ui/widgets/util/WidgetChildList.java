package com.almworks.util.ui.widgets.util;

import com.almworks.util.ui.widgets.Widget;
import com.almworks.util.ui.widgets.WidgetAttach;
import com.almworks.util.ui.widgets.WidgetHost;
import com.almworks.util.ui.widgets.genutil.Log;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

/**
 * Utility implementation of {@link com.almworks.util.ui.widgets.WidgetAttach} for composite widgets.<br>
 * Tracks child widgets and attached hosts, attaches new and detaches removed children.
 * @param <T>
 */
public class WidgetChildList<T> implements WidgetAttach, Iterable<Widget<? super T>> {
  private static final Log<WidgetChildList> log = Log.get(WidgetChildList.class);
  private final List<Widget<? super T>> myChildren = Collections15.arrayList();
  private final List<WidgetHost> myHosts = Collections15.arrayList();

  @Override
  public void attach(@NotNull WidgetHost host) {
    for (Widget<? super T> child : myChildren) WidgetUtil.attachWidget(child, host);
    myHosts.add(host);
  }

  @Override
  public void detach(@NotNull WidgetHost host) {
    myHosts.remove(host);
    for (Widget<? super T> child : myChildren) WidgetUtil.detachWidget(child, host);
  }

  /**
   * Add new child. The child is attached to all hosts the owning widget is attached
   */
  public void addChild(Widget<? super T> widget) {
    myChildren.add(widget);
    for (WidgetHost host : myHosts) WidgetUtil.attachWidget(widget, host);
  }

  /**
   * Removes child. The child is detached from all hosts where is was attached by this utility
   * @param index index of child widget
   */
  public void removeChild(int index) {
    detachWidget(myChildren.remove(index));
  }

  private void detachWidget(Widget<? super T> widget) {
    for (WidgetHost host : myHosts) WidgetUtil.detachWidget(widget, host);
  }

  public Widget<? super T> get(int index) {
    return myChildren.get(index);
  }

  public int size() {
    return myChildren.size();
  }

  @Override
  public Iterator<Widget<? super T>> iterator() {
    return myChildren.iterator();
  }

  /**
   * Add or replace child widget with another one
   */
  public void setChild(int index, Widget<? super T> child) {
    if (index == myChildren.size()) addChild(child);
    else detachWidget(myChildren.set(index, child));
  }
}
