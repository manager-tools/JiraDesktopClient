package com.almworks.jira.provider3.comments.gui;

import com.almworks.api.application.ItemKey;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.edit.merge.MergeValue;
import com.almworks.items.gui.edit.merge.SlaveMergeValue;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.jira.provider3.gui.LoadedIssueUtil;
import com.almworks.jira.provider3.gui.edit.editors.VisibilityEditor;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.AdjustedSplitPane;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class ResolveCommentForm {
  public static final DataRole<ResolveCommentForm> ROLE = DataRole.createRole(ResolveCommentForm.class);

  private JPanel myWholePanel;
  private ATextArea myLocalText;
  private ATextArea myRemoteText;
  private JLabel myCommentSummary;
  private ALabel myRemoteInfo;
  private ALabel myResolutionState;
  private JLabel myRemoteVisibleTo;
  private AComboBox<ItemKey> myLocalVisibleTo;
  private final Map<SlaveMergeValue<MergeCommentVersion>, MergeCommentVersion> myResolutions = Collections15.hashMap();
  private SlaveMergeValue<MergeCommentVersion> myCurrentComment = null;
  @Nullable
  private final AList<SlaveMergeValue<MergeCommentVersion>> myAllComments;
  @Nullable
  private final SimpleModifiable mySingleModifiable;

  private ResolveCommentForm(AList<SlaveMergeValue<MergeCommentVersion>> allComments) {
    myAllComments = allComments;
    myLocalText.trackViewportHeight();
    myRemoteText.trackViewportHeight();
    myRemoteInfo.setSizeDelegate(new SizeDelegate() {
      @Override
      public Dimension getMinimumSize(JComponent component, Dimension componentSize) {
        if (componentSize == null)
          return null;
        return new Dimension(20, componentSize.height);
      }

      @Override
      public Dimension getPreferredSize(JComponent component, Dimension componentSize) {
        if (componentSize == null)
          return null;
        return new Dimension(40, componentSize.height);
      }
    });
    ConstProvider.addGlobalValue(myWholePanel, ROLE, this);
    if (myAllComments != null) for (SlaveMergeValue<MergeCommentVersion> comment : myAllComments.getCollectionModel().toList()) initResolution(comment);
    mySingleModifiable = myAllComments == null ?  new SimpleModifiable() : null;
    myLocalVisibleTo.setCanvasRenderer(VisibilityEditor.RENDERER);
  }

  private void showComment(SlaveMergeValue<MergeCommentVersion> comment) {
    saveCurrent();
    myCurrentComment = comment;
    myResolutionState.setVisible(true);
    myLocalText.setEnabled(true);
    myRemoteText.setEnabled(true);
    myLocalVisibleTo.setEnabled(true);
    myCommentSummary.setText(comment.getDisplayName());
    String localText;
    String resolutionText;
    long visibility;
    MergeCommentVersion resolution = myResolutions.get(comment);
    SlaveMergeValue.ResolutionKind resolutionKind = comment.getResolutionKind(resolution);
    if (resolution != null && resolutionKind != SlaveMergeValue.ResolutionKind.UNRESOLVED) {
      visibility = resolution.getVisibility();
      localText = resolution.getText();
      switch (resolutionKind) {
      case DISCARD:
        myLocalText.setEnabled(false);
        myLocalVisibleTo.setEnabled(false);
        resolutionText = "<Discarded>";
        visibility = comment.getVersion(MergeValue.REMOTE).getVisibility();
        break;
      case EDIT: resolutionText = "<Resolved>"; break;
      case COPY_NEW: resolutionText = "<Converted to new comment>"; break;
      case DELETE:
        myLocalText.setEnabled(false);
        myLocalVisibleTo.setEnabled(false);
        resolutionText = "<Deleted>";
        visibility = 0;
        localText = "";
        break;
      default:
        LogHelper.error("Unknown resolution", resolution);
        resolutionText = "<Unresolved>";
      }
    } else {
      MergeCommentVersion local = comment.getVersion(MergeValue.LOCAL);
      localText = local.getText();
      visibility = local.getVisibility();
      resolutionText = "<Unresolved>";
    }
    GuiFeaturesManager manager = EngineConsts.getGuiFeaturesManager(comment.getModel());
    myLocalText.setText(localText);
    myResolutionState.setText(resolutionText);
    myLocalVisibleTo.getModel().setSelectedItem(LoadedIssueUtil.getVisibilityItem(manager, visibility));
    MergeCommentVersion remote = comment.getVersion(MergeValue.REMOTE);
    myRemoteText.setText(remote.getText());
    String visibilityText = LoadedIssueUtil.getVisibilityText(manager, remote.getVisibility());
    if (visibilityText == null) visibilityText = VisibilityEditor.VISIBLE_TO_ALL;
    myRemoteVisibleTo.setText(visibilityText);
    String info = MergeCommentVersion.getRemoteInfo(comment);
    if (info != null && info.length() > 0) {
      myRemoteInfo.setText(info);
      myRemoteInfo.setVisible(true);
    } else myRemoteInfo.setVisible(false);
  }

  private void clearComment() {
    saveCurrent();
    myCurrentComment = null;
    myCommentSummary.setText("");
    myLocalText.setText("");
    myLocalText.setEnabled(false);
    myResolutionState.setVisible(false);
    myRemoteText.setText("");
    myRemoteText.setEnabled(false);
    myRemoteInfo.setVisible(false);
  }

  private void saveCurrent() {
    if (myCurrentComment == null) return;
    myResolutions.put(myCurrentComment, createResolution(myCurrentComment));
  }

  @NotNull
  private MergeCommentVersion createResolution(@NotNull SlaveMergeValue<MergeCommentVersion> comment) {
    MergeCommentVersion prev = myResolutions.get(comment);
    if (prev == null) prev = comment.getVersion(MergeValue.LOCAL);
    else if (prev.isDeleted()) return prev;
    ItemKey visible = myLocalVisibleTo.getModel().getSelectedItem();
    long visibility = visible != null ? visible.getItem() : 0;
    return comment.tryFindSame(MergeCommentVersion.copy(prev, myLocalText.getText(), Math.max(0, visibility)));
  }

  @NotNull
  public static Map<SlaveMergeValue<MergeCommentVersion>, MergeCommentVersion> showWindow(DialogBuilder builder,
    List<SlaveMergeValue<MergeCommentVersion>> comments, Configuration config) {
    if (comments.isEmpty()) return Collections15.emptyMap();
    builder.setTitle(comments.size() == 1 ? "Resolve Comment Conflict" : "Resolve Comments Conflicts");
    builder.setModal(true);
    builder.setEmptyOkAction();
    builder.setEmptyCancelAction();
    builder.addGlobalDataRoot();
    final boolean[] ok = {false};
    builder.addOkListener(new AnActionListener() {
      @Override
      public void perform(ActionContext context) throws CantPerformException {
        ok[0] = true;
      }
    });
    final DetachComposite life = new DetachComposite();
    final Map<SlaveMergeValue<MergeCommentVersion>, MergeCommentVersion> resolutions;
    try {
      boolean single = comments.size() == 1;
      final ResolveCommentForm form = single ? setupSingleComment(life, builder, comments.get(0)) : setupMultiComments(life, builder, comments, config);
      builder.showWindow();
      form.saveCurrent();
      if (single && ok[0]) {
        SlaveMergeValue<MergeCommentVersion> comment = form.getCurrentComment();
        if (form.isUnresolved(comment)) form.setResolveCurrent(MergeValue.LOCAL);
      }
      resolutions = form.myResolutions;
    } finally {
      life.detach();
    }
    return ok[0] ? resolutions : Collections15.<SlaveMergeValue<MergeCommentVersion>, MergeCommentVersion>emptyMap();
  }

  private static ResolveCommentForm setupMultiComments(Lifespan life, DialogBuilder builder, List<SlaveMergeValue<MergeCommentVersion>> comments, Configuration config) {
    final AList<SlaveMergeValue<MergeCommentVersion>> list = new AList<SlaveMergeValue<MergeCommentVersion>>();
    final ResolveCommentForm form = createForm(life, comments.get(0).getModel(), list);
    list.setCollectionModel(FixedListModel.create(comments));
    list.setCanvasRenderer(new CanvasRenderer<SlaveMergeValue<MergeCommentVersion>>() {
      @Override
      public void renderStateOn(CellState state, Canvas canvas, SlaveMergeValue<MergeCommentVersion> item) {
        if (item == null) return;
        if (form.isUnresolved(item)) canvas.setFontStyle(Font.BOLD);
        canvas.appendText(Util.NN(item.getDisplayName()));
      }
    });
    AdjustedSplitPane splitPane = UIUtil.createSplitPane(new JScrollPane(list), form.myWholePanel, true, config, "divider", 150);
    builder.setContent(addToolbar(splitPane, true));
    final SelectionAccessor<SlaveMergeValue<MergeCommentVersion>> selection = list.getSelectionAccessor();
    selection.addAWTChangeListener(Lifespan.FOREVER, new ChangeListener() {
      @Override
      public void onChange() {
        SlaveMergeValue<MergeCommentVersion> comment = selection.getSelection();
        if (form.myCurrentComment == comment) return;
        if (comment == null) form.clearComment();
        else form.showComment(comment);
      }
    });
    selection.ensureSelectionExists();
    return form;
  }

  private static ResolveCommentForm createForm(final Lifespan life, EditItemModel model, @Nullable AList<SlaveMergeValue<MergeCommentVersion>> list) {
    final ResolveCommentForm form = new ResolveCommentForm(list);
    VisibilityEditor.VARIANTS.configure(life, model, new VariantsAcceptor<ItemKey>() {
      @Override
      public void accept(AListModel<? extends ItemKey> variants, @Nullable Configuration recentConfig) {
        form.myLocalVisibleTo.setModel(SelectionInListModel.create(life, SegmentedListModel.create(life, FixedListModel.create((ItemKey)null), variants), null));
      }
    });
    return form;
  }

  private static ResolveCommentForm setupSingleComment(Lifespan life, DialogBuilder builder, SlaveMergeValue<MergeCommentVersion> comment) {
    ResolveCommentForm form = createForm(life, comment.getModel(), null);
    form.initResolution(comment);
    form.showComment(comment);
    builder.setContent(addToolbar(form.myWholePanel, false));
    return form;
  }

  private void initResolution(SlaveMergeValue<MergeCommentVersion> comment) {
    if (comment == null) return;
    MergeCommentVersion resolution = comment.getResolution();
    if (resolution != null) myResolutions.put(comment, resolution);
  }

  private static JComponent addToolbar(JComponent component, boolean navigation) {
    JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(component, BorderLayout.CENTER);
    AToolbar toolbar = new AToolbar();
//    if (navigation) toolbar.addAction(BaseEditComment.RESOLVE_EDIT);
//    toolbar.addAction(BaseEditComment.RESOLVE_DISCARD);
//    toolbar.addAction(BaseEditComment.RESOLVE_NEW);
//    if (navigation) {
//      toolbar.addAction(BaseEditComment.NEXT_FORWARD);
//      toolbar.addAction(BaseEditComment.NEXT_BACKWARD);
//    }
    toolbar.addToNorth(panel);
    return panel;
  }

  public SlaveMergeValue<MergeCommentVersion> getCurrentComment() {
    return myCurrentComment;
  }

  public SlaveMergeValue<MergeCommentVersion> setResolveCurrent(int version) {
    if (myCurrentComment == null) return null;
    MergeCommentVersion resolution;
    if (version != MergeValue.LOCAL) resolution = myCurrentComment.getVersion(version);
    else resolution = createResolution(myCurrentComment);
    myResolutions.put(myCurrentComment, resolution);
    if (myAllComments == null) showComment(myCurrentComment);
    fireCurrentChanged();
    return myCurrentComment;
  }

  private void fireCurrentChanged() {
    if (mySingleModifiable != null) mySingleModifiable.fireChanged();
    else if (myAllComments != null) {
      AListModel<SlaveMergeValue<MergeCommentVersion>> model = myAllComments.getCollectionModel();
      int index = model.indexOf(myCurrentComment);
      if (index >= 0) model.forceUpdateAt(index);
    } else LogHelper.error("Should not happen");
  }

  public List<SlaveMergeValue<MergeCommentVersion>> getAllComments() {
    if (myAllComments == null) return Collections15.emptyList();
    return Collections.unmodifiableList(myAllComments.getCollectionModel().toList());
  }

  public boolean isUnresolved(SlaveMergeValue<MergeCommentVersion> comment) {
    return comment != null && myResolutions.get(comment) == null;
  }

  @Nullable
  public MergeCommentVersion getCurrentResolution() {
    return myResolutions.get(myCurrentComment);
  }

  public void selectComment(SlaveMergeValue<MergeCommentVersion> comment) {
    if (myAllComments == null) return;
    myAllComments.getSelectionAccessor().setSelected(comment);
  }

  public Modifiable getSelectionModifiable() {
    Modifiable modifiable = myAllComments != null ? myAllComments.getSelectionAccessor() : mySingleModifiable;
    LogHelper.assertError(modifiable != null);
    return modifiable;
  }
}
