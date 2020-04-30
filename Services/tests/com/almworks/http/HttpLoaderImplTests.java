package com.almworks.http;

import com.almworks.util.tests.BaseTestCase;

public class HttpLoaderImplTests extends BaseTestCase {
  public void testServerOverescapingHandling() {
    checkOverescaping("?id=3982%252C4490%252C4491%252C4496%252C4516%252C4524%252C4542%252C4555%252C4559%252C4563%252C4568%252C4573","?id=3982%2C4490%2C4491%2C4496%2C4516%2C4524%2C4542%2C4555%2C4559%2C4563%2C4568%2C4573");
    checkOverescaping("", null);
    checkOverescaping("ddd", null);
    checkOverescaping("%", null);
    checkOverescaping("%%%%%%", null);
    checkOverescaping("%aa%vv%cc%vv%dd%1jkmjc01", null);
    checkOverescaping("xxxxx%", null);
    checkOverescaping("xxxxx%2", null);
    checkOverescaping("xxxxx%25", null);
    checkOverescaping("xxxxx%252", null);
    checkOverescaping("xxxxx%252C", "xxxxx%2C");
    checkOverescaping("xxxxx%252J", null);
    checkOverescaping("xxxxx%FFhakhbajkhba%25FDdcdcd", "xxxxx%FFhakhbajkhba%FDdcdcd");
  }

  private void checkOverescaping(String original, String expected) {
    String result = HttpLoaderImpl.fixServerOverEscapingQuery(original);
    assertEquals(expected, result);
  }
}
