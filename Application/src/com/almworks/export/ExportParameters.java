package com.almworks.export;

import com.almworks.api.application.util.ItemExport;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;

public class ExportParameters extends PropertyMap {
  private final LinkedHashSet<ItemExport> myKeys = Collections15.linkedHashSet();
  private Exporter myExporter;
  private NumberFormat myNumberFormat = null;
  private DateFormat myDateFormat = null;
  private Locale myLocale = null;

  public Exporter getExporter() {
    return myExporter;
  }

  public void setExporter(Exporter exporter) {
    myExporter = exporter;
  }

  /**
   * @return predictable order set
   */
  @NotNull
  public Set<ItemExport> getKeys() {
    return Collections.unmodifiableSet(myKeys);
  }

  void setKeys(List<ItemExport> keys) {
    myKeys.clear();
    myKeys.addAll(keys);
    fireChanged();
  }

  public NumberFormat getNumberFormat() {
    return myNumberFormat;
  }

  void setNumberFormat(NumberFormat numberFormat) {
    myNumberFormat = numberFormat;
    fireChanged();
  }

  public DateFormat getDateFormat() {
    return myDateFormat;
  }

  void setDateFormat(DateFormat format) {
    myDateFormat = format;
    fireChanged();
  }

  public int getKeysCount() {
    return myKeys.size();
  }

  public Locale getLocale() {
    return myLocale;
  }

  public void setLocale(Locale locale) {
    myLocale = locale;
    fireChanged();
  }

  public boolean getBoolean(TypedKey<Boolean> key) {
    Boolean r = get(key);
    return r != null && r;
  }
}
