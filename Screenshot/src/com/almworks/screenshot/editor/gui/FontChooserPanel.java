package com.almworks.screenshot.editor.gui;

import com.almworks.util.ui.swing.AwtUtil;
import com.sun.java.swing.plaf.windows.WindowsBorders;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FontChooserPanel implements ActionListener {

  public static final Font DEFALUT_FONT = new Font("Arial", 0, 12);

  private Font myFont = DEFALUT_FONT;

  private Color myFontColor = Color.BLACK;

  SimpleAttributeSet myAttributes;

  private static FontChooserPanel ourInstance = new FontChooserPanel();

  private Box myPanel;

  private JComboBox myFontName;

  private JComboBox myFontSize;

  private JComboBox myFontColorBox;

  private FontChangeListener myListener;
  private String[] myFontNames;

  public void setListener(FontChangeListener myListener) {
    this.myListener = myListener;
  }

  public Font getCurrentFont() {
    return myFont;
  }

  public static FontChooserPanel getInstance() {
    return ourInstance;
  }

  public JComponent getComponent() {
    return myPanel;
  }

  private FontChooserPanel() {
    myPanel = new Box(BoxLayout.Y_AXIS);
    myAttributes = new SimpleAttributeSet();

    StyleConstants.setFontFamily(myAttributes, myFont.getFontName());
    StyleConstants.setFontSize(myAttributes, myFont.getSize());

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

    myFontNames = ge.getAvailableFontFamilyNames();
    myFontName = new JComboBox(myFontNames);

    setupInitFontName();

    myFontName.addActionListener(this);

    myFontSize = new JComboBox();
    for (int i = 10; i < 37; i += 2) {
      myFontSize.addItem(Integer.toString(i));
    }

    setupInitFontSize();

    myFontSize.addActionListener(this);

    myFontColorBox = new JComboBox(Palette.COLORS);
    ComboBoxRenderer comboBoxRenderer = new ComboBoxRenderer();
    myFontColorBox.setPreferredSize(myFontName.getPreferredSize());
    comboBoxRenderer.setPreferredSize(myFontName.getPreferredSize());

    myFontColorBox.setRenderer(comboBoxRenderer);
    myFontColorBox.setEditable(false);
    myFontColorBox.setFocusable(false);
    myFontColorBox.setMaximumRowCount(12);
    myFontColorBox.addActionListener(this);
    myFontColorBox.setSelectedItem(myFontColor);

    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 3));
    panel.add(myFontName);
    myPanel.add(panel);

    panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 3));
    panel.add(myFontSize);
    myPanel.add(panel);

    panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 3));
    panel.add(myFontColorBox);
    myPanel.add(panel);
  }


  private void setupInitFontSize() {
    String anObject = Integer.toString(myFont.getSize());
    myFontSize.setSelectedItem(anObject);
  }

  private void setupInitFontName() {
    String name = myFont.getFamily();
    myFontName.setSelectedItem(name);
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == myFontName) {
      StyleConstants.setFontFamily(myAttributes, (String) myFontName.getSelectedItem());
    } else if (e.getSource() == myFontSize) {
      StyleConstants.setFontSize(myAttributes, Integer.parseInt((String) myFontSize.getSelectedItem()));
    } else if (e.getSource() == myFontColorBox) {
      myFontColor = (Color) myFontColorBox.getSelectedItem();

      myFontColorBox.setForeground((Color) myFontColorBox.getSelectedItem());
    }
    if (myListener != null) {
      String family = StyleConstants.getFontFamily(myAttributes);
      int fontSize = StyleConstants.getFontSize(myAttributes);
      myFont = new Font(family, 0, fontSize);
      myListener.onFontChanged(myFont, myFontColor);
    }
  }

  public Color getCurrentFontColor() {
    return myFontColor;
  }

  public interface FontChangeListener {
    void onFontChanged(Font newFont, Color newColor);
  }


  class ComboBoxRenderer extends JComponent implements ListCellRenderer {
    Color myColor;

    public ComboBoxRenderer() {
      setOpaque(true);
    }

    public void paint(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      g.setColor(getForeground());
      g.fillRect(0, 0, getWidth(), getHeight());
      if (getBorder() != null) {
        getBorder().paintBorder(this, g, 0, 0, getWidth(), getHeight());
      }
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
      boolean cellHasFocus)
    {
      Color color = (Color) value;

      setForeground(color);
      if (isSelected) {
        setBorder(new WindowsBorders.DashedBorder(
          color == Color.BLACK || color == Color.BLUE || color == Color.DARK_GRAY ? Color.WHITE : Color.BLACK));
      } else {
        setBorder(null);
      }
      return this;
    }
  }
}
                            