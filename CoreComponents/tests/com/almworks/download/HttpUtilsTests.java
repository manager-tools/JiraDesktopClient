package com.almworks.download;

import com.almworks.api.http.HttpUtils;
import com.almworks.util.tests.BaseTestCase;
import org.apache.commons.httpclient.Header;

public class HttpUtilsTests extends BaseTestCase {
  public void testGetAuxiliaryHeaderInfo() {
    assertEquals(null, HttpUtils.getHeaderAuxiliaryInfo((String) null, null));
    assertEquals(null, HttpUtils.getHeaderAuxiliaryInfo((Header) null, null));
    assertEquals(null, HttpUtils.getHeaderAuxiliaryInfo("", null));
    assertEquals(null, HttpUtils.getHeaderAuxiliaryInfo("", ""));
    assertEquals(null, HttpUtils.getHeaderAuxiliaryInfo("", "whatever"));
    assertEquals(null, HttpUtils.getHeaderAuxiliaryInfo("whatever", ""));

    assertEquals("x.xml", HttpUtils.getHeaderAuxiliaryInfo("application/xml; name=x.xml", "name"));
    assertEquals("x.xml", HttpUtils.getHeaderAuxiliaryInfo("application/xml; name=\"x.xml\"", "name"));
    assertEquals("x.xml", HttpUtils.getHeaderAuxiliaryInfo("application/xml; name='x.xml'", "name"));
    assertEquals("x.xml", HttpUtils.getHeaderAuxiliaryInfo("application/xml; name= 'x.xml' ", "name"));
    assertEquals("x.xml", HttpUtils.getHeaderAuxiliaryInfo("application/xml  ;   name=\t   'x.xml' ", "name"));
    assertEquals("x.xml", HttpUtils.getHeaderAuxiliaryInfo("application/xml  ;   name=\t   'x.xml' ;name=xxx; a=b", "name"));
    assertEquals("x.xml", HttpUtils.getHeaderAuxiliaryInfo("application/xml; encoding=koi8-r; name=x.xml", "name"));
  }
}
