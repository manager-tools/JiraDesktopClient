package com.almworks.screenshot.editor.image;


import com.almworks.screenshot.editor.gui.ZoomPanel;
import com.almworks.screenshot.editor.layers.ActionLayer;
import com.almworks.screenshot.editor.layers.LayerOptionsPanel;
import com.almworks.screenshot.editor.tools.annotate.AnnotateTool;
import com.almworks.screenshot.editor.tools.blur.BlurTool;
import com.almworks.screenshot.editor.tools.crop.CropTool;
import com.almworks.screenshot.editor.tools.highlight.HighlightTool;
import com.almworks.screenshot.editor.tools.transform.SelectionTool;
import com.almworks.screenshot.editor.tools.zoom.ZoomInTool;
import com.almworks.util.SequenceRunner;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.ui.BrokenLineBorder;
import com.almworks.util.ui.UIComponentWrapper2;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;

public class ImageEditor implements UIComponentWrapper2 {

  private final WorkingImage myWorkingImage;

  private final SequenceRunner myDisposeRunner = new SequenceRunner();

  private final List<ImageTool> myTools = Collections15.arrayList();
  private final ImageDesk myImageDesk;


  private JPanel myWholePanel = null;


  private UIComponentWrapper2 myToolPanel;

  public ImageEditor(BufferedImage image) {
    assert image != null;

    myWorkingImage = new WorkingImage(image);
    myImageDesk = new ImageDesk(myWorkingImage);

    installDefaultTools();

    myImageDesk.start();
  }


  public UIComponentWrapper2 createToolPanel(ImageDesk desk, List<ImageTool> tools) {
    return new EditorToolbar(desk, tools);
  }

  private void setupWholePanel() {
    myWholePanel.setLayout(new BorderLayout(0, 0));
    myWholePanel.add(myToolPanel.getComponent(), BorderLayout.WEST);
    myWholePanel.add(myImageDesk.getComponent(), BorderLayout.CENTER);
    myWholePanel.doLayout();
  }

  private void setupToolShortcuts(List<ImageTool> tools) {
    InputMap inputMap = myWholePanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ActionMap actionMap = myWholePanel.getActionMap();

    for (ImageTool tool : tools) {
      inputMap.put(KeyStroke.getKeyStroke(tool.getKeyCode(), 0), tool.getName());
      actionMap.put(tool.getName(), new SelectToolAction(tool));
    }
  }

  private void installDefaultTools() {
    myTools.add(new CropTool(myWorkingImage));
    myTools.add(new HighlightTool(myWorkingImage));
    myTools.add(new AnnotateTool(myWorkingImage));
    myTools.add(new BlurTool(myWorkingImage));
    myTools.add(new ZoomInTool(myWorkingImage));
    myTools.add(new SelectionTool(myWorkingImage));
  }

  public BufferedImage getImage() {
    return myWorkingImage.getResultImage();
  }

  public void addDisposeNotification(Runnable runnable) {
    myDisposeRunner.add(runnable);
  }

  public JComponent getComponent() {
    if (myWholePanel == null) {
      myWholePanel = new JPanel();
      myToolPanel = createToolPanel(myImageDesk, myTools);
      setupWholePanel();
      myImageDesk.addData(myWholePanel);
    }
    return myWholePanel;
  }

  public void dispose() {

  }

  public Detach getDetach() {
    return new Detach() {
      protected void doDetach() {
        myDisposeRunner.runAndClear();
      }
    };
  }

  private class SelectToolAction extends AbstractAction {
    private final ImageTool myTool;

    public SelectToolAction(ImageTool tool) {
      myTool = tool;
    }

    public void actionPerformed(ActionEvent e) {
      myImageDesk.getWorkingImage().changeActionLayer(myTool.createActionLayer());
    }
  }


  private class EditorToolbar implements UIComponentWrapper2 {
    private final LayerOptionsPanel myLayerOptionsPanel = new LayerOptionsPanel();

    private final JPanel myOptionsPanel = new JPanel(new BorderLayout());

    private final JPanel mySidebar = new JPanel();

    private final ZoomPanel myZoomPanel;

    private List<ImageTool> myTools;

    public EditorToolbar(ImageDesk imageDesk, List<ImageTool> tools) {
      myZoomPanel = new ZoomPanel(myImageDesk.getWorkingImage(), imageDesk);
      setupSidebar(tools);
      setupListenWorkingImage();
    }

    public JComponent getComponent() {
      return mySidebar;
    }

    @Deprecated
    public void dispose() {
    }

    public Detach getDetach() {
      return null;
    }

    private void setupSidebar(List<ImageTool> tools) {
      mySidebar.setLayout(new BorderLayout(0, 9));
      if(Aqua.isAqua()) {
        mySidebar.setBorder(UIUtil.getCompoundBorder(
          new BrokenLineBorder(Aqua.MAC_BORDER_COLOR, 1, BrokenLineBorder.EAST),
          new EmptyBorder(5, 3, 0, 3)));
      } else {
        mySidebar.setBorder(new EmptyBorder(0, 0, 0, 5));
      }
      myTools = tools;
      mySidebar.add(createToolPanel(tools), BorderLayout.NORTH);
      setupToolShortcuts(tools);

      mySidebar.add(myOptionsPanel);
      myOptionsPanel.add(myLayerOptionsPanel.getComponent(), BorderLayout.CENTER);
      myOptionsPanel.add(myZoomPanel.getComponent(), BorderLayout.SOUTH);
    }

    private JPanel createToolPanel(List<ImageTool> tools) {
      ImageToolbarBuilder builder = new ImageToolbarBuilder();
      ButtonGroup g = new ButtonGroup();
      if (tools != null) {
        for (ImageTool tool : tools) {
          tool.installWidgets(builder, g);
        }
      }
      return builder.createBar();
    }

    private void setupListenWorkingImage() {
      ScalarModel<ActionLayer> selectedLayer = myImageDesk.getWorkingImage().getSelectedLayer();
      selectedLayer.getEventSource().addAWTListener(Lifespan.FOREVER, new ScalarModel.Adapter<ActionLayer>() {
        public void onScalarChanged(ScalarModelEvent<ActionLayer> event) {
          ActionLayer layer = event.getNewValue();
          for (ImageTool myTool : myTools) {
            if (myTool.isOwner(layer)) {
              myTool.activate();
            }
          }
          myLayerOptionsPanel.show(layer == null ? null : layer.getOptions());
        }
      });

      ScalarModel<Double> scale = myImageDesk.getWorkingImage().getScaleModel();
      scale.getEventSource().addAWTListener(Lifespan.FOREVER, new ScalarModel.Adapter<Double>() {
        @Override
        public void onScalarChanged(ScalarModelEvent<Double> event) {
          myZoomPanel.setCustomScale(event.getNewValue());
        }
      });
    }
  }
}

