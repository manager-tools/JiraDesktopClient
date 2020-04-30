package com.almworks.jira.provider3.gui.edit.fields;

import com.almworks.api.misc.WorkArea;
import com.almworks.jira.provider3.gui.edit.ResolvedField;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.exec.Context;
import com.almworks.util.io.IOUtils;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class FieldInfoSetBuilder {
  private final HashMap<ResolvedField,Pair<NameMnemonic,Boolean>> myFields = Collections15.hashMap();

  public void add(ResolvedField field, NameMnemonic fieldName, boolean mandatory) {
    NameMnemonic etcFieldName = getEtcFieldName(field);
    if (etcFieldName != null) {
      String loadedText = fieldName.getText();
      if (Util.equals(loadedText, etcFieldName.getText())) fieldName = etcFieldName;
      else if (etcFieldName.getMnemonicIndex() >= 0) {
        int index = Util.upper(loadedText).indexOf(Character.toUpperCase(etcFieldName.getMnemonicChar()));
        if (index >= 0) fieldName = NameMnemonic.create(loadedText, index);
      }
    }
    myFields.put(field, Pair.create(fieldName, mandatory));
  }

  public FieldInfoSet build() {
    return new FieldInfoSet(Collections15.unmodifiableMapCopy(myFields));
  }

  private static final String FIELD_NAME_FILE = "fieldNames.properties";
  private static final Map<String, NameMnemonic> ETC_FIELD_NAMES = Collections15.hashMap();
  private static boolean ETC_LOADED = false;
  private static NameMnemonic getEtcFieldName(ResolvedField field) {
    synchronized (ETC_FIELD_NAMES) {
      if (!ETC_LOADED) {
        loadEtcFieldNames();
        ETC_LOADED = true;
      }
      return ETC_FIELD_NAMES.get(field.getJiraId());
    }
  }

  private static void loadEtcFieldNames() {
    WorkArea workArea = Context.get(WorkArea.APPLICATION_WORK_AREA);
    if (workArea == null) {
      LogHelper.error("Missing workArea");
      return;
    }
    File file = new File(workArea.getEtcDir(), FIELD_NAME_FILE);
    if (file.isFile() && file.exists()) loadEtcFieldNames(file);
    else {
      file = new File(workArea.getInstallationEtcDir(), FIELD_NAME_FILE);
      if (file.isFile() && file.exists()) loadEtcFieldNames(file);
    }
  }

  private static void loadEtcFieldNames(File file) {
    Properties properties = new Properties();
    FileInputStream stream = null;
    try {
      stream = new FileInputStream(file);
      properties.load(stream);
    } catch (IOException e) {
      LogHelper.error(e);
      return;
    } finally {
      IOUtils.closeStreamIgnoreExceptions(stream);
    }
    for (String propName : properties.stringPropertyNames()) {
      String value = properties.getProperty(propName);
      if (value != null) ETC_FIELD_NAMES.put(propName, NameMnemonic.parseString(value));
    }
  }

  /**
   * @param keepMandatory when true keeps fields mandatory if they are already mandatory.<br>
   *                      when false field left mandatory if it is already mandatory and it is mandatory in merged field set
   */
  public void merge(FieldInfoSet other, boolean keepMandatory) {
    for (Map.Entry<ResolvedField, Pair<NameMnemonic, Boolean>> entry : other.getInfos().entrySet()) {
      ResolvedField field = entry.getKey();
      Pair<NameMnemonic, Boolean> existing = myFields.get(field);
      if (existing == null) myFields.put(field, entry.getValue());
      else {
        boolean alreadyMandatory = existing.getSecond();
        boolean otherMandatory = entry.getValue().getSecond();
        if (alreadyMandatory && !keepMandatory && !otherMandatory) myFields.put(field, Pair.create(existing.getFirst(), false));
      }
    }
  }

  private Boolean otherMandatory(Map.Entry<ResolvedField, Pair<NameMnemonic, Boolean>> entry) {
    return entry.getValue().getSecond();
  }

  public boolean isEmpty() {
    return myFields.isEmpty();
  }
}
