package com.almworks.items.gui.meta.util;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.items.gui.meta.schema.columns.ColumnComparator;
import com.almworks.items.gui.meta.schema.columns.Columns;
import com.almworks.items.gui.meta.schema.columns.SizePolicies;
import com.almworks.items.gui.meta.schema.export.Exports;
import com.almworks.items.gui.meta.schema.gui.ViewerField;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeys;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public abstract class BaseFieldInfo<C extends BaseFieldInfo<C>> implements FieldInfo {
  private DBIdentity myOwner;
  private String myCommonId;
  private String myColumnId;
  private String myConstraintId;
  private String myDisplayName;
  private ScalarSequence myDataPromotion = null;
  private int myCharCount = 20;
  private ItemDownloadStage myMinComparatorStage;
  private ScalarSequence myComparator;
  private boolean myHideEmptyLeftField = false;
  private boolean myIsMultiline = false;
  private DBIdentity mySecondLevelComparator = null;
  private ScalarSequence myApplicability;
  private ScalarSequence myKeyDataLoader = null;
  private ScalarSequence myConstraintIcon = null;

  protected C self() {
    return (C) this;
  }

  public C setOwner(DBIdentity owner) {
    myOwner = owner;
    return self();
  }

  public C setId(String id) {
    myCommonId = id;
    return self();
  }

  public C setColumnId(String columnId) {
    myColumnId = columnId;
    return self();
  }

  public C setConstraintId(String constraintId) {
    myConstraintId = constraintId;
    return self();
  }

  public C setDisplayName(String displayName) {
    displayName = Util.NN(displayName).trim();
    if (displayName.length() == 0) displayName = null;
    myDisplayName = displayName;
    return self();
  }

  public C setDataPromotion(ScalarSequence dataPromotion) {
    myDataPromotion = dataPromotion;
    return self();
  }

  public C setCharCount(int charCount) {
    myCharCount = charCount;
    return self();
  }

  /**
   * Value comparator sequence - seconds and third arguments of {@link ColumnComparator#l2Comparator(com.almworks.items.sync.util.identity.DBIdentity, com.almworks.api.application.ItemDownloadStage, com.almworks.items.sync.util.identity.ScalarSequence)}
   * @see ColumnComparator
   */
  public C setColumnComparator(@Nullable ItemDownloadStage minStage, ScalarSequence comparator) {
    myMinComparatorStage = minStage;
    myComparator = comparator;
    return self();
  }

  public C setSecondLevelComparator(DBIdentity secondLevelComparator) {
    mySecondLevelComparator = secondLevelComparator;
    return self();
  }

  public C setHideEmptyLeftField(boolean hideEmptyLeftField) {
    myHideEmptyLeftField = hideEmptyLeftField;
    return self();
  }

  public C setMultiline(boolean isMultiline) {
    myIsMultiline = isMultiline;
    return self();
  }

  public C setApplicability(ScalarSequence applicability) {
    myApplicability = applicability;
    return self();
  }

  @Override
  public DBStaticObject createModelKey() {
    return ModelKeys.create(getOwner(), getModelKeyId(), getKeyDataLoader(), myDataPromotion);
  }

  public C overrideKeyDataLoader(ScalarSequence dataLoader) {
    myKeyDataLoader = dataLoader;
    return self();
  }

  public C setConstraintIcon(ScalarSequence constraintIcon) {
    myConstraintIcon = constraintIcon;
    return self();
  }

  private ScalarSequence getKeyDataLoader() {
    if (myKeyDataLoader != null) return myKeyDataLoader;
    return createKeyDataLoader();
  }

  @Override
  public DBStaticObject createExport() {
    ScalarSequence policy = createExportPolicy(createModelKey());
    if (policy == null) return null;
    return Exports.create(getOwner(), getColumnId(), getDisplayName(), policy);
  }

  protected abstract ScalarSequence createExportPolicy(DBStaticObject modelKey);

  protected abstract ScalarSequence createKeyDataLoader();

  protected String getDisplayName() {
    return myDisplayName != null ? myDisplayName : myCommonId;
  }

  protected String getColumnId() {
    return myColumnId != null ? myColumnId : myCommonId;
  }

  protected String getConstraintId() {
    return myConstraintId != null ? myConstraintId : myCommonId;
  }

  protected String getModelKeyId() {
    return getColumnId();
  }

  public DBIdentity getOwner() {
    return myOwner;
  }

  @Nullable
  protected ScalarSequence getApplicability() {
    return myApplicability;
  }

  protected ScalarSequence getConstraintIcon() {
    return myConstraintIcon;
  }

  @Override
  public DBStaticObject createColumn() {
    ScalarSequence comparator = myComparator != null ? ColumnComparator.l2Comparator(mySecondLevelComparator, myMinComparatorStage, myComparator) : null;
    String displayName = getDisplayName();
    return Columns.create(getOwner(), getColumnId(), displayName, displayName, SizePolicies.freeLetterMWidth(myCharCount), Columns.convertModelKeyValue(createModelKey()),
      createRenderer(), comparator, null, null);
  }

  @Override
  public DBStaticObject createViewerField() {
    DBStaticObject modelKey = createModelKey();
    ScalarSequence behaviour = createFieldBehaviour(modelKey, getDisplayName(), myHideEmptyLeftField, myIsMultiline);
    return ViewerField.create(getOwner(), modelKey, behaviour);
  }

  protected abstract ScalarSequence createFieldBehaviour(DBStaticObject modelKey, String displayName, boolean hideEmptyLeftField, boolean isMultiline);

  protected abstract ScalarSequence createRenderer();

  /**
   * Allows subclass instances to be used as prototype. Copies all current value to target.
   * @param target field info to copy values to
   */
  protected void copyPrototypeTo(BaseFieldInfo<C> target) {
    target.myOwner = myOwner;
    target.myCommonId = myCommonId;
    target.myColumnId = myColumnId;
    target.myConstraintId = myConstraintId;
    target.myDisplayName = myDisplayName;
    target.myCharCount = myCharCount;
    target.myComparator = myComparator;
    target.myMinComparatorStage = myMinComparatorStage;
    target.myHideEmptyLeftField = myHideEmptyLeftField;
    target.myIsMultiline = myIsMultiline;
    target.mySecondLevelComparator = mySecondLevelComparator;
    target.myApplicability = myApplicability;
    target.myKeyDataLoader = myKeyDataLoader;
    target.myConstraintIcon = myConstraintIcon;
  }
}
