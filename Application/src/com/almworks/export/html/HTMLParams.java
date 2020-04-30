package com.almworks.export.html;

import com.almworks.export.FileExportParams;
import org.almworks.util.TypedKey;

import java.io.File;

public interface HTMLParams extends FileExportParams {
  TypedKey<File> CSS_FILE = TypedKey.create("CSS_FILE");
  TypedKey<Integer> BUGS_PER_TABLE = TypedKey.create("BUGS_PER_TABLE");
}
