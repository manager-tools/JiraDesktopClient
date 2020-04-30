package com.almworks.export.pdf.itext;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.ItemExport;
import com.almworks.api.application.viewer.Comment;
import com.almworks.engine.gui.attachments.Attachment;
import com.itextpdf.text.Document;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.List;

/**
 * @author Alex
 */

public class PrintBuilder {
  private List<PrintElement<Document>> myPrintElements = Collections15.arrayList();
  private boolean myFromBlank;
  private IssueHeader myHeader;
  private AttributeTable myTableElements = new AttributeTable();
  private boolean myTableCompact;

  public void setAttaches(ModelKey<? extends Collection<? extends Attachment>> attachKey, boolean graphics, boolean text) {
    myPrintElements.add(new Attachments(attachKey, text, graphics));
  }

  public void addAttribute(ItemExport att) {
    myTableElements.addAttribute(att);
  }

  public void addComments(ModelKey<? extends Collection<? extends Comment>> commentsKey) {
    myPrintElements.add(new Comments(commentsKey));
  }

  public void addTextSection(String title, ItemExport key) {
    final LargeTextElement o = new LargeTextElement(key);

    myPrintElements.add(o);
  }

  public List<PrintElement<Document>> createList() {
    List<PrintElement<Document>> resultList = Collections15.arrayList();

    if (myHeader != null) {
      resultList.add(myHeader);
    }

    resultList.add(myTableElements);
    resultList.addAll(myPrintElements);


    resultList.add(new PageBreak(myFromBlank));

    
    return resultList;
  }

  public void setNewFromBlank(boolean fromBlank) {
    myFromBlank = fromBlank;
  }

  public void setHeader(ItemExport key, ItemExport ... others ) {
    myHeader = new IssueHeader(key, others);
  }

  public void addLargeField(ItemExport modelKey) {
    myPrintElements.add(new LargeTextElement(modelKey));
  }

  public void setTableCompact(boolean aBoolean) {
    myTableElements.setColCount(aBoolean ? 2 : 1);
  }

  /*public void setHasTableOfContents(boolean aBoolean) {
    myHeader.setHasAnchor(aBoolean);
  } */
}
