package com.almworks.engine.gui;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.ModelMap;
import com.almworks.api.application.viewer.Comment;
import com.almworks.api.application.viewer.CommentRenderingHelper;
import com.almworks.api.application.viewer.CommentsController;
import com.almworks.api.application.viewer.UIController;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.Highlightable;
import com.almworks.util.components.layout.WidthDrivenComponent;
import com.almworks.util.components.layout.WidthDrivenComponentAdapter;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.ActionToolbarEntry;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.PresentationMapping;
import com.almworks.util.ui.actions.ToolbarEntry;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class CommentsFormlet<T extends Comment> extends AbstractFormlet implements Highlightable{
  public static final ComponentProperty<CommentsController> COMMENTS_CONTROLLER = ComponentProperty.createProperty("commentsController");
  private final JPanel myComments = new JPanel();
  private final WidthDrivenComponentAdapter myAdapter = new WidthDrivenComponentAdapter(myComments);
  private final ModelKey<? extends Collection<? extends T>> myKey;
  private final CommentsController<T> myController;
  private final List<ToolbarEntry> myActions;

  private int myLastCommentsCount;
  private String myLastCommentText;
  private boolean myVisible;

  public CommentsFormlet(
    ModelKey<? extends Collection<? extends T>> key,
    Configuration config, DataRole<T> dataRole,
    final Comparator<? super T> dateComparator,
    @Nullable final Convertor<Collection<? extends T>, Collection<? extends T>> selector,
    @Nullable CommentRenderingHelper<T> helper, TextDecoratorRegistry decorators)
  {
    super(config);
    myKey = key;
    myController = CommentsController.setup(myComments, key);
    COMMENTS_CONTROLLER.putClientValue(myComments, myController);
    myController.setRendererProperties(false, decorators, helper);
    myController.setTableBorder(false);
    myController.setComparator(dateComparator);
    myController.addGlobalDataRole(dataRole);
    myController.setConfiguration(config);
    if (selector != null)
      myController.setCommentsSelector(selector);
    myActions = Collections15.arrayList();
    myActions.add(new ActionToolbarEntry(CommentsController.ACTION_OLDEST_FIRST, myComments, PresentationMapping.NONAME));
    myActions.add(new ActionToolbarEntry(CommentsController.ACTION_NEWEST_FIRST, myComments, PresentationMapping.NONAME));
//    myActions.add(SeparatorToolbarEntry.JDOM_ADJUSTER);
    myActions.add(new ActionToolbarEntry(CommentsController.ACTION_SHOW_THREAD_TREE, myComments, PresentationMapping.NONAME));
    myActions.add(new ActionToolbarEntry(CommentsController.ACTION_EXPAND_ALL, myComments, PresentationMapping.NONAME));

    UIController.CONTROLLER.putClientValue(myComments, new UIController<JPanel>() {
      public void connectUI(Lifespan lifespan, final ModelMap model, JPanel component) {
        ChangeListener listener = new ChangeListener() {
          public void onChange() {
            Collection<? extends T> comments = myKey.getValue(model);
            Collection<? extends T> shownComments = (selector == null || comments == null) ? comments : selector.convert(comments);
            myLastCommentsCount = shownComments == null ? 0 : shownComments.size();
            T lastComment = null;
            if (myLastCommentsCount > 0) {
              lastComment = Collections.max(shownComments, dateComparator);
            }
            if (lastComment == null) {
              myLastCommentText = null;
            } else {
              myLastCommentText = lastComment.getWhoText();
              if (myLastCommentText == null || myLastCommentText.length() == 0) {
                myLastCommentText = lastComment.getText();
              } else {
                myLastCommentText = "(" + myLastCommentText + ") " + lastComment.getText();
              }
            }
            myVisible = myLastCommentsCount > 0;
            fireFormletChanged();
          }
        };
        model.addAWTChangeListener(lifespan, listener);
        listener.onChange();
        myController.connectUI(lifespan, model, component);
      }
    });
  }

  public CommentsController<T> getController() {
    return myController;
  }

  public String getCaption() {
    return isCollapsed() ? myLastCommentText : null;
  }

  @NotNull
  public WidthDrivenComponent getContent() {
    return myAdapter;
  }

  public boolean isVisible() {
    return myVisible;
  }

  public List<? extends ToolbarEntry> getActions() {
    return isCollapsed() ? null : myActions;
  }

  public void setHighlightPattern(Pattern pattern) {
    myController.setHighlightPattern(pattern);
  }
}
