package com.almworks.jira.provider3.gui.edit.workflow;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.ImageSliceEvent;
import com.almworks.items.cache.QueryImageSlice;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.jira.provider3.schema.WorkflowActionSet;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.properties.Role;
import org.almworks.util.detach.DetachComposite;
import org.picocontainer.Startable;

public class WLActionSets implements Startable {
  public static final Role<WLActionSets> ROLE = Role.role(WLActionSets.class);

  private static final AttributeLoader<Long> PROJECT = AttributeLoader.create(WorkflowActionSet.PROJECT);
  private static final AttributeLoader<Long> STATUS = AttributeLoader.create(WorkflowActionSet.STATUS);
  private static final AttributeLoader<Long> ISSUE_TYPE = AttributeLoader.create(WorkflowActionSet.ISSUE_TYPE);

  private final DetachComposite myLife = new DetachComposite();
  private final QueryImageSlice mySlice;
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  public WLActionSets(DBImage image) {
    mySlice = image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, WorkflowActionSet.DB_TYPE));
    mySlice.addData(PROJECT, STATUS, ISSUE_TYPE, WorkflowActionSet.ACTIONS);
  }

  @Override
  public void start() {
    mySlice.ensureStarted(myLife);
    mySlice.addListener(myLife, new ImageSlice.Listener() {
      @Override
      public void onChange(ImageSliceEvent event) {
        myModifiable.fireChanged();
      }
    });
  }

  @Override
  public void stop() {
    myLife.detach();
  }

  public SimpleModifiable getModifiable() {
    return myModifiable;
  }

  public boolean isApplicable(long action, long project, long type, long status) {
    if (project == 0 || type == 0 || status == 0) return false;
    int index = 0;
    boolean semiFound = false;
    while (index >= 0) {
      index = mySlice.findIndexByValue(index, PROJECT, project, STATUS, status);
      if (index < 0) return false;
      long item = mySlice.getItem(index);
      if (mySlice.getValue(item, ISSUE_TYPE) == type) {
        LongList actions = mySlice.getValue(item, WorkflowActionSet.ACTIONS);
        return actions.contains(action);
      }
      semiFound = true;
      index++;
    }
    return semiFound;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  private void debugPrint(long project) {
    System.out.println("For PROJECT: " + project);
    System.out.println("TYPE   STATUS  :  ACTIONS");
    for (int ii = mySlice.findIndexByValue(0, PROJECT, project); ii >= 0; ii = mySlice.findIndexByValue(ii + 1, PROJECT, project)) {
      long setItem = mySlice.getItem(ii);
      System.out.println(mySlice.getValue(setItem, ISSUE_TYPE) + " " + mySlice.getValue(setItem, STATUS) + " : " + mySlice.getValue(setItem, WorkflowActionSet.ACTIONS));
    }
  }
}
