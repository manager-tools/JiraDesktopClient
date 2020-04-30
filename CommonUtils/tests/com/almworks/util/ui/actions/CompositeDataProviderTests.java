package com.almworks.util.ui.actions;

import com.almworks.util.tests.GUITestCase;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.util.List;

/**
 * @author dyoma
 */
public class CompositeDataProviderTests extends GUITestCase {
  private static final TypedKey<Object> ROLE = TypedKey.create("role");
  private static final String VALUE = "value";

  public void testDataAndComponent() {
    CompositeDataProvider composite = new CompositeDataProvider();
    JPanel compositeLocation = new JPanel();
    assertNull(composite.getObjectsByRole(ROLE));
    assertNull(composite.getSourceComponent(ROLE, compositeLocation));
    JLabel component = new JLabel();
    MockProvider provider = new MockProvider(ROLE, component);
    composite.addProvider(provider);
    provider.setValue(VALUE);
    List<Object> values = composite.getObjectsByRole(ROLE);
    assertNotNull(values);
    assertSame(VALUE, values.get(0));
    assertSame(component, composite.getSourceComponent(ROLE, component));
  }

  public void testConstProvider() {
    JLabel component = new JLabel();
    DataProvider.DATA_PROVIDER.putClientValue(component, new MockProvider(TypedKey.create("k2")));
    ConstProvider.addRoleValue(component, ROLE, VALUE);
    assertSame(component, DataProvider.DATA_PROVIDER.getClientValue(component).getSourceComponent(ROLE, component));
  }
}
