#! /bin/sh

# Installation directory of Apache ANT (the dir which containts "bin", "lib" sub directories)
ANT_HOME=
# Home directory of Java8 JDK (the dir which contains "bin", "lib", "jre" subdirectories)
JDK8_HOME=

"$JDK8_HOME/bin/java" -cp "$ANT_HOME/lib/ant-launcher.jar" org.apache.tools.ant.launch.Launcher -f ./build.xml prepareDistribution -Djdk="$JDK8_HOME" -Dbuild.number=9876