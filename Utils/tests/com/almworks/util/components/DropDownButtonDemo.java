package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DropDownButtonDemo extends JPanel {
  private final OrderListModel<MyAction> myModel = OrderListModel.create();
  private final AList<MyAction> myList = new AList<MyAction>(myModel);

  private final JTextField myActionName = new JTextField();
  private final JCheckBox myActionVisible = new JCheckBox("Visible");
  private final JCheckBox myActionEnabled = new JCheckBox("Enabled");

  private final JButton mySetButton = new JButton("Set");
  private final JButton myAddButton = new JButton("Add");
  private final JButton myRemoveButton = new JButton("Remove");

  private final JLabel myActionComponent = new JLabel();

  private DropDownButton myDropDown;

  public DropDownButtonDemo() {
    super(new BorderLayout());
    buildUp();

    myModel.addElement(new MyAction("AAA", true, true));

    myDropDown.setActions(myModel);

    myList.getSelectionAccessor().addChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        MyAction action = myList.getSelectionAccessor().getSelection();
        if (action != null) {
          myActionName.setText(action.getName());
          myActionVisible.setSelected(action.isVisible());
          myActionEnabled.setSelected(action.isEnabled());
        } else {
          myActionName.setText("");
          myActionVisible.setSelected(false);
          myActionEnabled.setSelected(false);
        }
      }
    });

    mySetButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        DropDownButtonDemo.MyAction action = myList.getSelectionAccessor().getSelection();
        if (action != null) {
          action.setName(myActionName.getText());
          action.setVisible(myActionVisible.isSelected());
          action.setEnabled(myActionEnabled.isSelected());
        }
      }
    });

    myAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        MyAction action = new MyAction(myActionName.getText(), myActionEnabled.isSelected(), myActionVisible.isSelected());
        int i = myList.getSelectionAccessor().getSelectedIndex();
        if (i < 0) {
          myModel.addElement(action);
        } else {
          myModel.insert(i + 1, action);
        }
      }
    });

    myRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = myList.getSelectionAccessor().getSelectedIndex();
        if (i >= 0) {
          myModel.removeAt(i);
        }
      }
    });
  }

  private void buildUp() {
    AToolbar toolbar = new AToolbar(myActionComponent);
    myDropDown = toolbar.addDropDownButton(myActionComponent, "none");
    add(toolbar, BorderLayout.NORTH);

    JPanel actPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
    actPanel.add(mySetButton);
    actPanel.add(myAddButton);
    actPanel.add(myRemoveButton);

    Box detailsPanel = new Box(BoxLayout.Y_AXIS);
    myActionName.setAlignmentX(0F);
    myActionVisible.setAlignmentX(0F);
    myActionEnabled.setAlignmentX(0F);
    actPanel.setAlignmentX(0F);
    myActionName.setMaximumSize(new Dimension(Short.MAX_VALUE, myActionName.getPreferredSize().height));
    detailsPanel.add(myActionName);
    detailsPanel.add(myActionVisible);
    detailsPanel.add(myActionEnabled);
    detailsPanel.add(actPanel);
    detailsPanel.add(new JPanel());

    myList.setPrototypeCellValue(new MyAction("there was an old lady", true, true));

    JPanel mainPanel = new JPanel(new BorderLayout(5, 0));
    mainPanel.add(new JScrollPane(myList), BorderLayout.WEST);
    mainPanel.add(detailsPanel, BorderLayout.CENTER);

    add(mainPanel, BorderLayout.CENTER);
  }

  public static void main(String[] args) {
    LAFUtil.installExtensions();
    LAFUtil.installPatches();
    DebugFrame.show(new DropDownButtonDemo(), 500, 500);
  }

  private class MyAction extends SimpleAction implements CanvasRenderable {
    private final SimpleModifiable myModifiable = new SimpleModifiable();

    private String myName;
    private boolean myEnabled;
    private boolean myVisible;

    public MyAction(String name, boolean enabled, boolean visible) {
      myName = name;
      myEnabled = enabled;
      myVisible = visible;
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.updateOnChange(myModifiable);
      context.putPresentationProperty(PresentationKey.NAME, myName);
      context.setEnabled(myVisible ? (myEnabled ? EnableState.ENABLED : EnableState.DISABLED) : EnableState.INVISIBLE);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {

    }

    public String getName() {
      return myName;
    }

    public void setName(String name) {
      if (!Util.equals(name, myName)) {
        myName = name;
        myModifiable.fireChanged();
      }
    }

    public boolean isEnabled() {
      return myEnabled;
    }

    public void setEnabled(boolean enabled) {
      if (myEnabled != enabled) {
        myEnabled = enabled;
        myModifiable.fireChanged();
      }
    }

    public boolean isVisible() {
      return myVisible;
    }

    public void setVisible(boolean visible) {
      if (visible != myVisible) {
        myVisible = visible;
        myModifiable.fireChanged();
      }
    }

    public void renderOn(Canvas canvas, CellState state) {
      canvas.appendText(myName);
    }
  }
}
