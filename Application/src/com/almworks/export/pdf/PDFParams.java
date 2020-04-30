package com.almworks.export.pdf;

import com.almworks.export.FileExportParams;
import org.almworks.util.TypedKey;

/**
 * @author Alex
 */
public interface PDFParams extends FileExportParams {
  TypedKey<Boolean> COMMENTS = TypedKey.create("COMMENTS");
  TypedKey<Boolean> ATTACH_GRAPH = TypedKey.create("ATTACH_GRAPH");
  TypedKey<Boolean> ATTACH_TEXT = TypedKey.create("ATTACH_TEXT");
  TypedKey<Boolean> ON_NEW_PAGE = TypedKey.create("ON_NEW_PAGE");
  TypedKey<Boolean> COMPACT_TABLE = TypedKey.create("COMPACT_TABLE");
}
