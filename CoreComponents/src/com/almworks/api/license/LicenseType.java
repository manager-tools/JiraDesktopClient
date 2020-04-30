package com.almworks.api.license;

import com.almworks.util.AppBook;
import com.almworks.util.Enumerable;
import com.almworks.util.i18n.LText;

public class LicenseType extends Enumerable<LicenseType> {
  /**
   * This flag is set to make JIRA Client Lite work with alternative licensing scheme:
   * - no connection limits
   * - no project limits
   * - only up to 10 users in the local db per connection
   */
  @Deprecated
  public static final boolean JIRACLIENT_STARTER = true;

  private static final String PREFIX = "Application.License.";
  private static final LText FULL_TEXT = AppBook.text(PREFIX + "FULL", "Single-user license");
  private static final LText EVAL_TEXT = AppBook.text(PREFIX + "EVAL", "Evaluation license");
  private static final LText EAP_TEXT = AppBook.text(PREFIX + "EAP", "EAP license");
  private static final LText OS_TEXT = AppBook.text(PREFIX + "OS", "License for open-source projects");
  private static final LText INVALID_TEXT = AppBook.text(PREFIX + "INVALID", "License is INVALID");
  private static final LText FLOATING_TEXT = AppBook.text(PREFIX + "FLOATING", "Team license");
  private static final LText PERSONAL_TEXT = AppBook.text(PREFIX + "PERSONAL", "Personal license");
  private static final LText ACADEMIC_TEXT = AppBook.text(PREFIX + "ACADEMIC", "Academic license");
  private static final LText SITE_TEXT = AppBook.text(PREFIX + "SITE", "Site license");
  private static final LText LITE_TEXT = AppBook.text(PREFIX + "LITE", "Lite license");
  private static final LText READER_TEXT = AppBook.text(PREFIX + "READER", "Reader license");
  private static final LText AMKT_TEXT = AppBook.text(PREFIX + "AMKT", "Atlassian Marketplace license should be installed onto your Jira server");
  private static final LText NO_LICENSE_TEXT = AppBook.text(PREFIX + "NOLICENSE", "No license");

  // commercial user-based
  // single-user license keeps "FULL" id for backward compatibility
  public static final LicenseType SINGLE_USER = new LicenseType("FULL", FULL_TEXT);
  public static final LicenseType PERSONAL = new LicenseType("PERSONAL", PERSONAL_TEXT);

  // commercial floating-based
  public static final LicenseType FLOATING = new LicenseType("FLOATING", FLOATING_TEXT);
  public static final LicenseType ACADEMIC = new LicenseType("ACADEMIC", ACADEMIC_TEXT);
  public static final LicenseType SITE = new LicenseType("SITE", SITE_TEXT);

  // License stub for AMKT distribution
  /**
   * @deprecated No license is required to connect to servers with JCPL installed
   */
  @Deprecated
  public static final LicenseType AMKT = new LicenseType("AMKT", AMKT_TEXT);

  // free
  /**
   * @deprecated no license is required to connection to sponsored servers
   */
  @Deprecated
  public static final LicenseType OPEN_SOURCE = new LicenseType("OS", OS_TEXT);
  /**
   * @deprecated this license type is deprecated. To connection to JIRA Starter servers new license type is required
   */
  @Deprecated
  public static final LicenseType LITE = new LicenseType("LITE", LITE_TEXT);
  /**
   * @deprecated this license type is not supported now (even it was ever supported before)
   */
  @Deprecated
  public static final LicenseType READER = new LicenseType("READER", READER_TEXT);

  // eval/eap
  /**
   * @deprecated EAP licenses not supported (or needs other implementation
   */
  @Deprecated
  public static final LicenseType EAP = new LicenseType("EAP", EAP_TEXT);
  /**
   * @deprecated user has to obtain evaluation license key
   */
  @Deprecated
  public static final LicenseType KEYLESS_EVALUATION = new LicenseType("KLEVAL", EVAL_TEXT);
  public static final LicenseType KEYED_EVALUATION = new LicenseType("EVAL", EVAL_TEXT);

  // other
  public static final LicenseType INVALID = new LicenseType("INVALID", INVALID_TEXT);
  public static final LicenseType NO_LICENSE = new LicenseType("NO LICENSE", NO_LICENSE_TEXT);

  private final LText myDisplayName;

  private LicenseType(String name, LText displayName) {
    super(name);
    myDisplayName = displayName;
  }

  public LText getDisplayName() {
    return myDisplayName;
  }

  public static LicenseType forName(String name) {
    LicenseType type = forName(LicenseType.class, name);
    return type == null ? INVALID : type;
  }

  public boolean requiresExpirationDate() {
    return this == KEYLESS_EVALUATION || this == KEYED_EVALUATION || this == EAP;
  }

  public boolean isEvaluation() {
    return this == KEYED_EVALUATION || this == KEYLESS_EVALUATION;
  }

  public boolean isLeaseCounted() {
    return this == FLOATING || this == ACADEMIC;
  }

  public boolean hasLicensedToInfo() {
    return this == SINGLE_USER || this == PERSONAL || this == FLOATING || this == ACADEMIC || this == SITE;
  }

  public boolean isValid() {
    return this != NO_LICENSE && this != INVALID;
  }
}
