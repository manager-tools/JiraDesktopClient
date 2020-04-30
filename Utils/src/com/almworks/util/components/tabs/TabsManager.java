package com.almworks.util.components.tabs;

import com.almworks.util.collections.ChangeListener1;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadSafe;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataProvider;
import com.almworks.util.ui.actions.DataRole;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author dyoma
 */
public class TabsManager implements UIComponentWrapper2 {
  public static final DataRole<TabsManager> ROLE = DataRole.createRole(TabsManager.class);
  static final ComponentProperty<ContentTab> TAB = ComponentProperty.createProperty("tab");

  private final JPanel myPanel = new JPanel(new SingleChildLayout(SingleChildLayout.CONTAINER));
  private final List<ContentTab> myTabs = Collections15.arrayList();
  private final FireEventSupport<ChangeListener1> myListeners = FireEventSupport.create(ChangeListener1.class);
  private final SimpleModifiable myModifiable = new SimpleModifiable();
  @Nullable
  private UIComponentWrapper myDefaultComponent = null;
  private final ChangeListener myChangeListener = new ChangeListener() {
    public void stateChanged(ChangeEvent e) {
      tabSelectionChanged(getSelectedTab());
    }
  };

  public TabsManager() {
    DataProvider.DATA_PROVIDER.putClientValue(myPanel, new TabDataProvider(this));
    ConstProvider.addRoleValue(myPanel, ROLE, this);
    TabForwardProvider.install(this);
  }

  public ContentTab createTab() {
    ContentTab result = new ContentTab(this);
    myTabs.add(result);
    return result;
  }

  public ContentTab createTab(String name) {
    ContentTab tab = createTab();
    tab.setName(name);
    return tab;
  }

  void deleteTab(ContentTab tab) {
    assert !isInserted(tab);
    myTabs.remove(tab);
  }

  @Nullable
  public ContentTab getSelectedTab() {
    if (myPanel.getComponentCount() == 0)
      return null;
    JTabbedPane tabbedPane = getTabbedPane();
    if (tabbedPane == null) {
      Component component = myPanel.getComponent(0);
      if (myDefaultComponent != null && component == myDefaultComponent.getComponent())
        return null;
      assert component instanceof JComponent : component;
      ContentTab tab = TAB.getClientValue((JComponent) component);
      assert tab != null : component;
      return tab;
    }
    Component selection = tabbedPane.getSelectedComponent();
    if (selection == null)
      return null;
    ContentTab tab = TAB.getClientValue((JComponent) selection);
    assert tab != null : selection;
    return tab;
  }

  public int getSelectedTabIndex() {
    if (myPanel.getComponentCount() == 0)
      return -1;
    JTabbedPane tabbedPane = getTabbedPane();
    if (tabbedPane == null) {
      Component component = myPanel.getComponent(0);
      if (myDefaultComponent != null && component == myDefaultComponent.getComponent())
        return -1;
      assert component instanceof JComponent : component;
      assert TAB.getClientValue((JComponent) component) != null : component;
      return 0;
    }
    return tabbedPane.getSelectedIndex();
  }

  @Nullable
  public JComponent getSelectedComponent() {
    ContentTab tab = getSelectedTab();
    return tab != null ? tab.getJComponent() : null;
  }

  @NotNull
  @ThreadSafe
  public Modifiable getModifiable() {
    return myModifiable;
  }

  public JComponent getComponent() {
    return myPanel;
  }

  public void dispose() {
    myPanel.removeAll();
    if (myDefaultComponent != null)
      myDefaultComponent.dispose();
    myDefaultComponent = null;
    for (ContentTab tab : myTabs) {
      UIComponentWrapper wrapper = tab.getComponent();
      if (wrapper != null)
        clearAccessibleParent(wrapper.getComponent());
      tab.disposeComponent();
    }
  }

  public Detach getDetach() {
    return new Disposer(this);
  }

