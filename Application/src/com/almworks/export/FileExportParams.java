package com.almworks.export;

import org.almworks.util.TypedKey;

import java.io.File;

public interface FileExportParams {
  TypedKey<File> TARGET_FILE = TypedKey.create("TARGET_FILE");
}
