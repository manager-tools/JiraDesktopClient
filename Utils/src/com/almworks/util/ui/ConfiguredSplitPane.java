package com.almworks.util.ui;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.ACollectionComponent;
import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.Configuration;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ConfiguredSplitPane extends AdjustedSplitPane {
  private static final String DIVIDER_SETTING = "divider";
  private static final String EXPLICIT_HIDE = "explicitHide";

  private final ConfigAccessors.Int myDividerSetting;

  private boolean myUpdateDivider = false;
  private boolean myResettingDivider = false;
  private boolean myHidingSecondPanel = false;
  private float myRatio;

  @Nullable
  private final ACollectionComponent<?> myTopCollection;
  private final MySelectionListener myTableListener;

  private final Lifecycle mySwingLife = new Lifecycle(false);
  private final ConfigAccessors.Bool myHideBottom;
  private int myStoredDividerSize;

  private ConfiguredSplitPane(boolean leftright, Component one, Component two, Configuration config,
    float defaultDividerRatio, ACollectionComponent<?> topCollection, boolean showBottomByDefault)
  {
    super(leftright ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, one, two);
    int defaultValue = (int) (UIUtil.SPLITPANE_DIVIDER_GRANULARITY * defaultDividerRatio);
    myDividerSetting = ConfigAccessors.integer(config, defaultValue, DIVIDER_SETTING);
    myHideBottom = ConfigAccessors.bool(config, EXPLICIT_HIDE, !showBottomByDefault);
    myRatio = Math.min(0.95F, Math.max(1F * myDividerSetting.getInt() / UIUtil.SPLITPANE_DIVIDER_GRANULARITY, 0.05F));
    myTopCollection = topCollection;
    myTableListener = topCollection == null ? null : new MySelectionListener();
  }

  public static ConfiguredSplitPane createLeftRight(Component one, Component two, Configuration config,
    float defaultRatio)
  {
    return new ConfiguredSplitPane(true, one, two, config, defaultRatio, null, true);
  }

  public static ConfiguredSplitPane createTopBottom(Component one, Component two, Configuration config,
    float defaultRatio)
  {
    return new ConfiguredSplitPane(false, one, two, config, defaultRatio, null, true);
  }

  public static ConfiguredSplitPane createLeftRightJumping(Component one, Component two, Configuration config,
    float defaultRatio, ACollectionComponent<?> topTable, boolean showRightByDefault)
  {
    return new ConfiguredSplitPane(true, one, two, config, defaultRatio, topTable, showRightByDefault);
  }

  public static ConfiguredSplitPane createTopBottomJumping(Component one, Component two, Configuration config,
    float defaultRatio, ACollectionComponent<?> topTable, boolean showBottomByDefault)
  {
    return new ConfiguredSplitPane(false, one, two, config, defaultRatio, topTable, showBottomByDefault);
  }

  public void addNotify() {
    mySwingLife.cycleStart();
    myUpdateDivider = true;
    super.addNotify();
    if (myTopCollection != null) {
      assert myTableListener != null : this;
      myTopCollection.getSelectionAccessor().addAWTChangeListener(mySwingLife.lifespan(), myTableListener);
      myTableListener.onChange();
    }
  }

  public void removeNotify() {
    super.removeNotify();
    mySwingLife.cycleEnd();
  }

  public void reshape(int x, int y, int w, int h) {
    super.reshape(x, y, w, h);
    if (myUpdateDivider) {
      myUpdateDivider = false;
      resetDivider();
      // allow validation to finish
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          resetDivider();
        }
      });
    } else {
      updateRatio();
    }
  }

  private void updateRatio() {
    if (!hasBothComponents())
      return;
    if (!myResettingDivider && !myHidingSecondPanel) {
      int location = getDividerLocation();
      boolean leftright = getOrientation() == HORIZONTAL_SPLIT;
      int size = leftright ? getWidth() : getHeight();
      float ratio = 1F * location / size;
      myRatio = Math.min(0.95F, Math.max(ratio, 0.05F));
      int setting = (int) (UIUtil.SPLITPANE_DIVIDER_GRANULARITY * myRatio);
      myDividerSetting.setInt(setting);
    }
  }

  private void resetDivider() {
    if (!myHidingSecondPanel) {
      int size = getOrientation() == HORIZONTAL_SPLIT ? getWidth() : getHeight();
      assert myRatio >= 0F && myRatio <= 1F;
      int divider = (int) (myRatio * size);
      if (Math.abs(getDividerLocation() - divider) > 5) {
        myResettingDivider = true;
        setDividerLocation(divider);
        myResettingDivider = false;
      }
    }
  }

  public void setDividerLocation(int location) {
    super.setDividerLocation(location);
    updateRatio();
  }

  @Override
  public void setDividerSize(int newSize) {
    int oldSize = getDividerSize();
    if (oldSize > 0) {
      myStoredDividerSize = oldSize;
    }
    super.setDividerSize(newSize);
  }

  public void restoreDividerSize() {
    int newSize = myStoredDividerSize;
    if (newSize == 0) {
      newSize = UIManager.getInt("SplitPane.dividerSize");
    }
    super.setDividerSize(newSize);
  }

  public void setLeftComponent(Component comp) {
    super.setLeftComponent(comp);
    checkDivider();
  }

  public void setTopComponent(Component comp) {
    super.setTopComponent(comp);
    checkDivider();
  }

  public void setRightComponent(Component comp) {
    super.setRightComponent(comp);
    checkDivider();
  }

  public void setBottomComponent(Component comp) {
    super.setBottomComponent(comp);
    checkDivider();
  }

  private void checkDivider() {
    if (hasBothComponents()) {
      restoreDividerSize();
      resetDivider();
    } else
      setDividerSize(0);
  }

  private boolean hasBothComponents() {
    return getLeftComponent() != null && getRightComponent() != null;
  }

  private void hideSecondPanel() {
    if (!myHidingSecondPanel) {
      myHidingSecondPanel = true;
      setDividerSize(0);
      setDividerLocation(Short.MAX_VALUE);
      getBottomComponent().setVisible(false);
    }
  }

  private boolean hasSelection() {
    return myTopCollection != null && myTopCollection.getSelectionAccessor().hasSelection();
  }

  private void showSecondPanel() {
    if (myHidingSecondPanel) {
      myHidingSecondPanel = false;
      getBottomComponent().setVisible(true);
      restoreDividerSize();
      if (isDisplayable()) {
        resetDivider();
      }
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          assert myTopCollection != null;
          myTopCollection.scrollSelectionToView();
        }
      });
    }
  }

  private class MySelectionListener implements ChangeListener {
    public void onChange() {
      assert myTopCollection != null;
      boolean hasSelection = hasSelection();
      boolean explicitlyHidden = myHideBottom.getBool();
      if (!hasSelection || explicitlyHidden)
        hideSecondPanel();
      else
        showSecondPanel();
    }
  }

  public void setSecondExplicitlyHidden(boolean hidden) {
    if (myHideBottom.getBool() == hidden)
      return;
    myHideBottom.setBool(hidden);
    if (hidden || !hasSelection())
      hideSecondPanel();
    else
      showSecondPanel();
    firePropertyChange(EXPLICIT_HIDE, !hidden, hidden);
  }

  public boolean isBottomExplicitlyHidden() {
    return myHideBottom.getBool();
  }

  public void toggleSecondExplicitlyHidden() {
    setSecondExplicitlyHidden(!isBottomExplicitlyHidden());
  }

  public AnAction createShowBottomAction(String name) {
    return new SimpleAction(name, Icons.DETAILS_PANEL) {
      protected void customUpdate(UpdateContext context) throws CantPerformException {
        context.updateOnPropertyChange(ConfiguredSplitPane.this, EXPLICIT_HIDE);
        boolean hidden = isBottomExplicitlyHidden();
        context.putPresentationProperty(PresentationKey.TOGGLED_ON, !hidden);
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        toggleSecondExplicitlyHidden();
      }
    };
  }

