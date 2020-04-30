package com.almworks.engine.gui.attachments;

import com.almworks.api.download.DownloadManager;
import com.almworks.api.download.DownloadedFile;
import com.almworks.api.download.FileDownloadListener;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.ACollectionComponent;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.ThumbnailView;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.config.Configuration;
import com.almworks.util.progress.ProgressData;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static com.almworks.util.ui.actions.PresentationMapping.NONAME;

public class AttachmentsPanel<T extends Attachment> implements WidthDrivenComponent, AttachmentsEnv, Highlightable, FileDownloadListener, AttachmentsController {
  private static final int GAP = 5;
  private static final String TABLE_CONFIG = "columns";

  private final JPanel mySwitchPanel;
  private final ThumbnailView<T> myThumbnailView;
  private final Lifecycle myModelLife = new Lifecycle();
  private final AttachmentDownloadStatus<T> myDownloadStatus = new AttachmentDownloadStatus(this);
  private final AttachmentsViewMode myMode;
  private final MenuBuilder myPopupMenu;
  private final List<AttachmentProperty<? super T, ?>> myProperties = Collections15.arrayList();
  private final AttachmentThumbnailUI<T> myThumbnailUI;
  private final AttachmentsTable<T> myTable;

  private AttachmentTooltipProvider<? super T> myTooltipProvider;
  private final Configuration myConfiguration;
  public static final IdActionProxy GENERAL_DELETE_ATTACHMENT = new IdActionProxy(MainMenu.Edit.DELETE_ATTACHMENT);
  private final DataRole<T> myAttachmentDataRole;

  public AttachmentsPanel(MenuBuilder popupMenu, DataRole<T> attachmentDataRole, Configuration globalConfig, @Nullable Configuration localConfig) {
    myConfiguration = globalConfig;
    if (localConfig == null)
      localConfig = globalConfig;
    myMode = new AttachmentsViewMode(localConfig);
    myPopupMenu = popupMenu;
    myThumbnailUI = new AttachmentThumbnailUI<T>(this);
    myThumbnailView = ThumbnailView.create(myThumbnailUI);
    myAttachmentDataRole = attachmentDataRole;
    myTable = new AttachmentsTable<T>(myDownloadStatus, myAttachmentDataRole);
    myTable.adjustForFormlet();
    mySwitchPanel = new JPanel();
    mySwitchPanel.setLayout(new CardLayout(0, 0));
  }

    public void initialize() {
    ConstProvider.addGlobalValue(mySwitchPanel, ROLE, this);
    initializeThumbnails();
    initializeTable();
    initializeViewMode();
  }

  private void initializeThumbnails() {
    myThumbnailView.setDataRoles(Attachment.ROLE, myAttachmentDataRole);
    myThumbnailView.setImmediateTooltips(true);
    myThumbnailView.setPopupMenu(myPopupMenu);
    myThumbnailView.setScrollableMode(false);
  }

  private void initializeTable() {
    myTable.initialize(myProperties, myConfiguration.getOrCreateSubset(TABLE_CONFIG), myPopupMenu);
  }

