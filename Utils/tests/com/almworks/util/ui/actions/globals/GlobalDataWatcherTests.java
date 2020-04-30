package com.almworks.util.ui.actions.globals;


import com.almworks.util.collections.Comparing;
import com.almworks.util.tests.GUITestCase;
import com.almworks.util.ui.actions.ConstProvider;
import com.almworks.util.ui.actions.DataProvider;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

/**
 * @author dyoma
 */
public class GlobalDataWatcherTests extends GUITestCase {
  private final MyCallback myCallback;
  private static final TypedKey<Object> ROLE1 = TypedKey.create("role1");
  private static final TypedKey<Object> ROLE2 = TypedKey.create("role2");
  private static final TypedKey<Object> ROLE3 = TypedKey.create("role3");
  private static final String VALUE2 = "value2";
  private static final String VALUE3 = "value3";
  private final GlobalDataWatcher myWatcher;

  public GlobalDataWatcherTests() {
    myCallback = new MyCallback();
    myWatcher = new GlobalDataWatcher(myCallback);
  }

  public void testWatchingRoot() {
    JPanel root = new JPanel();
    JLabel leaf1 = new JLabel();
    root.add(leaf1);
    ConstProvider.addGlobalValue(root, ROLE2, VALUE2);
    ConstProvider.addGlobalValue(leaf1, ROLE3, VALUE3);
    myWatcher.watch(root);
    assertNull(myWatcher.getProvider(ROLE1));
    assertNull(myWatcher.getProvider(ROLE2));
    checkRoleProvider(ROLE3, VALUE3);

    JLabel leaf2 = new JLabel();
    String value1 = "value1";
    ConstProvider.addGlobalValue(leaf2, ROLE1, value1);
    root.add(leaf2);
    checkRoleProvider(ROLE1, value1);

    GlobalData.KEY.removeAll(leaf2);
    myWatcher.reviewDataAt(leaf2);
    assertNull(myWatcher.getProvider(ROLE1));

    GlobalData.KEY.addClientValue(leaf2, ROLE1);
    myWatcher.reviewDataAt(leaf2);
    checkRoleProvider(ROLE1, value1);

    JPanel panel = new JPanel();
    root.remove(leaf2);
    assertNull(myWatcher.getProvider(ROLE1));
    root.add(panel);
    panel.add(leaf2);
    checkRoleProvider(ROLE1, value1);

    root.remove(panel);
    assertNull(myWatcher.getProvider(ROLE1));

    root.add(panel);
    checkRoleProvider(ROLE1, value1);

    root.remove(panel);
    assertNull(myWatcher.getProvider(ROLE1));
    DataRoot.KEY.putClientValue(panel, GlobalDataRoot.TERMINATOR);
    root.add(panel);
    assertNull(myWatcher.getProvider(ROLE1));
    ConstProvider.addGlobalValue(panel, ROLE1, "value1a");
    myWatcher.reviewDataAt(panel);
    checkRoleProvider(ROLE1, "value1a");
  }

  public void testNotifications() {
    JPanel root = new JPanel();
    myWatcher.watch(root);
    JPanel panel = new JPanel();
    root.add(panel);
    JLabel leaf1 = new JLabel();
    panel.add(leaf1);
    JLabel leaf2 = new JLabel();
    root.add(leaf2);
    ConstProvider.addGlobalValue(leaf1, ROLE2, VALUE2);
    myWatcher.reviewDataAt(leaf1);
    myCallback.assertSingleAdded(ROLE2);
    ConstProvider.addGlobalValue(leaf2, ROLE3, VALUE3);
    myWatcher.reviewDataAt(leaf2);
    myCallback.assertSingleAdded(ROLE3);

    root.remove(leaf2);
    panel.remove(leaf1);
    assertNull(myWatcher.getProvider(ROLE2));
    assertNull(myWatcher.getProvider(ROLE3));
    myCallback.assertEmpty();
    root.add(leaf2);
    myCallback.assertSingleAdded(ROLE3);
    panel.add(leaf1);
    myCallback.assertSingleAdded(ROLE2);

    panel.remove(leaf1);
    root.remove(leaf2);
    myWatcher.watch(new JPanel());
    myCallback.assertEmpty();
    panel.add(leaf1);
    root.add(leaf2);
    myCallback.assertEmpty();

    panel.remove(leaf1);
    root.remove(leaf2);
    DataRoot.KEY.putClientValue(root, DataRoot.TERMINATOR);
    myWatcher.watch(root);
    root.add(leaf2);
    panel.add(leaf1);
    myCallback.assertEmpty();
  }

  public void testLoosingData() {
    JPanel root = new JPanel();
    JPanel leaf = new JPanel();
    ConstProvider.addGlobalValue(leaf, ROLE1, VALUE2);
    root.add(leaf);
    myWatcher.watch(root);
    root.remove(leaf);
    myCallback.assertRemoved(ROLE1);
  }

  private void checkRoleProvider(TypedKey<?> role, Object value) {
    DataProvider provider = myWatcher.getProvider(role);
    assertNotNull(provider);
    List<Object> values = provider.getObjectsByRole(role);
    assertNotNull(values);
    assertEquals(1, values.size());
    assertEquals(value, values.get(0));
  }

  private static class MyCallback implements GlobalDataWatcher.WatcherCallback {
    private final Set<TypedKey<?>> myLastAdded = Collections15.hashSet();
    private final Set<TypedKey<?>> myLastRemoved = Collections15.hashSet();

    public void onDataAppears(@NotNull Collection<? extends TypedKey<?>> roles) {
      myLastAdded.addAll(roles);
    }

    public void onDataDisappears(@NotNull Collection<? extends TypedKey<?>> roles) {
      myLastRemoved.addAll(roles);
    }

    public void assertAdded(Collection<? extends TypedKey<?>> roles) {
      assertTrue(Comparing.areSetsEqual(myLastAdded, roles));
      myLastAdded.clear();
    }

    public void assertAdded(TypedKey<?>[] roles) {
      assertAdded(Arrays.asList(roles));
    }

    public void assertSingleAdded(TypedKey<?> role) {
      assertAdded(Collections.singleton(role));
    }

    public void assertEmpty() {
      assertEquals(myLastAdded.toString() , 0, myLastAdded.size());
    }

    public void assertRemoved(TypedKey<?> role) {
      assertTrue(Comparing.areSetsEqual(myLastRemoved, Collections.singleton(role)));
      myLastRemoved.clear();
    }
  }
}
