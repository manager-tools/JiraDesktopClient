package com.almworks.jira.provider3.app;

import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.jira.provider3.attachments.JiraAttachments;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.gui.CascadeSchema;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.gui.table.SubtaskTreeStructure;
import com.almworks.jira.provider3.gui.timetrack.LoadedWorklogLoader;
import com.almworks.jira.provider3.gui.viewer.CommentImplLoader;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.ParentLoader;
import com.almworks.jira.provider3.gui.viewer.links.subtasks.SubtasksLoader;
import com.almworks.jira.provider3.links.JiraLinks;
import com.almworks.jira.provider3.links.structure.IssuesLinkTreeLayout;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.jira.provider3.schema.Status;
import com.almworks.jira.provider3.schema.Version;
import com.almworks.util.properties.Role;

public class JiraFeaturesProvider implements GuiFeaturesManager.Provider {
  public static final Role<JiraFeaturesProvider> ROLE = Role.role(JiraFeaturesProvider.class);

  @Override
  public void registerFeatures(FeatureRegistry registry) {
    Project.registerFeatures(registry);
    MetaSchema.registerFeatures(registry);
    CascadeSchema.registerFeatures(registry);
    CommentImplLoader.registerFeature(registry);
    JiraAttachments.registerFeature(registry);
    JiraLinks.registerFeature(registry);
    LoadedWorklogLoader.registerFeature(registry);
    IssuesLinkTreeLayout.registerFeatures(registry);
    SubtaskTreeStructure.registerFeatures(registry);
    ParentLoader.registerFeature(registry);
    SubtasksLoader.registerFeature(registry);
    CustomFieldsComponent.registerFeature(registry);
    IssueType.registerFeature(registry);
    Status.registerFeature(registry);
    Version.registerFeature(registry);
  }
}
