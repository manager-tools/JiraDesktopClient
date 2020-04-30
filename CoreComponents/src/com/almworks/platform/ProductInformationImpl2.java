package com.almworks.platform;

import com.almworks.api.gui.BuildNumber;
import com.almworks.api.install.Setup;
import com.almworks.api.platform.ProductInformation;
import com.almworks.util.Terms;
import com.almworks.util.i18n.Local;
import com.almworks.util.i18n.LocalTextProvider;
import org.almworks.util.Failure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author : Dyoma
 */
class ProductInformationImpl2 implements ProductInformation {
  private static final String BUILD_DATE_FORMAT = "yyyy/MM/dd HH:mm z";

  private final ProductInfoProperties myProductInfoProperties;

  public ProductInformationImpl2() {
    myProductInfoProperties = new ProductInfoProperties();
    Local.getBook().installProvider(new LocalTextProvider() {
      @NotNull
      public Weight getWeight() {
        return Weight.SYSTEM;
      }

      @Nullable
      public String getText(@NotNull String key, @NotNull Locale locale) {
        if (Terms.key_Deskzilla.equals(key))
          return Setup.getProductName();
        return null;
      }
    });
  }


  public String getName() {
    return myProductInfoProperties.getProductName();
  }

  public String getVersion() {
    return myProductInfoProperties.getVersion();
  }

  public String getVersionType() {
    return myProductInfoProperties.getVersionType();
  }

  @NotNull
  public BuildNumber getBuildNumber() {
    return BuildNumber.create(myProductInfoProperties.getBuildNumber());
  }

  public Date getBuildDate() {
    String date = myProductInfoProperties.getBuildDate();
    Date buildDate;
    if (date != null) {
      try {
        buildDate = new SimpleDateFormat(BUILD_DATE_FORMAT).parse(date);
      } catch (ParseException e) {
        throw new Failure(e);
      }
    } else
      buildDate = new Date();
    return buildDate;
  }

  public boolean isProductionVersion() {
    String type = getVersionType();
    return type != null && !EAP.equalsIgnoreCase(type) && !DEBUG.equalsIgnoreCase(type);
  }
}