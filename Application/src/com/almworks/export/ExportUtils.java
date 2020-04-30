package com.almworks.export;

import com.almworks.export.csv.CSVParams;
import com.almworks.util.English;
import com.almworks.util.files.FileActions;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.EnabledAction;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ExportUtils {

  private ExportUtils() {}

  public static String getTotals(List<ExportedData.ArtifactRecord> records) {
    StringBuffer buf = new StringBuffer();
    Map<String, Integer> typeCounters = Collections15.linkedHashMap();
    for (ExportedData.ArtifactRecord record : records) {
      String typeId = record.getDisplayableType();
      Integer c = typeCounters.get(typeId);
      typeCounters.put(typeId, c == null ? 1 : c + 1);
    }
    for (Map.Entry<String, Integer> entry : typeCounters.entrySet()) {
      String type = entry.getKey();
      Integer count = entry.getValue();
      if (buf.length() > 0)
        buf.append(", ");
      buf.append(count).append(' ').append(English.getSingularOrPlural(Util.lower(type), count));
    }
    return buf.toString();
  }

  public static List<AnAction> createDoneActions(PropertyMap parameters) {
    List<AnAction> actions = Collections15.arrayList();
    final File file = parameters.get(CSVParams.TARGET_FILE);
    if (file != null) {
      if (FileActions.isSupported(FileActions.Action.OPEN)) {
        actions.add(new EnabledAction("&Open") {
          protected void doPerform(ActionContext context) {
            FileActions.openFile(file, null);
          }
        });
      }
      if (FileActions.isSupported(FileActions.Action.OPEN_AS)) {
        actions.add(new EnabledAction("Open &As\u2026") {
          protected void doPerform(ActionContext context) {
            FileActions.openAs(file, null);
          }
        });
      }
      if (FileActions.isSupported(FileActions.Action.OPEN_CONTAINING_FOLDER)) {
        actions.add(new EnabledAction(FileActions.OPEN_FOLDER_TITLE) {
          protected void doPerform(ActionContext context) {
            FileActions.openContainingFolder(file, null);
          }
        });
      }
    }
    return actions;
  }
}
