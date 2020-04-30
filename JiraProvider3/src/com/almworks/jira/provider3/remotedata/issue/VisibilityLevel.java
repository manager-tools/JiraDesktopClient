package com.almworks.jira.provider3.remotedata.issue;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.jira.provider3.schema.Group;
import com.almworks.jira.provider3.schema.ProjectRole;
import com.almworks.jira.provider3.sync.schema.ServerGroup;
import com.almworks.jira.provider3.sync.schema.ServerProjectRole;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

public class VisibilityLevel {
  private final boolean myRole;
  private final String myId;

  private VisibilityLevel(boolean role, String id) {
    myRole = role;
    myId = id;
  }

  @Nullable("When no visibility or illegal item (internal error)")
  public static VisibilityLevel load(@Nullable ItemVersion item) {
    if (item == null || item.getItem() <= 0) return null;
    boolean isGroup = item.equalValue(DBAttribute.TYPE, Group.DB_TYPE);
    boolean isRole = item.equalValue(DBAttribute.TYPE, ProjectRole.DB_TYPE);
    if (isGroup) return create(false, item.getValue(Group.ID));
    else if (isRole) return create(true, item.getValue(ProjectRole.NAME));
    else {
      LogHelper.error("Wrong security", item);
      return null;
    }
  }

  @Nullable("When illegal data")
  private static VisibilityLevel create(boolean isRole, String id) {
    if (id == null || id.isEmpty()) return null;
    return new VisibilityLevel(isRole, id);
  }

  @SuppressWarnings("SimplifiableIfStatement")
  public boolean isSame(@Nullable EntityHolder visibility) {
    if (visibility == null) return false;
    if (!(myRole ? ServerProjectRole.TYPE : ServerGroup.TYPE).equals(visibility.getItemType())) return false;
    return myId.equals(visibility.getScalarValue(myRole ? ServerProjectRole.NAME : ServerGroup.ID));
  }

  @SuppressWarnings("unchecked")
  public JSONObject createJson() {
    JSONObject result = new JSONObject();
    result.put("type", myRole ? "role" : "group");
    result.put("value", myId);
    return result;
  }

  public static boolean areSame(EntityHolder entity, VisibilityLevel visibility) {
    if (entity == null) return visibility == null;
    return visibility != null && visibility.isSame(entity);
  }
}
