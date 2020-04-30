package com.almworks.explorer;

import com.almworks.api.application.*;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Containers;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.models.BaseTableColumnAccessor;
import com.almworks.util.models.ColumnAccessor;
import com.almworks.util.models.ColumnSizePolicy;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.BaseRendererComponent;
import org.almworks.util.Collections15;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dyoma
 */
class LoadedArtifactStatusColumn extends BaseTableColumnAccessor<LoadedItem, Comparable>
  implements ColumnTooltipProvider<LoadedItem>
{
  private static final Comparable NO_STATE = 0;
  private static final Comparable LOCALLY_CHANGED = 1;
  private static final Comparable CONFLICT = 2;
  private static final Comparable PROBLEM = 3;

  private static final int INTER_ICON_SPACE = 2;
  private static final int MINIMUM_WIDTH = 37;
  private static final int LEFT_MARGIN = 4;
  private static final int RIGHT_MARGIN = 2;
//  JCO-529
//  private static final int DOWNLOAD_STATE_INDICATOR_SIZE = 7;

  private static final Map<Comparable, Icon> STATE_ICONS;
  private static final Map<Comparable, String> STATE_TOOLTIPS;

//  JCO-529
//  private static final Map<ArtifactDownloadStage, String> DOWNLOAD_STAGE_TOOLTIPS;
//  private static final Map<ArtifactDownloadStage, Icon> DOWNLOAD_STAGE_ICONS;

  static {
    HashMap<Comparable, Icon> icons = Collections15.hashMap();
    icons.put(CONFLICT, Icons.ARTIFACT_STATE_HAS_SYNC_CONFLICT);
    icons.put(PROBLEM, Icons.ARTIFACT_STATE_HAS_SYNC_PROBLEM);
    icons.put(LOCALLY_CHANGED, Icons.ARTIFACT_STATE_HAS_UNSYNC_CHANGES);
    STATE_ICONS = Collections.unmodifiableMap(icons);

//  JCO-529
//    Map<ArtifactDownloadStage, Icon> dsIcons = Collections15.hashMap();
//    dsIcons.put(ArtifactDownloadStage.NEW, Icons.STATE_FLAG_NEW);
//    dsIcons.put(ArtifactDownloadStage.DUMMY, Icons.STATE_FLAG_DUMMY);
////    dsIcons.put(ArtifactDownloadStage.FULL, Icons.STATE_FLAG_FULL_DOWNLOADED);
//    dsIcons.put(ArtifactDownloadStage.QUICK, Icons.STATE_FLAG_HALF_DOWNLOADED);
//    dsIcons.put(ArtifactDownloadStage.STALE, Icons.STATE_FLAG_HALF_DOWNLOADED);
//    DOWNLOAD_STAGE_ICONS = Collections.unmodifiableMap(dsIcons);

    HashMap<Comparable, String> tooltips = Collections15.hashMap();
    tooltips.put(CONFLICT, Local.parse(
      "<html><body>There are conflicting changes.<br>You have changed this " + Terms.ref_artifact +
        ", and at the same time someone has changed it on server.<br>" +
        "Use Edit-Merge action to resolve the conflict."));

    tooltips.put(PROBLEM, Local.parse(
      "<html><body>There was a synchronization problem with this " + Terms.ref_artifact + ".<br>" +
        "To view the error message, either select this " + Terms.ref_artifact +
        " or open synchronization window by clicking on 'Last updated' message in the status bar."));

    tooltips.put(LOCALLY_CHANGED, Local.parse("<html><body>This " + Terms.ref_artifact +
      " has been changed locally. Changes are not yet uploaded to the server.<br>" +
      "To upload changes, right click on this " + Terms.ref_artifact + " and select 'Upload' action.<br>" +
      "You can use Connection | Upload Changes action to upload changes made to all " + Terms.ref_artifacts + "."));
    STATE_TOOLTIPS = Collections.unmodifiableMap(tooltips);

//  JCO-529
//    HashMap<ArtifactDownloadStage, String> downloadTooltips = Collections15.hashMap();
//    downloadTooltips.put(ArtifactDownloadStage.DUMMY, Local.parse(
//      Terms.ref_Artifact + " has not been downloaded. To download, right-click on it and select 'Download' action."));
//    downloadTooltips.put(ArtifactDownloadStage.QUICK, Local.parse(Terms.ref_Artifact +
//      " has been partially downloaded. To download all the details, right-click on it and select 'Download' action."));
//    downloadTooltips.put(ArtifactDownloadStage.STALE, Local.parse(Terms.ref_Artifact +
//      " has been partially downloaded. To download all the details, right-click on it and select 'Download' action."));
//    downloadTooltips.put(ArtifactDownloadStage.FULL, Local.parse(Terms.ref_Artifact + " has been fully downloaded."));
//    downloadTooltips.put(ArtifactDownloadStage.NEW,
//      Local.parse(Terms.ref_Artifact + " has been created locally but not yet uploaded."));
//    DOWNLOAD_STAGE_TOOLTIPS = downloadTooltips;
  }

  private final ATable<LoadedItem> myTable;
  private final Bottleneck myUpdateBottleneck = new Bottleneck(150, ThreadGate.AWT, new Runnable() {
    public void run() {
      doUpdateWidth();
    }
  });

  private WeakReference<AListModel<? extends LoadedItem>> myLastModel;
  private int myWidth;
  private int myMinimumWidth;
  // JCO-529
//  private static final int STAGE_INDICATOR_RIGHT_GAP = 2;


  public LoadedArtifactStatusColumn(ATable<LoadedItem> table) {
    super("state", "State", new MyRenderer(), Containers.comparablesComparator());
    myTable = table;
    myWidth = getMinimumWidth();
    putHint(TableColumnAccessor.LINE_EAST_HINT, true);
  }

  private int getMinimumWidth() {
    if (myMinimumWidth == 0) {
      JTable table = new JTable(1, 1);
      TableCellRenderer renderer = table.getTableHeader().getDefaultRenderer();
      Component c = renderer.getTableCellRendererComponent(table, getColumnHeaderText(), true, true, -1, 0);
      int headerWidth = c.getPreferredSize().width + 4;
      myMinimumWidth = Math.max(headerWidth, MINIMUM_WIDTH);
    }
    return myMinimumWidth;
  }

  public Comparable getValue(LoadedItem object) {
    return getState(object);
  }

  public String getColumnHeaderText() {
    return "";
  }

  public int getPreferredWidth(JTable table, ATableModel<LoadedItem> tableModel,
    ColumnAccessor<LoadedItem> renderingAccessor, int columnIndex)
  {
    return myWidth;
  }

  public ColumnSizePolicy getSizePolicy() {
    return ColumnSizePolicy.FIXED;
  }

  public boolean isOrderChangeAllowed() {
    return false;
  }

  public boolean isSortingAllowed() {
    return false;
  }

  public ColumnTooltipProvider<LoadedItem> getTooltipProvider() {
    return this;
  }

  public String getTooltip(CellState cellState, LoadedItem element, Point cellPoint, Rectangle cellRect) {
    if (element == null)
      return null;
    int x = cellPoint.x;

    if (x < LEFT_MARGIN || x >= cellRect.width)
      return null;

    // JCO-529
//    if (x >= cellRect.width - DOWNLOAD_STATE_INDICATOR_SIZE - STAGE_INDICATOR_RIGHT_GAP - 1) {
//      return DOWNLOAD_STAGE_TOOLTIPS.get(ArtifactDownloadStageKey.retrieveValue(element.getValues()));
//    }

    x -= LEFT_MARGIN;

    Comparable state = getState(element);
    Icon stateIcon = STATE_ICONS.get(state);
    if (stateIcon != null) {
      int w = stateIcon.getIconWidth();
      if (x < w) {
        return STATE_TOOLTIPS.get(state);
      }
      x -= w + INTER_ICON_SPACE;
    }

    StateIcon[] icons = StateIconHelper.getStateIcons(element.getValues());
    if (icons != null) {
      for (StateIcon icon : icons) {
        Icon ico = icon.getIcon();
        if (ico != null) {
          int w = ico.getIconWidth();
          if (w > 0) {
            if (x < w) {
              return icon.getTooltip(element);
            } else {
              x -= w + INTER_ICON_SPACE;
            }
          }
        }
      }
    }

    return null;
  }

  public void updateWidth(AListModel<? extends LoadedItem> model) {
    myLastModel = new WeakReference<AListModel<? extends LoadedItem>>(model);
    myUpdateBottleneck.request();
  }

  private void doUpdateWidth() {
    AListModel<? extends LoadedItem> model = myLastModel.get();
    if (model != null) {
      int width = getMinimumWidth();

      for (int i = 0; i < model.getSize(); i++) {
        LoadedItem item = model.getAt(i);
        int w = getState(item) == NO_STATE ? 0 : 16;
        StateIcon[] stateIcons = StateIconHelper.getStateIcons(item.getValues());
        if (stateIcons != null) {
          for (StateIcon stateIcon : stateIcons) {
            if (w > 0)
              w += INTER_ICON_SPACE;
            w += stateIcon.getIcon().getIconWidth();
          }
        }
        // JCO-529
        w += LEFT_MARGIN + RIGHT_MARGIN/* + DOWNLOAD_STATE_INDICATOR_SIZE + STAGE_INDICATOR_RIGHT_GAP*/;
        width = Math.max(width, w);
      }
      if (myWidth != width) {
        myWidth = width;
        myTable.resizeColumn(this);
      }
    }
  }

  private static Comparable getState(LoadedItem item) {
    LoadedItem.DBStatus dbStatus = item.getDBStatus();
    if (dbStatus == ItemWrapper.DBStatus.DB_CONFLICT)
      return CONFLICT;
    if (item.hasProblems())
      return PROBLEM;
    if (dbStatus.isUploadable())
      return LOCALLY_CHANGED;
    return NO_STATE;
  }


  private static class MyRenderer extends BaseRendererComponent
    implements CollectionRenderer<LoadedItem>, RowBorderBounding
  {
    private Icon myIcon;
    private StateIcon[] myStateIcons;
    private Color myBackground;
    private ItemDownloadStage myDownloadStage;

    public JComponent getRendererComponent(CellState state, LoadedItem item) {
      PropertyMap values = item.getValues();
      myStateIcons = StateIconHelper.getStateIcons(values);
      Comparable artifactState = getState(item);
      myIcon = STATE_ICONS.get(artifactState);
      myDownloadStage = ItemDownloadStageKey.retrieveValue(values);
      return this;
    }

    public int getRowBorderX(JTable table, Graphics g) {
      return -1;
    }

    protected void paintComponent(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      int width = getWidth();
      int height = getHeight();

      // download state
      // JCO-529
//      paintDownloadStage(g, width - 1 - STAGE_INDICATOR_RIGHT_GAP - DOWNLOAD_STATE_INDICATOR_SIZE, 0,
//        DOWNLOAD_STATE_INDICATOR_SIZE, height);

      // icons
      int x = LEFT_MARGIN;
      int w = paintDBStateIcon(g, x);
      if (w > 0)
        x += w + INTER_ICON_SPACE;
      paintStateIcons(g, x);
    }

//    JCO-529
//    private void paintDownloadStage(Graphics g, int x, int y, int width, int height) {
//      Icon dsIcon = DOWNLOAD_STAGE_ICONS.get(myDownloadStage);
//      if (dsIcon == null)
//        return;
//      int boxh = dsIcon.getIconHeight();
//      assert dsIcon.getIconWidth() == width : dsIcon;
//      int boxy = boxh < height ? y + (height - boxh) / 2 : y;
//      dsIcon.paintIcon(this, g, x, boxy);
//    }

    private Color getBackgroundColor() {
      if (myBackground == null) {
        Container p = getParent();
        while (p != null && !(p instanceof JTable))
          p = p.getParent();
        if (p == null) {
          assert false : this;
          myBackground = Color.WHITE;
        } else {
          Color c = UIManager.getColor("Panel.background");
          myBackground = ColorUtil.between(p.getBackground(), c, 0.2F);
        }
      }
      return myBackground;
    }

    private int paintDBStateIcon(Graphics g, int x) {
      if (myIcon == null) {
        return 0;
      } else {
        paintIcon(g, myIcon, x);
        return myIcon.getIconWidth();
      }
    }

    private void paintIcon(Graphics g, Icon icon, int x) {
      int y = 0;
      int height = getHeight();
      if (height > 0) {
        y = (height - icon.getIconHeight()) / 2;
      }
      icon.paintIcon(this, g, x, y);
    }

    private void paintStateIcons(Graphics g, int x) {
      if (myStateIcons != null) {
        for (StateIcon stateIcon : myStateIcons) {
          Icon icon = stateIcon.getIcon();
          paintIcon(g, icon, x);
          int w = icon.getIconWidth();
          if (w > 0)
            x += w + INTER_ICON_SPACE;
        }
      }
    }
  }
}
