package com.almworks.engine.gui.attachments;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.ListModelHolder;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.tables.SortingTableHeaderController;
import com.almworks.util.config.Configuration;
import com.almworks.util.models.TableColumnAccessor;
import com.almworks.util.ui.DocumentFormAugmentor;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class AttachmentsTable<T extends Attachment> implements TableTooltipProvider {
  private final ASortedTable<T> myTable;
  private final AttachmentDownloadStatus<T> myStatus;
  private AttachmentTooltipProvider<? super T> myTooltipProvider;
  private final Lifecycle myLife = new Lifecycle(false);
  private final ListModelHolder<T> myRows = ListModelHolder.create();

  private static final String COLUMNS_CONFIG = "columns";

  public AttachmentsTable(AttachmentDownloadStatus<T> status, DataRole<T> attachmentDataRole) {
    myStatus = status;
    myTable = new ASortedTable<T>();
    myTable.setDataRoles(Attachment.ROLE, attachmentDataRole);
    myTable.setStriped(true);
    myTable.setGridHidden();
    myTable.setScrollableMode(false);
    if(Aqua.isAqua()) {
      final JTable jTable = (JTable) myTable.getSwingComponent();
      DocumentFormAugmentor.DO_NOT_AUGMENT.putClientValue(jTable.getTableHeader(), true);
    }
  }

  public void adjustForFormlet() {
    JComponent details = myTable.toComponent();
    if (details instanceof JScrollPane) {
      details.setBorder(Aqua.isAqua() ? Aqua.MAC_LIGHT_BORDER_NORTH : AwtUtil.EMPTY_BORDER);
    }
    myTable.addTooltipProvider(this);
    ImmediateTooltips.installImmediateTooltipManager(Lifespan.FOREVER, myTable.getSwingComponent());
  }

  public void initialize(List<AttachmentProperty<? super T, ?>> properties, Configuration config, MenuBuilder popupMenu) {
    myLife.cycle();
    Lifespan curLife = myLife.lifespan();
    setupColumns(curLife, properties, config);
    curLife.add(myTable.setCollectionModel(myRows));
    popupMenu.addToComponent(curLife, myTable.getSwingComponent());
  }

  private void setupColumns(Lifespan life, List<AttachmentProperty<? super T, ?>> properties, Configuration config) {
    boolean firstTime = !config.isSet(COLUMNS_CONFIG);
    AListModel<TableColumnAccessor<T, ?>> columns = createColumns(properties);
    SortingTableHeaderController header = myTable.getHeaderController();
    header.setUserFullColumnsModel(life, columns, false);
    myTable.setColumnModel(header.getUserFilteredColumnModel());
    Configuration columnsConfig = config.getOrCreateSubset(COLUMNS_CONFIG);
    ColumnsConfiguration.install(life, columnsConfig, header);
    if (firstTime) {
      myTable.forcePreferredColumnWidths();
    }
  }

  private AListModel<TableColumnAccessor<T, ?>> createColumns(Collection<AttachmentProperty<? super T, ?>> properties) {
    List<TableColumnAccessor<T, ?>> tableColumns = AttachmentPropertyColumn.collectList(properties, myStatus);
    tableColumns.add((TableColumnAccessor<T, ?>) new DownloadStateColumn(myStatus));
    return FixedListModel.create(tableColumns);
  }

  public JComponent getComponent() {
    return myTable.toComponent();
  }

  public void setTooltipProvider(AttachmentTooltipProvider<? super T> tooltipProvider) {
    myTooltipProvider = tooltipProvider;
  }

  public void setHighlightPattern(Pattern pattern) {
    myTable.setHighlightPattern(pattern);
  }

  public FlatCollectionComponent<T> getAComponent() {
    return myTable;
  }

  public JComponent getSwingComponent() {
    return myTable.getSwingComponent();
  }

  public void setCollectionModel(Lifespan life, AListModel<T> model) {
    life.add(myRows.setModel(model));
  }

  public void repaintUrl(String url) {
    if (url == null || url.length() == 0) myTable.repaint();
    else {
      AListModel<? extends T> model = myTable.getCollectionModel();
      for (int i = 0; i < model.getSize(); i++) {
        T attachment = model.getAt(i);
        if (url.equals(attachment.getUrl())) {
          Rectangle r = myTable.getElementRect(i);
          myTable.getSwingComponent().repaint(r);
        }
      }
    }
  }

  public String getTooltip(int row, int column, Point tablePoint) {
    AListModel<? extends T> model = myTable.getCollectionModel();
    if (row < 0 || row >= model.getSize())
      return null;
    T item = model.getAt(row);
    return AttachmentsPanel.getTooltipText(myTooltipProvider, myStatus, item);
  }

  public Collection<? extends Attachment> getAttachments() {
    AListModel<? extends T> model = myTable.getCollectionModel();
    return Collections15.arrayList(model.toList());
  }
}
