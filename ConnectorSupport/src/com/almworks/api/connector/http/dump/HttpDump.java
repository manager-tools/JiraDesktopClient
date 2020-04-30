package com.almworks.api.connector.http.dump;

import com.almworks.api.connector.http.DumpUtils;
import com.almworks.api.connector.http.HttpDumper;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;

class HttpDump {
  private final FileOutputStream myStream;

  private HttpDump(FileOutputStream stream) {
    myStream = stream;
  }

  @SuppressWarnings("FinalizeDoesntCallSuperFinalize")
  @Override
  protected void finalize() throws Throwable {
    myStream.close();
  }

  @Nullable("When dump is not required or dump failed")
  public static HttpDump dumpSuccess(HttpDumper.DumpSpec spec, FileRequestDumper dumper) {
    HttpDumper.DumpLevel level = spec.getLevel();
    if (level == HttpDumper.DumpLevel.NONE || level == HttpDumper.DumpLevel.ERRORS) return null;
    Pair<FileOutputStream, PrintStream> pair = doDump(spec, dumper);
    if (pair == null) return null;
    PrintStream printStream = pair.getSecond();
    DumpUtils.startReply(printStream, dumper.getMessage(), true, null);
    printStream.flush(); // And forget. Don't close since underlaying stream may be used later
    return new HttpDump(pair.getFirst());
  }

  public static void dumpAndClose(HttpDumper.DumpSpec spec, FileRequestDumper dumper, @Nullable Throwable e) {
    Pair<FileOutputStream, PrintStream> pair = doDump(spec, dumper);
    if (pair == null) return;
    PrintStream out = pair.getSecond();
    DumpUtils.startReply(out, dumper.getMessage(), false, e);
    out.println("-- There was no response --");
    IOUtils.closeStreamIgnoreExceptions(out);
    IOUtils.closeStreamIgnoreExceptions(pair.getFirst());
  }

  @Nullable
  private static Pair<FileOutputStream, PrintStream> doDump(HttpDumper.DumpSpec spec, FileRequestDumper dumper) {
    File dumpFile = null;
    FileOutputStream stream = null;
    PrintStream out = null;
    boolean success = false;
    try {
      String requestUrl = dumper.getRequestUrl();
      dumpFile = DumpUtils.getDumpFile(spec.getDir(), dumper.getScript(), DumpUtils.getDumpURI(requestUrl));
      assert dumpFile != null;
      stream = new FileOutputStream(dumpFile);
      out = new PrintStream(new BufferedOutputStream(stream));
      DumpUtils.writeRequestData(out, requestUrl, dumper.getRequestCookies(), dumper.getCookiesAfter(), dumper.getRequestHeaders(), dumper.getPostParameters(),
        dumper.getRawRequest(), dumper.getPolizei());
      DumpUtils.writeHttpReport(out, dumper.getHttpReport());
//      writeReplyData(out, myResponse, myApplicationMessage, success, myException);
      success = true;
      return Pair.create(stream, out);
    } catch (DumpUtils.DumpFileException e) {
      LogHelper.debug("cannot create dump file: " + e.getMessage());
    } catch (IOException e) {
      LogHelper.debug("failed to write dump to " + dumpFile, e);
    } finally {
      if (!success) {
        LogHelper.debug("failure to write dump, cause is unknown");
        IOUtils.closeStreamIgnoreExceptions(out);
        IOUtils.closeStreamIgnoreExceptions(stream);
      }
    }
    return null;
  }

  public void closeStream() {
    IOUtils.closeStreamIgnoreExceptions(myStream);
  }

  public void byteRead(int readByte) {
    try {
      myStream.write(readByte);
    } catch (IOException e) {
      LogHelper.warning("Error writing http dump", e);
    }
  }

  public void arrayRead(byte[] array, int off, int length) {
    try {
      myStream.write(array, off, length);
    } catch (IOException e) {
      LogHelper.warning("Error writing http dump", e, length);
    }
  }
}
