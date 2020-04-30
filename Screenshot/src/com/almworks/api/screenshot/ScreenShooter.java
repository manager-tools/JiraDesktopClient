package com.almworks.api.screenshot;

import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.Role;

import java.awt.*;

public interface ScreenShooter {
  Role<ScreenShooter> ROLE = Role.role(ScreenShooter.class);

  void shoot(Component contextComponent, Configuration conf, Procedure<Screenshot> result);

  void edit(Image image, Configuration conf, Procedure<Screenshot> result);
}
