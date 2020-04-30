package com.almworks.util.components.completion;

import com.almworks.util.Env;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Equality;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Factory1;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import com.almworks.util.tests.KeyboardInput;
import org.almworks.util.Collections15;
import util.concurrent.SynchronizedBoolean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author dyoma
 */
public class CompletingComboBoxControllerTests extends BaseTestCase {
  private final CompletingComboBoxController<String> myController = new CompletingComboBoxController<String>();
  private final JComboBox myCombo = (JComboBox) myController.getComponent();
  private final KeyboardInput myKeyboard = new KeyboardInput(myCombo);
  private JFrame myFrame;
  private JTextField myEditor;
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  protected void setUp() throws Exception {
    super.setUp();
    if (GraphicsEnvironment.isHeadless())
      return;
    myFrame = new JFrame();
    final SynchronizedBoolean focusGained = new SynchronizedBoolean(false);
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        myController.setFilterFactory(new Factory1<Condition<String>, String>() {
          public Condition<String> create(final String argument) {
            return new Condition<String>() {
              public boolean isAccepted(String value) {
                return value.indexOf(argument) != -1;
              }
            };
          }
        });
        //noinspection unchecked
        myController.setConvertors(new Convertor<String, String>() {
                                     public String convert(String value) {
                                       return "!" + value;
                                     }
                                   }, new Convertor<String, String>() {
                                     public String convert(String value) {
                                       //noinspection ConstantConditions
                                       return value != null && value.startsWith("!") ? value.substring(1) : value;
                                     }
                                   }, Equality.GENERAL
        );
        myController.setModel(
          SelectionInListModel.create(Arrays.asList("123", "234", "345", "456", "345a", "345ab", "345b"), null));
        myEditor = (JTextField) myCombo.getEditor().getEditorComponent();
        myEditor.addFocusListener(new FocusAdapter() {
          public void focusGained(FocusEvent e) {
            focusGained.set(true);
            assertTrue(myEditor.hasFocus());
          }
        });
        myFrame.getContentPane().add(myCombo, BorderLayout.CENTER);
        final JLabel label = new JLabel("xx");
        myCombo.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            label.setText(String.valueOf(myCombo.getSelectedItem()));
          }
        });
        myFrame.getContentPane().add(label, BorderLayout.NORTH);
        myFrame.pack();
        myFrame.setVisible(true);
        myCombo.requestFocusInWindow();
      }
    });
    GUITestCase.flushAWTQueue();
    focusGained.waitForValue(true);
    assertTrue(myEditor.hasFocus());
  }

  protected void tearDown() throws Exception {
    if (!GraphicsEnvironment.isHeadless())
      myFrame.dispose();
    super.tearDown();
  }
  
  public void testNoSelection() throws InterruptedException, InvocationTargetException {
    if (GraphicsEnvironment.isHeadless())
      return;
    assertTrue(myEditor.hasFocus());
    assertFalse(myCombo.isPopupVisible());
    myKeyboard.sendKey_Down();
    assertTrue(myCombo.isPopupVisible());

    ComboBoxModel model = myCombo.getModel();
    CHECK.size(7, toList(model));
    myKeyboard.typeKey(KeyEvent.VK_3, '3');
    assertEquals("3", myEditor.getText());
    CHECK.size(6, toList(model));
    if (!Env.isMac()) {
      myKeyboard.sendKey_Down();
      assertEquals("!123", myEditor.getText());

      myKeyboard.sendKey_Down();
      myKeyboard.sendKey_Down();
      assertEquals("!345", myEditor.getText());
    }
  }

  public void testExistingSelection() throws InvocationTargetException, InterruptedException {
    if (GraphicsEnvironment.isHeadless())
      return;
    ComboBoxModel model = myCombo.getModel();
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        String item = myController.getModel().getAt(2);
        myController.setSelectedItem(item);
      }
    });
    GUITestCase.flushAWTQueue();
    assertEquals("!345", myEditor.getText());

    myKeyboard.sendKey(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK);
    myKeyboard.sendKey_Down();
    assertEquals("!345", myEditor.getText());
    CHECK.size(7, toList(model));
    if (!Env.isMac()) {
      myKeyboard.typeKey(KeyEvent.VK_A, 'a');
      CHECK.size(2, toList(model));
      myKeyboard.sendKey_Down();
      assertEquals("!345a", myEditor.getText());
      myKeyboard.sendKey_Enter();
      assertEquals("!345a", myEditor.getText());
      assertEquals("345a", model.getSelectedItem());
    }
  }

  private Collection<Object> toList(ListModel model) {
    java.util.List<Object> result = Collections15.arrayList(model.getSize());
    for (int i = 0; i < model.getSize(); i++)
      result.add(model.getElementAt(i));
    return result;
  }

  public static void main(String[] args) throws Exception {
    CompletingComboBoxControllerTests tests = new CompletingComboBoxControllerTests();
    tests.setUp();
    tests.myCombo.setSelectedIndex(2);
  }
}