  void removeComponent(ContentTab tab) {
    int index = getExistingIndex(tab);
    JTabbedPane tabbedPane = getTabbedPane();
    if (tabbedPane == null) {
      assert index == 0 : index;
      myPanel.remove(0);
      tabSelectionChanged(null);
    } else {
      UIComponentWrapper wrapper = tab.getComponent();
      if (wrapper != null) {
        JComponent component = wrapper.getComponent();
        clearAccessibleParent(component);
      }
      tabbedPane.removeTabAt(index);
      int tabsCount = tabbedPane.getTabCount();
      if (tabsCount > 1 || (myDefaultComponent != null && tabsCount == 1))
        return;
      Component lastComponent;
      ContentTab selectedTab;
      if (tabsCount == 1) {
        lastComponent = tabbedPane.getComponentAt(0);
        tabbedPane.removeTabAt(0);
        clearAccessibleParent(lastComponent);
        tabbedPane.setSelectedIndex(-1);
        selectedTab = TAB.getClientValue((JComponent) lastComponent);
      } else {
        assert myDefaultComponent != null;
        lastComponent = myDefaultComponent.getComponent();
        selectedTab = null;
      }
      myPanel.remove(tabbedPane);
      myPanel.add(lastComponent);
      myPanel.revalidate();
      myPanel.repaint();
      tabSelectionChanged(selectedTab);
    }
  }

  private void clearAccessibleParent(@Nullable Component component) {
    if (component instanceof Accessible) {
      component.getAccessibleContext().setAccessibleParent(null);
    }
  }

  private int getIndex(ContentTab tab) {
    int index = 0;
    for (ContentTab contentTab : myTabs) {
      if (contentTab == tab) {
        return index;
      }
      if (contentTab.isShowing())
        index++;
    }
    return -1;
  }

  private int getExistingIndex(ContentTab tab) {
    assert myTabs.contains(tab) : tab;
    assert tab.isShowing() : tab;
    assert isInserted(tab);
    return getIndex(tab);
  }

  private int getInsertionIndex(ContentTab tab) {
    assert myTabs.contains(tab) : tab;
    assert tab.isShowing() : tab;
    assert !isInserted(tab) : tab;
    return getIndex(tab);
  }

  private boolean isInserted(ContentTab tab) {
    if (myPanel.getComponentCount() == 0)
      return false;
    if (tab.getComponent() == null)
      return false;
    Component tabContent = tab.getJComponent();
    JTabbedPane tabbedPane = getTabbedPane();
    if (tabbedPane == null)
      return tabContent == myPanel.getComponent(0);
    for (int i = 0; i < tabbedPane.getTabCount(); i++)
      if (tabbedPane.getComponentAt(i) == tabContent)
        return true;
    return false;
  }

  void insertTab(ContentTab tab) {
    int index = getInsertionIndex(tab);
    if (myPanel.getComponentCount() == 0) {
      assert index == 0 : index;
      myPanel.add(tab.getJComponent());
      myPanel.revalidate();
      myPanel.repaint();
      tabSelectionChanged(tab);
      return;
    }
    JTabbedPane tabbedPane = getTabbedPane();
    if (tabbedPane != null) {
      insertTab(tabbedPane, tab, index);
      return;
    }
    ContentTab firstTab = TAB.getClientValue(Util.cast(JComponent.class, myPanel.getComponent(0)));
    myPanel.remove(0);
    tabbedPane = createTabbedPane();
    myPanel.add(tabbedPane);
    myPanel.revalidate();
    myPanel.repaint();
    if (firstTab != null)
      insertTab(tabbedPane, firstTab, 0);
    insertTab(tabbedPane, tab, index);
  }

  private JTabbedPane createTabbedPane() {
    JTabbedPane tabbedPane;
    tabbedPane = UIUtil.newJTabbedPane();
    tabbedPane.addChangeListener(myChangeListener);
//    tabbedPane.addMouseListener(new BasePopupHandler() {
//      public void mouseReleased(MouseEvent e) {
//        if (e.getButton() == MouseEvent.BUTTON2) {
//          ContentTab tab = findTab(e);
//          if (tab != null) {
//            tab.delete();
//            return;
//          }
//        }
//        super.mouseReleased(e);
//      }
//
//      protected void showPopupMenu(MouseEvent e, Component source) {
//        ContentTab tab = findTab(e);
//        if (tab == null)
//          return;
//        tab.showPopup(e);
//      }
//
//      @Nullable
//      private ContentTab findTab(MouseEvent e) {
//        Component source = (Component) e.getSource();
//        assert source instanceof JTabbedPane;
//        JTabbedPane tabbedPane = (JTabbedPane) source;
//        int index = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
//        return index >= 0 ? TAB.getClientValue((JComponent) tabbedPane.getComponentAt(index)) : null;
//      }
//    });

    Aqua.makeBorderlessTabbedPane(tabbedPane);
    Aero.makeBorderlessTabbedPane(tabbedPane);

    return tabbedPane;
  }

