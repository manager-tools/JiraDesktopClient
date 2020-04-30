package com.almworks.jira.provider3.gui.viewer;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.viewer.Comment;
import com.almworks.api.application.viewer.LinksEditorKit;
import com.almworks.api.application.viewer.textdecorator.TextDecoratorRegistry;
import com.almworks.api.config.MiscConfig;
import com.almworks.engine.gui.BaseTextController;
import com.almworks.engine.gui.CommentsFormlet;
import com.almworks.engine.gui.LinkTextFormlet;
import com.almworks.engine.gui.TextController;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.util.DefaultItemViewer;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.attachments.AttachmentsFormlet2;
import com.almworks.jira.provider3.attachments.JiraAttachments;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.actions.JiraActions;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.SubtasksFormlet;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.util.components.layout.WidthDrivenColumn;
import com.almworks.util.config.Configuration;

import javax.swing.*;
import java.util.List;

public class IssueViewer3 extends DefaultItemViewer {

  private final TextDecoratorRegistry myDecorators;
  private final MiscConfig myGlobalConfig;

  public IssueViewer3(Configuration config, TextDecoratorRegistry decorators, GuiFeaturesManager features, MiscConfig globalConfig) {
    super(config, features, MetaSchema.KEY_KEY, MetaSchema.KEY_SUMMARY);
    myDecorators = decorators;
    myGlobalConfig = globalConfig;
  }

  protected void addRightSideFormlets(WidthDrivenColumn column, Configuration settings) {
    addFormlet(column, "Sub-Tasks", new SubtasksFormlet(getFeatures(), settings.getOrCreateSubset("subtasks"), false), 1);
    addFormlet(column, "Parent Task", new SubtasksFormlet(getFeatures(), settings.getOrCreateSubset("parentTask"), true), 1);

    AttachmentsFormlet2<?> formlet = JiraAttachments.createFormlet(getFeatures(), settings.getOrCreateSubset("attachments"), myGlobalConfig);
    addHighlightable(formlet);
    addFormlet(column, "Attachments", formlet, 2);

    LinkTextFormlet envFormlet = linkTextFormlet(settings, MetaSchema.KEY_ENVIRONMENT);
    addFormlet(column, "Environment", envFormlet, 3);
    addHighlightable(envFormlet);

    addFormlet(column, "Links", JiraLinks.createFormlet(getFeatures(), settings.getOrCreateSubset("links")), 4);

    LinkTextFormlet descriptionFormlet = linkTextFormlet(settings, MetaSchema.KEY_DESCRIPTION);
    addFormlet(column, "Description", descriptionFormlet, 5);
    addHighlightable(descriptionFormlet);

    column.addComponent(createRightFieldFormlets(settings));

    ModelKey<List<CommentImpl>> commentsKey = CommentImpl.getModelKey(getFeatures());
    CommentsFormlet<CommentImpl> commentCommentsFormlet = createCommentsFormlet(settings, commentsKey);
    addFormlet(column, "Comments", commentCommentsFormlet, 6);
    addHighlightable(commentCommentsFormlet);

    WorklogFormlet worklogFormlet = new WorklogFormlet(settings, getFeatures());
    addFormlet(column, "Work Log", worklogFormlet, 7);
  }

  private CommentsFormlet<CommentImpl> createCommentsFormlet(Configuration settings, ModelKey<List<CommentImpl>> commentsKey) {
    CommentsFormlet<CommentImpl> formlet = new CommentsFormlet<CommentImpl>(commentsKey,
      settings.getOrCreateSubset("comments"), CommentImpl.DATA_ROLE, Comment.DATE_COMPARATOR, null,
      CommentImpl.COMMENT_RENDERING_HELPER, myDecorators);
    formlet.getController()
      .addMenuAction(JiraActions.ADD_COMMENT, false)
      .addMenuAction(JiraActions.REPLY_TO_COMMENT, false)
      .addMenuAction(JiraActions.EDIT_COMMENT, false)
      .addMenuAction(JiraActions.DELETE_COMMENT, false);
    return formlet;
  }

  private LinkTextFormlet linkTextFormlet(Configuration settings, DBStaticObject keyId) {
    ModelKey<String> modelKey = getFeatures().findScalarKey(keyId, String.class);
    JEditorPane component = new JEditorPane();
    component.setEditorKit(LinksEditorKit.create(myDecorators, false));
    BaseTextController<String> controller = TextController.humanTextViewer(modelKey);
    TextController.installController(component, controller);
    return new LinkTextFormlet(component, controller, settings.getOrCreateSubset(modelKey.getName()));
  }
}