  private void initializeViewMode() {
    JComponent thumbnails = myThumbnailView.toComponent();
    if (thumbnails instanceof JScrollPane) {
      thumbnails.setBorder(AwtUtil.EMPTY_BORDER);
    }
    myMode.addThumbnailsCard(mySwitchPanel, thumbnails);
    myMode.addTableCard(mySwitchPanel, myTable.getComponent());
    myMode.getModifiable().addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
      public void onChange() {
        onSwitchMode();
      }
    });
    onSwitchMode();
  }

  public List<ToolbarEntry> createToolbar() {
    List<ToolbarEntry> result = Collections15.arrayList();
    JComponent context = myThumbnailView.getSwingComponent();
    AnAction[] viewActions = SelectAttachmentViewModeAction.setup(context, myMode);
    for (AnAction viewAction : viewActions) {
      result.add(new ActionToolbarEntry(viewAction, context, NONAME));
    }
    result.add(ActionToolbarEntry.create(MainMenu.Attachments.DOWNLOAD_ALL, context, NONAME));
    result.add(ActionToolbarEntry.create(MainMenu.Attachments.SAVE_ALL, context, NONAME));
    return result;
  }

  private void onSwitchMode() {
    myMode.selectCard(mySwitchPanel);
    mySwitchPanel.invalidate();
    Container p = mySwitchPanel.getParent();
    if (p instanceof JComponent) {
      ((JComponent) p).revalidate();
    }

    ACollectionComponent<T> source = myMode.isThumbnailsMode() ? myTable.getAComponent() : myThumbnailView;
    ACollectionComponent<T> target = myMode.isThumbnailsMode() ? myThumbnailView : myTable.getAComponent();
    List<T> selected = source.getSelectionAccessor().getSelectedItems();
    target.getSelectionAccessor().setSelected(selected);
    target.scrollSelectionToView();
  }

  public int getPreferredWidth() {
    return mySwitchPanel.getPreferredSize().width;
  }

  public int getPreferredHeight(int width) {
    Insets insets = mySwitchPanel.getInsets();
    if (myMode.isThumbnailsMode()) {
      int iconicHeight = myThumbnailView.getPreferredHeight(width - insets.left - insets.right - 2 * GAP);
      return iconicHeight + insets.top + insets.bottom;
    } else {
      JComponent table = myTable.getSwingComponent();
      JComponent scrollpane = myTable.getComponent();
      Insets scrollinsets = AwtUtil.uniteInsetsFromTo((JComponent) table.getParent(), scrollpane);
      int tableHeight = table.getPreferredSize().height;
      if (table instanceof JTable) {
        tableHeight += ((JTable) table).getTableHeader().getPreferredSize().height;
      }
      return tableHeight + scrollinsets.top + scrollinsets.bottom + insets.top + insets.bottom;
    }
  }

  @NotNull
  public JComponent getComponent() {
    return mySwitchPanel;
  }

  public boolean isVisibleComponent() {
    return getComponent().isVisible();
  }

  public Detach show(final AListModel<T> model, DownloadManager downloadManager) {
    myModelLife.cycle();

    Lifespan lifespan = myModelLife.lifespan();
    myDownloadStatus.watch(lifespan, model, downloadManager);

    lifespan.add(myThumbnailView.setCollectionModel(model));
    myTable.setCollectionModel(lifespan, model);

    myThumbnailView.getSelectionAccessor().clearSelection();
    return myModelLife.getCurrentCycleDetach();
  }

  public void onDownloadStatus(DownloadedFile dfile) {
    String url = dfile.getKeyURL();
    DownloadedFile.State state = dfile.getState();
    if (state == DownloadedFile.State.READY && getModel().getSize() == 1) {
      myThumbnailView.revalidate();
      if (!myMode.isThumbnailsMode()) myTable.repaintUrl(url);
    } else {
      repaintItem(url);
    }
  }

  @Nullable
  public String getTooltipText(Attachment item) {
    return getTooltipText(myTooltipProvider, myDownloadStatus, (T)item);
  }

  public static <T extends Attachment> String getTooltipText(AttachmentTooltipProvider<? super T> provider, AttachmentDownloadStatus<T> status, T item) {
    if (provider == null)
      return null;
    if (item == null)
      return null;
    DownloadedFile dfile = item.getDownloadedFile(status);

    StringBuilder tooltip = new StringBuilder();
    provider.addTooltipText(tooltip, item, dfile);
    if (tooltip.length() == 0)
      return null;

    if (dfile != null) {
      DownloadedFile.State state = dfile.getState();
      addBr(tooltip).append(getStateString(item, state));
      if (state == DownloadedFile.State.DOWNLOADING) {
        ProgressData progress = dfile.getDownloadProgressSource().getProgressData();
        int percent = Math.max(0, Math.min(100, (int) (100F * progress.getProgress())));
        Object activity = progress.getActivity();
        tooltip.append(", ").append(percent).append("% done");
        if (activity != null) {
          addBr(tooltip).append(activity);
        }
      } else if (state == DownloadedFile.State.DOWNLOAD_ERROR) {
        String error = dfile.getLastDownloadError();
        if (error != null) {
          addBr(tooltip).append(error);
        }
      }
    }

    if (tooltip.length() == 0)
      return null;
    tooltip.insert(0, "<html><body>");
    return tooltip.toString();
  }

  public static String getStateString(Attachment attachment, DownloadedFile.State fileState) {
    if (fileState == DownloadedFile.State.UNKNOWN && attachment.isLocal()) return "Not Uploaded";
    else return DownloadedFile.State.getStateString(fileState);
  }

  private static StringBuilder addBr(StringBuilder tooltip) {
    if (tooltip.length() > 0)
      tooltip.append("<br>");
    return tooltip;
  }

  private void repaintItem(String url) {
    if (!myMode.isThumbnailsMode()) myTable.repaintUrl(url);
    else {
      if (url == null || url.length() == 0) myThumbnailView.repaintItems(0, Short.MAX_VALUE);
      else {
        AListModel<? extends T> model = getModel();
        for (int i = 0; i < model.getSize(); i++) {
          T attachment = model.getAt(i);
          if (url.equals(attachment.getUrl())) {
            myThumbnailView.repaintItems(i, i);
          }
        }
      }
    }
  }

  private AListModel<? extends T> getModel() {
    return myThumbnailView.getCollectionModel();
  }

  @Override
  public AttachmentDownloadStatus<T> getDownloadStatus() {
    return myDownloadStatus;
  }

  public Modifiable getAllAttachmentsModifiable() {
    return getModel();
  }

  public void saveAs(File file, Component component) {
    AttachmentUtils.saveAs(file, myTable.getComponent(), getViewConfig());
  }

  public Configuration getViewConfig() {
    return myConfiguration.getOrCreateSubset("view");
  }

  public Modifiable getDownloadedStatusModifiable() {
    return myDownloadStatus.getModifiable();
  }

  @ThreadAWT
  public void repaintAttachment(String url) {
    repaintItem(url);
  }

  @ThreadAWT
  public void revalidateAttachments() {
    Threads.assertAWTThread();
    if (myMode.isThumbnailsMode()) {
      myThumbnailView.revalidate();
    }
  }

  public void addProperty(AttachmentProperty<? super T, ?> property) {
    myProperties.add(property);
  }

  public void setLabelProperty(AttachmentProperty<? super T, ?> property) {
    myThumbnailUI.setLabelProperty(property);
  }

  public void setTooltipProvider(AttachmentTooltipProvider<? super T> provider) {
    myTooltipProvider = provider;
    myTable.setTooltipProvider(provider);
  }

  public void setHighlightPattern(Pattern pattern) {
    myTable.setHighlightPattern(pattern);
  }

  public Collection<? extends Attachment> getAttachments() {
    return myTable.getAttachments();
  }

  public void showAttachment(Attachment attachment, Component parentComponent, AttachmentShowStrategy viewStrat) {
    AttachmentsControllerUtil.downloadAndShowAttachment(this, attachment, parentComponent(parentComponent), getViewConfig(), viewStrat);
  }

  @NotNull
  private Component parentComponent(Component parentComponent) {
    if (parentComponent == null) parentComponent = mySwitchPanel;
    return parentComponent;
  }

  public void setTableMode() {
    myMode.setTableMode();
  }
}