//  public static void main(String[] args) {
//    SwingUtilities.invokeLater(new Runnable() {
//      public void run() {
//        LAFUtil.initializeLookAndFeel();
//        JPanel panel = new JPanel(new BorderLayout());
//        final Configuration config = MapMedium.createConfig();
//        final PlaceHolder place = new PlaceHolder();
//        place.setPreferredSize(new Dimension(300, 500));
//        JButton button = new JButton("Recreate");
//        button.addActionListener(new ActionListener() {
//          public void actionPerformed(ActionEvent e) {
//            place.show(new JLabel("recreating"));
//            Timer timer = new Timer(1000, new ActionListener() {
//              public void actionPerformed(ActionEvent e) {
//                OrderListModel m = OrderListModel.create(Arrays.asList(new String[] {"x", "y", "z"}));
//                TableColumnAccessor column = new BaseTableColumnAccessor("haba") {
//                  public Object getValue(Object object) {
//                    return object;
//                  }
//                };
//                OrderListModel cm = OrderListModel.create(Arrays.asList(new TableColumnAccessor[] {column}));
//                ATable table = new ATable();
//                table.setCollectionModel(m);
//                table.setColumnModel(cm);
//                table.getSelectionAccessor().ensureSelectionExists();
//                JTextArea content = new JTextArea();
//                JSplitPane splitPane = createTopBottomJumping(new JScrollPane(table), content, config, 0.3F, table);
//                splitPane.setResizeWeight(0);
//                place.show(splitPane);
//              }
//            });
//            timer.setRepeats(false);
//            timer.start();
//          }
//        });
//
//        panel.add(button, BorderLayout.NORTH);
//        panel.add(place, BorderLayout.CENTER);
//
//        DebugFrame.show(panel);
//      }
//    });
//  }
}
