package com.almworks.export.pdf.itext;

import com.almworks.api.application.ModelKey;
import com.almworks.api.application.util.ExportContext;
import com.almworks.api.application.viewer.Comment;
import com.almworks.api.engine.Connection;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.threads.Computable;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.almworks.util.Collections15;

import java.util.*;

public class Comments extends PrintKeyElement<Document> {
  private final ModelKey<? extends Collection<? extends Comment>> myModelKey;
  private List<CommentCopy> myData;

  protected Comments(ModelKey<? extends Collection<? extends Comment>> collectionModelKey) {
    myModelKey = collectionModelKey;
  }

  public void setContext(Connection connection, PropertyMap propertyMap, ExportContext context) {
    myData = CommentCopy.copyCollection(myModelKey.getValue(propertyMap), context);
  }

  public void appendPrintElement(Document container, ReportMetrics metrics, PdfWriter writer) throws DocumentException {
    for (CommentCopy comment : myData) {
      if (comment.getText().equals("")) continue;
      final Paragraph paragraph = new Paragraph("Comments", metrics.keyReportFont());
      paragraph.setSpacingBefore(metrics.SPACING);
      paragraph.setSpacingAfter(metrics.SPACING / 2);
      container.add(paragraph);
      break;
    }

    int count = 1;

    PdfPTable commentsTable = new PdfPTable(1);

    commentsTable.setSplitRows(true);
    commentsTable.setSplitLate(false);
    commentsTable.setSpacingAfter(metrics.SPACING);

    commentsTable.setWidthPercentage(100);

    for (CommentCopy commentCopy : myData) {
      if (commentCopy.getText().equals("") ) continue;
      PdfPTable comment = new PdfPTable(2);
      comment.setSplitRows(true);
      comment.setSplitLate(false);

      final String who = String.format("#%d %s", count++, commentCopy.getWhoText());
      PdfPCell c = new PdfPCell(new Paragraph(who, metrics.whenWhoFont()));

      c.setBorder(0);
      comment.addCell(c);

      c = new PdfPCell(new Paragraph(commentCopy.getWhenText(),
        metrics.whenWhoFont()));
      c.setBorder(0);
      c.setHorizontalAlignment(Element.ALIGN_RIGHT);

      comment.addCell(c);
      final Paragraph paragraph = new Paragraph(metrics.commentDescriptionFont().getSize() * 1.2f, commentCopy.getText(),
        metrics.commentDescriptionFont());
      c = new PdfPCell(paragraph);


      c.setBorder(0);
      c.setColspan(2);

      comment.addCell(c);
      PdfPCell p = new PdfPCell(comment);

      p.setPaddingTop(metrics.BEETWEEN_COMMENT_SPACING);

      p.setBorder(0);
      p.setBorderWidth(0);


      commentsTable.addCell(p);
    }
    container.add(commentsTable);
  }

  private static class CommentCopy {
    private final String myText;
    private final String myWhoText;
    private final String myWhenText;

    private CommentCopy(String text, String whoText, String whenText) {
      myText = text;
      myWhoText = whoText;
      myWhenText = whenText;
    }

    public static List<CommentCopy> copyCollection(final Collection<? extends Comment> collection, final ExportContext context) {
      if (collection == null || collection.isEmpty()) return Collections.emptyList();
      return ThreadGate.AWT_IMMEDIATE.compute(new Computable<List<CommentCopy>>() {
        @Override
        public List<CommentCopy> compute() {
          ArrayList<CommentCopy> list = Collections15.arrayList();
          for (Comment comment : collection) list.add(copy(comment, context));
          return list;
        }
      });
    }

    private static CommentCopy copy(Comment comment, ExportContext context) {
      final Date date = comment.getWhen();
      String whenText = date != null ? context.formatDate(date) : comment.getWhenText();
      return new CommentCopy(comment.getText(), comment.getWhoText(), whenText);
    }

    public String getText() {
      return myText;
    }

    public String getWhoText() {
      return myWhoText;
    }

    public String getWhenText() {
      return myWhenText;
    }
  }
}
