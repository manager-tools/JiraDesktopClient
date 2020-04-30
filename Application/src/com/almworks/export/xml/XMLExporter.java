package com.almworks.export.xml;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.install.Setup;
import com.almworks.api.platform.ProductInformation;
import com.almworks.export.ExportParameters;
import com.almworks.export.ExportedData;
import com.almworks.export.ExporterDescriptor;
import com.almworks.export.WriterExporterHelper;
import com.almworks.util.Pair;
import com.almworks.util.config.Configuration;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XMLExporter extends WriterExporterHelper {
  private final ProductInformation myProductInformation;
  private final Map<ItemExport, String> myTagCache = Collections15.hashMap();

  public XMLExporter(ExporterDescriptor descriptor, DialogManager dialogManager, Configuration configuration,
    ProductInformation productInformation)
  {
    super(descriptor, new XMLParametersForm(configuration), dialogManager);
    myProductInformation = productInformation;
  }

  protected void writeData(PrintWriter out, ExportedData data, ExportParameters parameters) {
    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    out.println("<export>");
    writeExportInfo(out, data, parameters);
    writeIssues(out, data, parameters);
    out.println("</export>");
  }

  private void writeIssues(PrintWriter out, ExportedData data, ExportParameters parameters) {
    out.println("  <issues>");
    Set<ItemExport> keys = parameters.getKeys();
    DateFormat dateFormat = parameters.getDateFormat();
    NumberFormat numberFormat = parameters.getNumberFormat();
    List<ExportedData.ArtifactRecord> records = data.getRecords();
    int size = records.size();
    double step = size == 0 ? 0F : 1F / size;
    int count = 0;
    ExportContext context = new ExportContext(numberFormat, dateFormat, false);
    for (ExportedData.ArtifactRecord record : records) {
      if (myCancelled.get()) {
        myProgress.addError("Export cancelled");
        break;
      }

      out.println("    <issue>");
      for (ItemExport key : keys) {
        Pair<String, ExportValueType> pair = key.formatForExport(record.getValues(), context);
        String tag = getTag(key);
        if (tag.length() == 0)
          continue;
        String value = pair == null ? "" : Util.NN(pair.getFirst());
        if (isCDATA(value)) {
          out.println("      <" + tag + "><![CDATA[");
          if (value.contains("]]>")) {
            value = value.replaceAll("\\]\\]>", "]]>]]&gt;<![CDATA[");
          }
          out.println(value);
          out.println("      ]]></" + tag + ">");
        } else {
          out.println("      <" + tag + ">" + escape(value) + "</" + tag + ">");
        }
      }
      out.println("    </issue>");
      count++;
      myProgress.setProgress((float) step * count);
    }
    out.println("  </issues>");
  }

  private boolean isCDATA(String value) {
    if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)
      return true;
    if (value.length() < 60)
      return false;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '<' || c == '>' || c == '&' || c == '\'' || c == '\"')
        return true;
    }
    return false;
  }

  private String getTag(ItemExport key) {
    String tag = myTagCache.get(key);
    if (tag == null) {
      tag = key.getDisplayName();
      if (tag == null || tag.length() == 0)
        tag = key.getId();
      if (tag == null) {
        tag = "";
      } else {
        tag = Util.lower(tag);
        tag = tag.replaceAll("[^a-z0-9\\s]", "");
        String[] words = tag.split("\\s+");
        if (words.length > 1) {
          StringBuffer tagb = new StringBuffer(tag.length());
          tagb.append(words[0]);
          for (int i = 1; i < words.length; i++) {
            String word = words[i];
            if (word.length() == 0)
              continue;
            tagb.append(Character.toUpperCase(word.charAt(0)));
            tagb.append(word.substring(1));
          }
          tag = tagb.toString();
        }
      }
      myTagCache.put(key, tag);
    }
    return tag;
  }

  private void writeExportInfo(PrintWriter out, ExportedData data, ExportParameters parameters) {
    out.println("  <exportInfo>");
    String collectionName = data.getCollectionName();
    GenericNode node = data.getNode();
    if (node != null) {
      String connectionName = getConnectionName(node);
      if (connectionName != null) {
        out.println("    <connection>" + connectionName + "</connection>");
      }
    }
    if (collectionName != null) {
      out.println("    <queryName>" + escape(collectionName) + "</queryName>");
    }
    if (node != null) {
      out.println("    <queryPath>");
      writePath(out, node);
      out.println("    </queryPath>");
    }
    String date = getDate(data.getDateCollected(), parameters.getLocale());
    if (date != null) {
      out.println("    <exportDate>" + date + "</exportDate>");
    }
    out.println("    <exporter>" + Setup.getProductName() + "</exporter>");
    out.println("    <exporterVersion>" + myProductInformation.getBuildNumber().toDisplayableString() + "</exporterVersion>");
    out.println("  </exportInfo>");
  }

  private void writePath(PrintWriter out, GenericNode node) {
    GenericNode parent = node.getParent();
    if (parent != null)
      writePath(out, parent);
    String name = node.getName();
    if (name == null)
      name = ".";
    out.println("      <node>" + escape(name) + "</node>");
  }
}
