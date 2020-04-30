package com.almworks.http;

import com.almworks.util.tests.BaseTestCase;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class JdkHttpsNtlmProxyHackTests extends BaseTestCase {
  public void testPatching() {
    JdkHttpsNtlmProxyHack.DelegateHttpsURLConnection.PatcherPrintStream ps =
      new JdkHttpsNtlmProxyHack.DelegateHttpsURLConnection.PatcherPrintStream(
        new PrintStream(new ByteArrayOutputStream()));

    assertEquals("", ps.preprocess(""));
    assertEquals("dlkdsaljsa vk vkfnv kfsajvn fakjvnsa v\r\nksndkcsanc dsa\r\n", ps.preprocess(
      "dlkdsaljsa vk vkfnv kfsajvn fakjvnsa v\r\nksndkcsanc dsa\r\n"));

    assertEquals("CONNECT whatever\r\nProxy-Connection: keep-alive\r\n\r\n", ps.preprocess("CONNECT whatever\r\n\r\n"));
    assertEquals("CONNECT whatever\r\nSomething\r\nProxy-Connection: keep-alive\r\n\r\n", ps.preprocess("CONNECT whatever\r\nSomething\r\n\r\n"));
    assertEquals("CONNECT whatever\r\nProxy-Connection: keep-alive\r\nSomething\r\n\r\n", ps.preprocess("CONNECT whatever\r\nProxy-Connection: die\r\nSomething\r\n\r\n"));

    // state
    assertEquals("CONNECT ", ps.preprocess("CONNECT "));
    assertEquals("CONNECT ", ps.preprocess("CONNECT ")); // garbage
    assertEquals("\r\n", ps.preprocess("\r\n"));
    assertEquals("Proxy-Connection: keep-alive\r\n\r\n   dddd", ps.preprocess("\r\n   dddd")); // garbage

    // state
    assertEquals("CONNECT server.edu HTTP/1.0\r\n", ps.preprocess("CONNECT server.edu HTTP/1.0\r\n"));
    assertEquals("Host: some.host\r\n", ps.preprocess("Host: some.host\r\n"));
    assertEquals("Proxy-Connection: keep-alive\r\n\r\n", ps.preprocess("\r\n"));

    // state
    assertEquals("CONNECT server.edu HTTP/1.0\r\n", ps.preprocess("CONNECT server.edu HTTP/1.0\r\n"));
    assertEquals("Host: some.host\r\n", ps.preprocess("Host: some.host\r\n"));
    assertEquals("Proxy-Connection: keep-alive\r\n", ps.preprocess("Proxy-Connection: keep-alive\r\n"));
    assertEquals("Host: some.host\r\n", ps.preprocess("Host: some.host\r\n"));
    assertEquals("\r\n", ps.preprocess("\r\n"));

    // state
    assertEquals("CONNECT server.edu HTTP/1.0\r\n", ps.preprocess("CONNECT server.edu HTTP/1.0\r\n"));
    assertEquals("Host: some.host\r\n", ps.preprocess("Host: some.host\r\n"));
    assertEquals("Proxy-Connection: keep-alive\r\n", ps.preprocess("Proxy-Connection: keep-deaddeaddead\r\n"));
    assertEquals("Host: some.host\r\n", ps.preprocess("Host: some.host\r\n"));
    assertEquals("\r\n", ps.preprocess("\r\n"));
  }
}
