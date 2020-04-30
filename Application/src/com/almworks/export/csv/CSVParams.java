package com.almworks.export.csv;

import com.almworks.export.FileExportParams;
import org.almworks.util.TypedKey;

import java.nio.charset.Charset;

public interface CSVParams extends FileExportParams {
  TypedKey<Boolean> OUTPUT_HEADER = TypedKey.create("OUTPUT_HEADER");
  TypedKey<Boolean> QUOTES_ALWAYS = TypedKey.create("QUOTES_ALWAYS");
  TypedKey<Character> DELIMITER_CHAR = TypedKey.create("DELIMITER_CHAR");
  TypedKey<Boolean> PROTECT_FORMULA = TypedKey.create("PROTECT_FORMULA");
  TypedKey<Charset> CHARSET = TypedKey.create("CHARSET");
}