  private void tabSelectionChanged(@Nullable ContentTab tab) {
    myListeners.getDispatcher().onChange(tab);
    myModifiable.fireChanged();
  }

  private static void insertTab(JTabbedPane tabbedPane, ContentTab tab, int index) {
    tabbedPane.insertTab(tab.getName(), null, tab.getJComponent(), tab.getTooltip(), index);
    JComponent tabComponent = Aqua.isAqua() ? new MacTabComponent(tab) : new TabComponent(tab);
    tabbedPane.setTabComponentAt(index, tabComponent);
    try {
      tabbedPane.setSelectedIndex(index);
    } catch (ArrayIndexOutOfBoundsException e) {
      if (!Aqua.isAqua()) {
        Log.debug("mac problem", e);
      } else {
        throw e;
      }
    }
  }

  void updateTab(ContentTab tab) {
    JTabbedPane tabbedPane = getTabbedPane();
    if (tabbedPane == null)
      return;
    int index = getExistingIndex(tab);
    tabbedPane.setTitleAt(index, tab.getName());
    tabbedPane.setToolTipTextAt(index, tab.getTooltip());
  }

  public Detach addSelectionListener(ChangeListener1<ContentTab> listener, ThreadGate gate) {
    return myListeners.addListener(gate, listener);
  }

  @Nullable
  private JTabbedPane getTabbedPane() {
    assert myPanel.getComponentCount() == 1;
    Component component = myPanel.getComponent(0);
    return Util.castNullable(JTabbedPane.class, component);
  }

  @NotNull
  public List<ContentTab> getTabs() {
    return Collections.unmodifiableList(myTabs);
  }

  @NotNull
  public Iterator<ContentTab> getShownTabs() {
    return ContentTab.SHOWN.filterIterator(getTabs().iterator());
  }

  public int getTabsCount() {
    return myTabs.size();
  }

  public void selectTab(ContentTab tab) {
    if (!tab.isShowing())
      return;
    JTabbedPane tabbedPane = getTabbedPane();
    if (tabbedPane == null)
      return;
    JComponent component = tab.getJComponent();
    int index = tabbedPane.indexOfComponent(component);
    if (index == -1) {
      assert false : tab;
      Log.warn("Missing tab: " + tab);
      return;
    }
    tabbedPane.setSelectedIndex(index);
  }

  public void selectTabIndex(int index) {
    if (index < 0 || index >= getTabsCount()) {
      assert false : index + " count: " + getTabsCount();
      return;
    }
    JTabbedPane tabbedPane = getTabbedPane();
    if (tabbedPane == null) {
      assert index == 0 : index;
      return;
    }
    tabbedPane.setSelectedIndex(index);
  }

  public void setDefaultComponent(@Nullable JComponent component) {
    setDefaultComponent(component != null ? new Simple(component) : null);
  }

  public void setDefaultComponent(@Nullable UIComponentWrapper component) {
    boolean insert = false;
    if (myDefaultComponent != null) {
      assert myPanel.getComponentCount() == 1 : myPanel.getComponentCount();
      UIComponentWrapper defaultComponent = myDefaultComponent;
      if (defaultComponent != null && myPanel.getComponent(0) == defaultComponent.getComponent()) {
        myPanel.remove(0);
        insert = true;
      }
    } else
      insert = myPanel.getComponentCount() == 0;
    myDefaultComponent = component;
    if (insert && component != null) {
      myPanel.add(component.getComponent());
      myPanel.revalidate();
      myPanel.repaint();
    }
  }

  public void requestFocusInWindow() {
    int size = myPanel.getComponentCount();
    if (size == 0)
      myPanel.requestFocusInWindow();
    else
      myPanel.getComponent(0).requestFocusInWindow();
  }
}
