package com.almworks.explorer.qbuilder.filter;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.qb.AttributeConstraintDescriptor;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPNotNull;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

/**
 * @author dyoma
 */
public class DateConstraintDescriptor extends AttributeConstraintDescriptor<Date> {
  private static final int SAFETY_INTERVAL = 5000;
  private final boolean myAcceptNullIfLaterThanConstraint;
  public static final TypedKey<Boolean> ACCEPT_NULL_IF_LATER_THAN_CONSTRAINT = TypedKey.create("acceptNullIfConstraintIsOnlyLaterThanConstraint");

  public DateConstraintDescriptor(String displayName, String id, DBAttribute<Date> attribute, boolean acceptNullIfLaterThanConstraint) {
    super(DateAttribute.DATE_TIME, displayName, id, attribute, Modifiable.NEVER);
    myAcceptNullIfLaterThanConstraint = acceptNullIfLaterThanConstraint;
  }

  @Override
  @Nullable
  public BoolExpr<DP> createFilter(PropertyMap data, ItemHypercube hypercube) {
    data.put(ACCEPT_NULL_IF_LATER_THAN_CONSTRAINT, myAcceptNullIfLaterThanConstraint);
    BoolExpr<DP> r = super.createFilter(data, hypercube);
    if (r != null && myAcceptNullIfLaterThanConstraint) {
      boolean present = true;
      long now = System.currentTimeMillis();
      DateUnit.DateValue before = data.get(DateAttribute.BEFORE);
      if (before != null && before.getUptoDateValue().getTime() <= now + SAFETY_INTERVAL) {
        present = false;
      }
      if (present) {
        DateUnit.DateValue after = data.get(DateAttribute.AFTER);
        if (after != null && after.getUptoDateValue().getTime() > now + SAFETY_INTERVAL) {
          present = false;
        }
      }
      if (present) {
        DBAttribute<Date> attribute = getAttribute();
        BoolExpr<DP> notSet = DPNotNull.create(attribute).negate();
        r = r.or(BoolExpr.and(notSet, ItemDownloadStage.IS_NEW));
      }
    }
    return r;
  }
}
