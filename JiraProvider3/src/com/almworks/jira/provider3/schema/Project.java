package com.almworks.jira.provider3.schema;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerProject;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.commons.Condition;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

public class Project {
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerProject.ID);
  public static final DBAttribute<String> KEY = ServerJira.toScalarAttribute(ServerProject.KEY);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerProject.NAME);
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerProject.TYPE);

  public static final DBAttribute<String> DESCRIPTION = ServerJira.toScalarAttribute(ServerProject.DESCRIPTION);
  public static final DBAttribute<String> PROJECT_URL = ServerJira.toScalarAttribute(ServerProject.PROJECT_URL);
  public static final DBAttribute<String> URL = ServerJira.toScalarAttribute(ServerProject.URL);
  public static final DBAttribute<Long> LEAD = ServerJira.toLinkAttribute(ServerProject.LEAD);

  private static final DBIdentity FEATURE_PROJECT_NARROWER = Jira.feature("narrower.project");
  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(KEY)
    .renderFirstNotNull(NAME, KEY, ID)
    .setNarrower(ScalarSequence.create(FEATURE_PROJECT_NARROWER))
    .addAttributeSubloaders(ID)
    .create();
  private static final DBNamespace NS = ServerJira.NS.subNs("project");
  public static final DBAttribute<AttributeMap> SUBTASK_DEFAULTS = NS.attributeMap("subtaskDefaults", "Sub-Task Defaults");

  public static Entity loadEntityForUpload(ItemVersion project) throws LoadEntityException {
    if (project == null || project.getItem() <= 0) throw LoadEntityException.create("Missing project", project);
    Integer id = project.getValue(ID);
    String key = project.getValue(KEY);
    if (id == null || key == null) throw LoadEntityException.create("Missing project identity", project, id, key);
    String name = project.getValue(NAME);
    Entity result = new Entity(ServerProject.TYPE).put(ServerProject.ID, id).put(ServerProject.KEY, key).put(StoreBridge.ITEM_ID, project.getItem());
    if (name != null) result.put(ServerProject.NAME, name);
    return result.fix();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_PROJECT_NARROWER, SerializableFeature.NoParameters.create(Narrower.INSTANCE, LoadedEnumNarrower.class));
  }

  @Nullable
  public static Long resolveById(ItemVersion connection, int projectId) {
    return Jira.resolveEnum(connection, DB_TYPE, ID, projectId);
  }

  private static class Narrower implements LoadedEnumNarrower {
    public static final LoadedEnumNarrower INSTANCE = new Narrower();

    @Override
    public void collectIssueAttributes(Collection<? super DBAttribute<Long>> target) {
      target.add(SyncAttributes.CONNECTION);
    }

    @Override
    public boolean isAccepted(ItemHypercube cube, LoadedItemKey value) {
      return LoadedEnumNarrower.DEFAULT.isAccepted(cube, value) && isAcceptedInOwnConnection(value);
    }

    @Override
    public boolean isAllowedValue(ItemVersion issue, ItemVersion value) {
      return LoadedEnumNarrower.DEFAULT.isAllowedValue(issue, value); // Filtered out projects are still valid as value so filter only by connection
    }

    private boolean isAcceptedInOwnConnection(LoadedItemKey value) {
      JiraConnection3 connection = value.getConnection(JiraConnection3.class);
      if (connection == null) return false;
      Set<Integer> filter = connection.getProjectsFilter();
      if (filter == null) return true;
      Integer id = value.getValue(ID);
      return filter.contains(id);
    }

    @Override
    public void collectValueAttributes(Collection<? super DBAttribute<?>> target) {
      target.add(ID);
    }

    @Override
    public <T extends ItemKey> AListModel<T> narrowModel(Lifespan life, AListModel<T> original, final ItemHypercube cube) {
      final SortedSet<Long> connections = cube.getIncludedValues(SyncAttributes.CONNECTION);
      final FilteringListDecorator<T> result = FilteringListDecorator.create(life, original);
      ThreadGate.AWT.execute(new Runnable() {
        public void run() {
          result.setFilter(new Condition<ItemKey>() {
            public boolean isAccepted(ItemKey value) {
              return Narrower.this.isAccepted(Util.castNullable(LoadedItemKey.class, value), connections) != null;
            }
          });
        }
      });
      return result;
    }

    @Override
    public <I extends ResolvedItem> List<I> narrowList(List<I> values, ItemHypercube cube) {
      SortedSet<Long> connections = cube.getIncludedValues(SyncAttributes.CONNECTION);
      List<I> result = Collections15.arrayList();
      for (I value : values) {
        I loaded = isAccepted(value, connections);
        if (loaded != null) result.add(loaded);
      }
      return result;
    }

    private <I extends ResolvedItem> I isAccepted(@Nullable I value, @Nullable SortedSet<Long> connections) {
      LoadedItemKey loaded = Util.castNullable(LoadedItemKey.class, value);
      return loaded != null && !(connections != null && !connections.contains(loaded.getConnectionItem())) &&
        isAcceptedInOwnConnection(loaded) ? value : null;
    }
  }
}
