package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.NameResolver;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.qb.*;
import com.almworks.api.application.tree.UserQueryNode;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.util.ItemKeys;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.dp.DPNotNull;
import com.almworks.util.TypedMap;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Comparing;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.RemoveableModifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.*;
import com.almworks.util.text.parser.*;
import com.almworks.util.threads.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author dyoma
 */
public class BaseEnumConstraintDescriptor extends AbstractConstraintDescriptor
  implements EnumConstraintType, CanvasRenderable
{
  private final DBAttribute myAttribute;
  private final DetachComposite myLife = new DetachComposite();
  private final EnumNarrower myNarrower;

  private final PropertyKey<OrderListModel<ItemKey>, List<ItemKey>> myModelKey;

  private final ItemKey myNotSetItem;

  private final String myDisplayableName;
  private final String myId;

  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private final EnumConstraintKind myKind;
  private final ItemKeyModelCollector<? extends ResolvedItem> myItemKeys;

  private final List<EnumGrouping> myGroupings;
  private final Convertor<ItemKey,String> myFilterConvertor;
  private final Comparator<? super ItemKey> myOrder;
  private final CanvasRenderer<ItemKey> myVariantsRenderer;

  private final AListModel<ItemKey> myUnresolvedModel;

  private final boolean mySearchSubstring;
  private Icon myIcon;

  protected BaseEnumConstraintDescriptor(DBAttribute attribute, EnumNarrower narrower, String displayableName, @Nullable ItemKey notSetObject, EnumConstraintKind kind,
    @Nullable List<EnumGrouping> groupings, Convertor<ItemKey,String> filterConvertor, @Nullable Comparator<? super ItemKey> order, @Nullable CanvasRenderer<ItemKey> variantsRenderer,
    boolean searchSubstring, String id, ItemKeyModelCollector<?> modelCollector)
  {
    myOrder = order;
    myGroupings = groupings;
    myFilterConvertor = filterConvertor;
    myDisplayableName = displayableName;
    myAttribute = attribute;
    myKind = kind;
    myId = id;
    myModelKey = new SubsetKey("subset", myId);
    myNarrower = narrower;
    myItemKeys = modelCollector;
    myNotSetItem = notSetObject;
    myVariantsRenderer = variantsRenderer != null ? variantsRenderer : ItemKey.ICON_NAME_RENDERER;
    mySearchSubstring = searchSubstring;
    myItemKeys.getModel().addChangeListener(myLife, myModifiable);

    myUnresolvedModel = prependNullItem(myLife, myItemKeys.getUnresolvedUniqueModel());
  }

  public void setIcon(Icon icon) {
    myIcon = icon;
  }

  public static PropertyMap createValues(List<ItemKey> subset) {
    PropertyMap values = new PropertyMap();
    values.put(SUBSET, subset);
    return values;
  }

  public void renderOn(Canvas canvas, CellState state) {
    if (myIcon != null)
      canvas.setIcon(myIcon);
    else
      canvas.setIcon(myKind.getIcon());
    canvas.appendText(myDisplayableName);
  }

  public String getDisplayName() {
    return myDisplayableName;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  public EnumConstraintType getType() {
    return this;
  }

  @NotNull
  public ConstraintDescriptor resolve(NameResolver resolver, @Nullable ItemHypercube cube, @Nullable PropertyMap data) {
    // todo #1183
    // todo re-resolve items always -
    if (data != null) {
      List<ItemKey> subset = data.get(SUBSET);
      List<ItemKey> subsetReplacement = null;
      if (subset != null) {
        for (int i = 0; i < subset.size(); i++) {
          ItemKey key = subset.get(i);
          long resolved = key.getResolvedItem();
          if (resolved != 0) {
            if (subsetReplacement != null) {
              subsetReplacement.add(key);
            }
          } else {
            if (subsetReplacement == null) {
              subsetReplacement = Collections15.arrayList();
              if (i > 0) {
                subsetReplacement.addAll(subset.subList(0, i));
              }
            }
            ItemKey notSet = myNotSetItem;
            if (notSet != null && isNullKey(key)) subsetReplacement.add(notSet);
            else {
              boolean resolutionsFound = resolveExisting(key.getId(), cube, subsetReplacement);
              if (!resolutionsFound) {
                // keep unresolved key
                subsetReplacement.add(key);
              }
            }
          }
        }
      }
      if (subsetReplacement == null) {
        List<ItemKey> changed = removeDuplicates(subset);
        if (changed != subset) {
          subsetReplacement = subset;
        }
      } else {
        subsetReplacement = removeDuplicates(subsetReplacement);
      }
      if (subsetReplacement != null) {
        // renaming makes this assertion fire
//        assert Util.equals(getUniqueKeys(SUBSET.getFrom(data)), getUniqueKeys(subsetReplacement)) :
//          "old:" + SUBSET.getFrom(data) + " new:" + getUniqueKeys(subsetReplacement);
        data.replace(SUBSET, subsetReplacement);
      }
    }
    return this;
  }

  public boolean isNullKey(ItemKey key) {
    if (key == null) return false;
    ItemKey notSet = myNotSetItem;
    if (notSet == null) return false;
    long notSetItem = notSet.getResolvedItem();
    long keyItem = key.getResolvedItem();
    if (notSetItem > 0 && keyItem > 0) return keyItem == notSetItem;
    return key.getId().equals(notSet.getId());
  }

  // works on O(n^2), to conserve memory
  static List<ItemKey> removeDuplicates(List<ItemKey> subset) {
    if (subset == null || subset.size() < 2)
      return subset;
    int size = subset.size();
    // check
    boolean filter = false;
    for (int i = 1; i < size; i++) {
      ItemKey key1 = subset.get(i);
      long r1 = key1.getResolvedItem();
      String id1 = key1.getId();
      for (int j = 0; j < i; j++) {
        ItemKey key2 = subset.get(j);
        if (r1 != 0) {
          if (r1 == key2.getResolvedItem()) {
            filter = true;
            break;
          }
        } else {
          if (Util.equals(id1, key2.getId())) {
            filter = true;
            break;
          }
        }
      }
    }
    if (!filter)
      return subset;
    List<ItemKey> result = Collections15.arrayList(subset.size());
    for (int i = 0; i < size; i++) {
      ItemKey key = subset.get(i);
      long r = key.getResolvedItem();
      if (r != 0) {
        // resolved is added if there are no other resolved in the result with the same artifact
        if (!containsArtifact(result, r)) {
          result.add(key);
        }
      } else {
        // unresolved is added if there are no other equal-by-id unresolved
        // but if there's an overriding resolved key, use it instead
        if (!result.contains(key)) {
          // check forward for same-named resolved value
          int found = -1;
          for (int j = i + 1; j < size; j++) {
            ItemKey kj = subset.get(j);
            long rj = kj.getResolvedItem();
            if (rj == 0)
              continue;
            if (key.equals(kj)) {
              // candidate -- check this artifact is not used yet
              if (!containsArtifact(result, rj)) {
                found = j;
                break;
              }
            }
          }
          if (found < 0) {
            result.add(key);
          } else {
            result.add(subset.get(found));
          }
        }
      }
    }
    return result;
  }

  private static boolean containsArtifact(List<ItemKey> result, long r) {
    boolean dupl = false;
    for (ItemKey aResult : result) {
      if (r == aResult.getResolvedItem()) {
        dupl = true;
        break;
      }
    }
    return dupl;
  }

  @Override
  @Nullable
  public ItemKey getMissingItem() {
    return myNotSetItem;
  }

  public RemoveableModifiable getModifiable() {
    return myModifiable;
  }

  @Nullable
  public BoolExpr<DP> createFilter(PropertyMap data, ItemHypercube hypercube) {
    List<ItemKey> subset = data.get(SUBSET);
    if (subset == null)
      return null;
    boolean includeMissing;
    ItemKey missing = getMissingItem();
    List<Long> items = resolve(subset, hypercube);
    if (missing == null) includeMissing = false;
    else if (subset.contains(missing)) includeMissing = true;
    else {
      long missingItem = missing.getResolvedItem();
      includeMissing = missingItem > 0 && items.contains(missingItem);
    }
    BoolExpr<DP> filter = myKind.createFilter(items, myAttribute);
    if (includeMissing) filter = filter.or(DPNotNull.create(myAttribute).negate());
    return filter;
  }

  public DBAttribute getAttribute() {
    return myAttribute;
  }

  public DBAttribute<Long> getParentAttribute() {
    return myKind.getParentAttribute();
  }

  @Nullable
  public Constraint createConstraint(PropertyMap data, ItemHypercube cube) {
    List<ItemKey> subset = data.get(SUBSET);
    if (subset == null)
      return null;
    List<Long> items = resolve(subset, cube);
    return myKind.createConstraint(items, myAttribute);
  }

  public boolean isSameData(PropertyMap data1, PropertyMap data2) {
    return Comparing.areSetsEqual(data1.get(SUBSET), data2.get(SUBSET));
  }

  public CanvasRenderable getPresentation() {
    return this;
  }

  @CanBlock
  public void waitForInitialization() throws InterruptedException {
    myItemKeys.waitForInitialization();
  }

  public PropertyMap getEditorData(PropertyMap data) {
    PropertyMap result = new PropertyMap();
    myModelKey.setInitialValue(result, data.get(SUBSET));
    return result;
  }

  public ConstraintEditor createEditor(ConstraintEditorNodeImpl node) {
    return new EnumAttributeConstraintEditor(getModelKey(), this, node, myOrder, myVariantsRenderer, mySearchSubstring);
  }

  public void writeFormula(FormulaWriter writer, PropertyMap data) {
    doWriteFormula(writer, getId(), data, myKind.getFormulaOperation());
  }

  public void writeFormula(FormulaWriter writer, String conditionId, PropertyMap data) {
    doWriteFormula(writer, conditionId, data, myKind.getFormulaOperation());
  }

  @Override
  public boolean isNotEmpty() {
    return getAllVariantsModel().getSize() > 0;
  }

  public AListModel<ItemKey> getEnumModel(final Lifespan life, ItemHypercube hypercube) {
    AListModel<? extends ResolvedItem> narrowed = getResolvedEnumModel(life, hypercube);
    AListModel<ItemKey> unresolved = ItemKeyModelCollector.createUnresolvedUniqueModel(life, narrowed);
    unresolved = prependNullItem(life, unresolved);
    return unresolved;
  }
  
  @Override
  public AListModel<? extends ResolvedItem> getResolvedEnumModel(final Lifespan life, ItemHypercube hypercube) {
    return myNarrower.narrowModel(life, myItemKeys.getModel(), hypercube);
  }

  @Override
  public List<ResolvedItem> getEnumList(ItemHypercube hypercube) {
    return myNarrower.narrowList((List<ResolvedItem>)myItemKeys.getModel().toList(), hypercube);
  }

  private AListModel<ItemKey> prependNullItem(Lifespan life, AListModel<ItemKey> model) {
    ItemKey notSet = myNotSetItem;
    if (notSet != null && notSet.getResolvedItem() <= 0)
      model = SegmentedListModel.create(life, FixedListModel.create(notSet), model);
    return model;
  }

  public AListModel<ItemKey> getEnumFullModel() {
    return myUnresolvedModel;
  }

  private AListModel<? extends ResolvedItem> getAllVariantsModel() {
    return myItemKeys.getModel();
  }

  public ConstraintDescriptor getDescriptor() {
    return this;
  }

  @NotNull
  public List<ResolvedItem> resolveKey(@Nullable String itemId, ItemHypercube cube) {
    List<ResolvedItem> tempList = Collections15.arrayList();
    resolveExisting(itemId, cube, tempList);
    return tempList;
  }

  @Override
  public boolean isNotSetItem(ItemKey itemKey) {
    return myNotSetItem == null ? itemKey == null : myNotSetItem.equals(itemKey);
  }

  @Nullable
  public String suggestName(String descriptorName, PropertyMap data, Map<TypedKey<?>, ?> hints)
    throws CannotSuggestNameException
  {
    List<? extends ItemKey> enumSubset = data.get(SUBSET);
    if (enumSubset == null)
      return null;
    ItemKey missing = getMissingItem();
    if (enumSubset.isEmpty() && missing != null)
      enumSubset = Collections.singletonList(missing);
    if (enumSubset.isEmpty())
      return null;
    if (enumSubset.size() == 1)
      return enumSubset.get(0).getDisplayName();
    if (UserQueryNode.SINGLE_ENUM_PLEASE.getFrom(hints) == Boolean.TRUE)
      throw new CannotSuggestNameException();
    Integer maxLength = UserQueryNode.MAX_NAME_LENGTH.getFrom(hints);
    if (maxLength == null)
      maxLength = (int) Short.MAX_VALUE;
    StringBuilder buffer = new StringBuilder();
    for (ItemKey key : enumSubset) {
      String dispName = key.getDisplayName();
      if (buffer.length() == 0) {
        buffer.append(dispName);
      } else {
        if (buffer.length() + 2 + dispName.length() < maxLength) {
          buffer.append(", ");
          buffer.append(dispName);
        } else {
          throw new CannotSuggestNameException();
        }
      }
    }
    return buffer.toString();
  }

  public int compareTo(ConstraintDescriptor o) {
    return myId.compareTo(o.getId());
  }

  public List<ResolvedItem> getAllValues(ItemHypercube cube) {
    List<ResolvedItem> allValues = ThreadGate.AWT_IMMEDIATE.compute(new Computable<List<ResolvedItem>>() {
      public List<ResolvedItem> compute() {
        AListModel<? extends ResolvedItem> myEnum = getAllVariantsModel();
        List<ResolvedItem> result = Collections15.arrayList(myEnum.getSize());
        for (int i = 0; i < myEnum.getSize(); i++)
          result.add(myEnum.getAt(i));
        return result;
      }
    });
    return myNarrower.narrowList(allValues, cube);
  }


  public List<ResolvedItem> getAllValues(@NotNull Connection connection) {
    ItemHypercube cube = ItemHypercubeUtils.adjustForConnection(new ItemHypercubeImpl(), connection);
    return getAllValues(cube);
  }

  public void detach() {
    myLife.detach();
  }

  private static void doWriteFormula(FormulaWriter writer, String conditionId, PropertyMap values, String operation) {
    writer.addToken(conditionId);
    writer.addRaw(" " + operation + " (");
    List<ItemKey> subset = values.get(SUBSET);
    if (subset != null) {
      HashSet<String> writtenIds = Collections15.hashSet();
      for (ItemKey itemKey : subset) {
        String id = itemKey.getId();
        if (writtenIds.add(id)) {
          writer.addToken(id);
          writer.addRaw(" ");
        }
      }
    }
    writer.addRaw(")");
  }

  private PropertyKey<OrderListModel<ItemKey>, List<ItemKey>> getModelKey() {
    return myModelKey;
  }

  public static void register(TokenRegistry<FilterNode> registry, final String operation) {
    registry.registerInfixConstraint(operation, new InfixParser<FilterNode>() {
      public FilterNode parse(ParserContext<FilterNode> left, ParserContext<FilterNode> right) throws ParseException {
        ParserContext<FilterNode> context = right.stripBraces();
        List<String> artifactNames = context.getAllTokens();
        String id = left.getSingle();
        return ConstraintFilterNode.parsed(id, unresolved(id, operation),
          createValues(ItemKeys.itemId().collectList(artifactNames)));
      }
    });
  }

  protected static ConstraintType unresolved(final String id, final String operation) {
    return new ConstraintType() {
      @SuppressWarnings({"InnerClassFieldHidesOuterClassField"})
      private final SubsetKey myModelKey = new SubsetKey("subset", id);

      public ConstraintEditor createEditor(ConstraintEditorNodeImpl node) {
        //noinspection ConstantConditions
        return null;
      }

      public void writeFormula(FormulaWriter writer, String conditionId, PropertyMap data) {
        doWriteFormula(writer, conditionId, data, operation);
      }

      public PropertyMap getEditorData(PropertyMap data) {
        PropertyMap result = new PropertyMap();
        myModelKey.setInitialValue(result, data.get(SUBSET));
        return result;
      }

      @Nullable
      public String suggestName(String descriptorName, PropertyMap data, Map<TypedKey<?>, ?> hints)
        throws CannotSuggestNameException
      {
        throw new CannotSuggestNameException();
      }
    };
  }

  protected static void addItems(Collection<ResolvedItem> tempResult, Collection<Long> result) {
    for (ResolvedItem resolvedItem : tempResult) {
      long resolved = resolvedItem.getResolvedItem();
      if (resolved > 0) result.add(resolved);
      else assert false : resolvedItem;
    }
  }

  @ThreadSafe
  private boolean resolveExisting(@Nullable String artifactId, @Nullable ItemHypercube cube,
    Collection<? super ResolvedItem> result)
  {
    return ItemKeys.resolveItemId(artifactId, cube, result, myNarrower, myItemKeys);
  }

  /**
   *
   * @return list of resolved items. Contains zero if keys collection contains nullValue.
   */
  @NotNull
  public List<Long> resolve(Collection<ItemKey> keys, ItemHypercube cube) {
    List<Long> result = Collections15.arrayList();
    List<ResolvedItem> tempResult = Collections15.arrayList();
    for (ItemKey key : keys) {
      long item = key.getResolvedItem();
      if(item != 0) {
        result.add(item);
      } else if (isNullKey(key)) result.add(0l);
      else {
        tempResult.clear();
        resolveExisting(key.getId(), cube, tempResult);
        addItems(tempResult, result);
      }
    }
    return result;
  }

  @NotNull
  public Set<Long> resolveSubtree(ItemKey key, ItemHypercube cube) {
    if (isNullKey(key)) return Collections.singleton(0l);
    if(myKind.getSubtreeKey() == null) {
      return Collections15.hashSet(resolve(Collections.singleton(key), cube));
    }

    final Set<Long> result = Collections15.hashSet();
    final List<ResolvedItem> items = Collections15.arrayList();
    resolveExisting(key.getId(), cube, items);
    addItems(items, result);

    for(final ResolvedItem item : items) {
      if(item instanceof TypedMap) {
        final Set<Long> subtree = ((TypedMap)item).getValue(myKind.getSubtreeKey());
        if(subtree != null) {
          result.addAll(subtree);
        }
      }
    }

    return result;
  }

  public static ConstraintDescriptor unresolvedDescriptor(String id, String operation) {
    return ConstraintDescriptorProxy.stub(id, unresolved(id, operation));
  }

  public static FilterNode createNode(ConstraintDescriptor descriptor, ItemKey value) {
    return createNode(descriptor, Collections.<ItemKey>singletonList(value));
  }

  public static FilterNode createNode(ConstraintDescriptor descriptor, List<ItemKey> enums) {
    PropertyMap values = createValues(enums);
    return new ConstraintFilterNode(descriptor, values);
  }

  @Nullable
  public List<EnumGrouping> getAvailableGroupings() {
    return myGroupings;
  }

  @Override
  public Convertor<ItemKey, String> getFilterConvertor() {
    return myFilterConvertor;
  }

  @Nullable
  @ThreadAWT
  public ResolvedItem findForItem(long item) {
    Threads.assertAWTThread();
    if (item < 0) return null;
    AListModel<? extends ResolvedItem> model = getAllVariantsModel();
    for (int i = model.getSize() - 1; i >= 0; i--) {
      ResolvedItem a = model.getAt(i);
      if (a.getResolvedItem() == item) {
        return a;
      }
    }
    return null;
  }

  public EnumConstraintKind getKind() {
    return myKind;
  }

  public static BaseEnumConstraintDescriptor create(
    DBAttribute attribute, EnumNarrower narrower,
    String displayableName, @Nullable ItemKey notSetItem, EnumConstraintKind kind, List<EnumGrouping> groupings,
    Convertor<ItemKey, String> filterConvertor, @Nullable Comparator<? super ItemKey> order,
    @Nullable CanvasRenderer<ItemKey> variantsRenderer, boolean searchSubstring, ItemKeyModelCollector<?> modelCollector,
    String id)
  {
    return new BaseEnumConstraintDescriptor(attribute, narrower, displayableName, notSetItem, kind, groupings,
      filterConvertor, order, variantsRenderer, searchSubstring, id, modelCollector);
  }

  public static BoolExpr<DP> calcExpr(BoolExpr<DP> variantFilter, DBItemType variantType) {
    BoolExpr<DP> expr;
    if(variantType != null) {
      expr = DPEqualsIdentified.create(DBAttribute.TYPE, variantType);
      if(variantFilter != null) {
        expr = expr.and(variantFilter);
      }
    } else if(variantFilter != null) {
      expr = variantFilter;
    } else {
      throw new IllegalArgumentException();
    }
    return expr;
  }

  public static class SubsetKey extends PropertyKey<OrderListModel<ItemKey>, List<ItemKey>> {
    private final String myId;

    public SubsetKey(String name, String id) {
      super(name, new TypedKeyWithEquality<List<ItemKey>>(name, id));
      myId = id;
    }

    public boolean equals(Object obj) {
      return obj instanceof SubsetKey && myId.equals(((SubsetKey) obj).myId);
    }

    public List<ItemKey> getModelValue(PropertyModelMap properties) {
      OrderListModel<ItemKey> listModel = properties.get(this);
      assert listModel != null : this;
      return listModel.toList();
    }

    public int hashCode() {
      return myId.hashCode();
    }

    public void installModel(final ChangeSupport changeSupport, PropertyModelMap propertyMap) {
      OrderListModel<ItemKey> model = OrderListModel.create();
      propertyMap.put(this, model);
      model.addListener(new AListModel.Adapter() {
        public void onChange() {
          changeSupport.fireChanged(getValueKey(), null, null);
        }
      });
    }

    public ChangeState isChanged(PropertyModelMap models, PropertyMap originalValues) {
      return ChangeState.choose(this, originalValues, getModelValue(models));
    }

    public void setInitialValue(PropertyMap values, List<ItemKey> value) {
      values.put(getValueKey(), value);
    }

    public void setModelValue(PropertyModelMap properties, List<ItemKey> value) {
      OrderListModel<ItemKey> model = properties.get(this);
      assert model != null : this;
      model.clear();
      model.addAll(Collections15.arrayList(value));
    }
  }
}
