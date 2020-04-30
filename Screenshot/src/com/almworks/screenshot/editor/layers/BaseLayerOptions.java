package com.almworks.screenshot.editor.layers;

import com.almworks.util.ui.SingleChildLayout;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

import static com.almworks.util.ui.SingleChildLayout.CONTAINER;
import static com.almworks.util.ui.SingleChildLayout.PREFERRED;

public abstract class BaseLayerOptions implements LayerOptions {
  private final JPanel myWholePanel = new JPanel();
  private final JPanel myContentPanel = new JPanel();
  private final JLabel myHeaderLabel = new JLabel();

  protected BaseLayerOptions(String title, String description) {
    assert title != null;
    myWholePanel.setLayout(new SingleChildLayout(PREFERRED, PREFERRED, CONTAINER, PREFERRED));
    myWholePanel.add(myContentPanel);

    myContentPanel.setLayout(new BorderLayout(0, 9));
    myContentPanel.setBorder(new CompoundBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), title), new EmptyBorder(0, 5, 5, 5)));
    myContentPanel.setAlignmentY(0F);

    if (description != null) {
      myContentPanel.add(myHeaderLabel, BorderLayout.NORTH);
      myHeaderLabel.setText("<html>" + description);
    }

  }

  public final void attach() {
    JComponent content = createContent();
    if (content != null) {
      myContentPanel.add(content, BorderLayout.CENTER);
      myContentPanel.setMinimumSize(content.getMinimumSize());
      //myContentPanel.setPreferredSize(content.getPreferredSize());
    }
  }

  public void dispose() {
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  protected abstract JComponent createContent();

  public static class SliderOptions extends BaseLayerOptions implements ChangeListener {
    private JSlider mySlider;
    private JLabel myLabel;
    private Box myPanel;


    public SliderOptions(String title, String descr) {
      super(title, descr);
      myPanel = new Box(BoxLayout.Y_AXIS);
      mySlider = new JSlider();
      mySlider.addChangeListener(this);
      myLabel = new JLabel();

      myPanel.add(mySlider);
      myPanel.add(myLabel);
    }

    public int getValue() {
      return mySlider.getValue();
    }

    public void setValue(int value) {
      mySlider.setValue(value);
    }

    public void setSliderParams(int min, int max, int init) {
      mySlider.setMinimum(min);
      mySlider.setMaximum(max);
      mySlider.setValue(init);
      myLabel.setText(Integer.toString(init));
    }

    protected JComponent createContent() {
      return myPanel;
    }

    public void stateChanged(ChangeEvent e) {
      myLabel.setText(Integer.toString(mySlider.getValue()));
    }
  }
}