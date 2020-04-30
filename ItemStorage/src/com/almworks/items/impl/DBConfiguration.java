package com.almworks.items.impl;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DP;
import com.almworks.items.api.ItemReference;
import com.almworks.items.dp.*;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.dbadapter.DBColumnType;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.migrations.DBMigrationProcedure;
import com.almworks.items.impl.migrations.ObfuscatedAttributeMap;
import com.almworks.items.impl.scalars.*;
import com.almworks.items.impl.sqlite.SQLUtil;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.items.impl.sqlite.filter.*;
import org.almworks.util.Collections15;
import org.almworks.util.Log;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DBConfiguration {
  private final Map<Class, ScalarValueAdapter> myValueAdapters = Collections15.hashMap();
  private final List<ExtractionOperatorFactory> myExtractionFactories =
    new CopyOnWriteArrayList<ExtractionOperatorFactory>();
  private final List<ExtractionOperatorFactory> myExtractionConvertorsRO =
    Collections.unmodifiableList(myExtractionFactories);
  private final List<DBTriggerCounterpart> myTriggers = new CopyOnWriteArrayList<DBTriggerCounterpart>();
  private final List<DBMigrationProcedure> myMigrations = new CopyOnWriteArrayList<DBMigrationProcedure>();

  private File myProfileBaseFile;

  public void registerScalarValueAdapter(ScalarValueAdapter<?> adapter) {
    synchronized (myValueAdapters) {
      Class<?> cls = adapter.getAdaptedClass();
      ScalarValueAdapter oldAdapter = myValueAdapters.get(cls);
      if (oldAdapter != null) {
        if (!oldAdapter.equals(adapter)) {
          Log.warn("cannot register SVA [" + adapter + "], class " + cls + " is already served by " + oldAdapter);
        }
        return;
      }
      myValueAdapters.put(cls, adapter);
    }
  }

  public <T> ScalarValueAdapter<T> getScalarValueAdapter(Class<T> scalarClass) {
    synchronized (myValueAdapters) {
      return myValueAdapters.get(scalarClass);
    }
  }

  public void registerExtractionFactory(ExtractionOperatorFactory factory) {
    if (!myExtractionFactories.contains(factory)) {
      myExtractionFactories.add(factory);
    }
  }

  public List<ExtractionOperatorFactory> getFilterConvertors() {
    return myExtractionConvertorsRO;
  }

  public static DBConfiguration createDefault(File databaseFile) {
    DBConfiguration r = new DBConfiguration();
    r.registerStandardScalarAdapters();
    r.registerStandardFilterConvertors();
    r.registerStandardMigrations();
    if ("true".equalsIgnoreCase(System.getProperty("profile.sql"))) {
      r.setProfileBaseFile(new File(databaseFile.getParent(), "profile.txt"));
    }
    // todo add migrations
    return r;
  }

  private void registerStandardFilterConvertors() {
    registerExtractionFactory(new ExtractionFactoryDPNotNull());
    registerExtractionFactory(new ExtractionFactoryDPEquals());
    registerExtractionFactory(new ExtractionFactoryDPEqualsIdentified());
    registerExtractionFactory(new ExtractionFactoryDPIntersects());
    registerExtractionFactory(new ExtractionFactoryDPIntersectsIdentified());
    registerExtractionFactory(new ExtractionFactoryDPReferredBy());
    registerExtractionFactory(new ExtractionFactoryDPCompare());
    // todo other standard DP
    registerExtractionFactory(new DefaultExtractionFactory());
  }

  private void registerInjectionConvertor(Class<? extends DP> predicateClass,
    Class<? extends ExtractionOperator> operatorClass)
  {
    try {
      registerExtractionFactory(new InjectionExtractionFactory(predicateClass, operatorClass));
    } catch (NoSuchMethodException e) {
      Log.error("cannot register convertor for " + predicateClass, e);
    }
  }

  public void registerStandardScalarAdapters() {
    registerScalarValueAdapter(new ScalarAdapterLong());
    registerScalarValueAdapter(new ScalarAdapterInteger());
    registerScalarValueAdapter(new ScalarAdapterString());
    registerScalarValueAdapter(new ScalarAdapterAttributeMap());
    registerScalarValueAdapter(new ScalarAdapterBoolean());
    registerScalarValueAdapter(new ScalarAdapterByteArray());
    registerScalarValueAdapter(new ScalarAdapterDecimal());
    registerScalarValueAdapter(new ScalarAdapterDate());
    registerScalarValueAdapter(new ScalarAdapterLongList());
    registerScalarValueAdapter(new ScalarAdapterCharacter());
  }

  public void registerStandardMigrations() {
    myMigrations.add(new ObfuscatedAttributeMap());
  }

  public Collection<DBMigrationProcedure> getMigrations() {
    return Collections.unmodifiableList(myMigrations);
  }

  public boolean isProfilingEnabled() {
    return myProfileBaseFile != null;
  }

  public File getProfileBaseFile() {
    return myProfileBaseFile;
  }

  public void setProfileBaseFile(File profileBaseFile) {
    myProfileBaseFile = profileBaseFile;
  }

  public void addTrigger(DBTriggerCounterpart trigger) {
    myTriggers.add(trigger);
  }

  public List<DBTriggerCounterpart> getTriggers() {
    return myTriggers;
  }

  private static ExtractionOperator equalsOneOf(AttributeAdapter adapter, boolean negated, boolean separateJoin, Collection values) {
    if (values == null)
      return null;
    ScalarValueAdapter scalarAdapter = adapter.getScalarAdapter();
    List<Object> databaseObjects = Collections15.arrayList(values.size());
    DBColumn column = adapter.getScalarColumn();
    boolean integer = column.getDatabaseClass() == DBColumnType.INTEGER;
    for (Object value : values) {
      Object v = scalarAdapter.toSearchValue(value);
      if (v == null)
        return null;
      databaseObjects.add(v);
      if (!integer && databaseObjects.size() > SQLUtil.MAX_SQL_PARAMS) {
        // too many parameters for SQL
        return null;
      }
    }
    WhereBuilder where;
    if (databaseObjects.isEmpty()) {
      where = null;
    } else if (databaseObjects.size() == 1) {
      where = new WhereBuilder.Equals(column, databaseObjects.get(0));
    } else {
      where = new WhereBuilder.EqualsOneOf(column, databaseObjects);
    }
    return new TableFilteringOperator(adapter.getTable(), negated, separateJoin, where);
  }

  private static class ExtractionFactoryDPNotNull extends TypedExtractionFactory<DPNotNull> {
    public ExtractionFactoryDPNotNull() {
      super(DPNotNull.class);
    }

    @Override
    protected ExtractionOperator convertTyped(DPNotNull predicate, boolean negated, TransactionContext trContext) {
      DBTable table = trContext.getDatabaseContext().getAttributeAdapter(predicate.getAttribute()).getTable();
      return new TableJoinOperator(table, negated);
    }
  }


  private static class ExtractionFactoryDPEquals extends TypedExtractionFactory<DPEquals> {
    public ExtractionFactoryDPEquals() {
      super(DPEquals.class);
    }

    @Override
    protected ExtractionOperator convertTyped(DPEquals predicate, boolean negated, TransactionContext trContext) {
      DBAttribute attribute = predicate.getAttribute();
      // only scalars
      if (attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR)
        return null;
      AttributeAdapter adapter = trContext.getDatabaseContext().getAttributeAdapter(attribute);

      List values = predicate.getValues();
      if (values.isEmpty()) return null;
      return equalsOneOf(adapter, negated, false, values);
    }
  }

  private static class ExtractionFactoryDPEqualsIdentified extends TypedExtractionFactory<DPEqualsIdentified> {
    public ExtractionFactoryDPEqualsIdentified() {
      super(DPEqualsIdentified.class);
    }

    @Override
    protected ExtractionOperator convertTyped(DPEqualsIdentified predicate, boolean negated,
      TransactionContext trContext)
    {
      DBAttribute attribute = predicate.getAttribute();
      // only scalars
      if (attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR)
        return null;
      final AttributeAdapter adapter = trContext.getDatabaseContext().getAttributeAdapter(attribute);
      final ItemReference object = predicate.getValue();
      if (object == null)
        return null;
      final DBColumn column = adapter.getScalarColumn();

      return new TableBasedOperator(adapter.getTable(), negated) {
        @Override
        protected WhereBuilder where(TransactionContext context) {
          long item = object.findItem(new DBReaderImpl(context));
          if (item == 0)
            return null;
          return new WhereBuilder.Equals(column, item);
        }
      };
    }
  }


  private static class ExtractionFactoryDPIntersects extends TypedExtractionFactory<DPIntersects> {
    public ExtractionFactoryDPIntersects() {
      super(DPIntersects.class);
    }

    @Override
    protected ExtractionOperator convertTyped(DPIntersects predicate, boolean negated,
      TransactionContext trContext)
    {
      if (negated) {
        // supporting negated INTERSECTS with SQL would require a rather complex SQL
        return null;
      }
      DBAttribute attribute = predicate.getAttribute();
      AttributeAdapter adapter = trContext.getDatabaseContext().getAttributeAdapter(attribute);
      Set values = predicate.getValues();
      return equalsOneOf(adapter, negated, true, values);
    }
  }


  private static class ExtractionFactoryDPIntersectsIdentified extends TypedExtractionFactory<DPIntersectsIdentified> {
    public ExtractionFactoryDPIntersectsIdentified() {
      super(DPIntersectsIdentified.class);
    }

    @Override
    protected ExtractionOperator convertTyped(DPIntersectsIdentified predicate, boolean negated,
      TransactionContext trContext)
    {
      if (negated) {
        // supporting negated INTERSECTS with SQL would require a rather complex SQL
        return null;
      }
      DBAttribute attribute = predicate.getAttribute();
      AttributeAdapter adapter = trContext.getDatabaseContext().getAttributeAdapter(attribute);
      final Set<DBIdentifiedObject> values = predicate.getValues();
      if (values == null)
        return null;

      final DBColumn column = adapter.getScalarColumn();
      if (column.getDatabaseClass() != DBColumnType.INTEGER) {
        Log.error("intersect query based on non-integer column " + column);
        return null;
      }

      return new TableBasedOperator(adapter.getTable(), negated, true) {
        @Override
        protected WhereBuilder where(TransactionContext context) {
          LongArray array = new LongArray(values.size());
          DBReaderImpl reader = new DBReaderImpl(context);
          for (DBIdentifiedObject value : values) {
            long m = reader.findMaterialized(value);
            if (m != 0)
              array.add(m);
          }
          if (array.isEmpty())
            return null;
          else if (array.size() == 1)
            return new WhereBuilder.Equals(column, array.get(0));
          else
            // todo fix converting parameters from LongArray into List<Long>, while WhereBuilder will convert them back to LongList
            return new WhereBuilder.EqualsOneOf(column, array.toList());
        }
      };
    }
  }

  private static class ExtractionFactoryDPReferredBy extends TypedExtractionFactory<DPReferredBy> {
    public ExtractionFactoryDPReferredBy() {
      super(DPReferredBy.class);
    }

    @Override
    protected ExtractionOperator convertTyped(DPReferredBy predicate, boolean negated,
      TransactionContext trContext)
    {
      DBAttribute<Long> attribute = predicate.getReferenceAttribute();
      AttributeAdapter adapter = trContext.getDatabaseContext().getAttributeAdapter(attribute);
      return new JoinSubqueryOperator(adapter.getTable(), adapter.getScalarColumn(), predicate.getRefereeQuery(),
        negated, trContext);
    }
  }

  private static class ExtractionFactoryDPCompare extends TypedExtractionFactory<DPCompare> {
    private ExtractionFactoryDPCompare() {
      super(DPCompare.class);
    }

    @Override
    protected ExtractionOperator convertTyped(DPCompare predicate, boolean negated, TransactionContext trContext) {
      DBAttribute attribute = predicate.getAttribute();
      AttributeAdapter adapter = trContext.getDatabaseContext().getAttributeAdapter(attribute);
      boolean less = predicate.isLess();
      boolean equal = predicate.isAcceptEqual();
      boolean acceptNull = predicate.isAcceptNull();
      if (negated) {
        less = !less;
        equal = !equal;
        acceptNull = !acceptNull;
      }
      ScalarValueAdapter scalarAdapter = adapter.getScalarAdapter();
      Object value = scalarAdapter.toSearchValue(predicate.getValue());
      DBTable table = adapter.getTable();
      if (value == null) return acceptNull ? new TableJoinOperator(table, true) : null;
      if (attribute.getScalarClass() == BigDecimal.class) return null; // would compare as strings
      WhereBuilder.Compare where =
        new WhereBuilder.Compare(adapter.getScalarColumn(), value, less, equal, acceptNull);
      return new TableFilteringOperator(table, acceptNull, false, where);
    }
  }
}
