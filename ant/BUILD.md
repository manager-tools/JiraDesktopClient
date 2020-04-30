## Build
    
### Build parameters
The [build.xml](build.xml) Ant script takes the following parameters:

 * **jdk** - path to Oracle JDK 8. The script uses this JDK to compile sources and run tests
 
 * **build.number** - build number. A built application shows build number on its About screen.
    Build script creates ZIP file with build number.
    If the build number is not provided, 0 (zero) build number is used by default.

A sample shell script to run the build:

```

#! /bin/sh

# Installation directory of Apache ANT (the dir which containts "bin", "lib" sub directories)
ANT_HOME=
# Home directory of Java8 JDK (the dir which contains "bin", "lib", "jre" subdirectories)
JDK8_HOME=

"$JDK8_HOME/bin/java" -cp "$ANT_HOME/lib/ant-launcher.jar" org.apache.tools.ant.launch.Launcher -f ./build.xml prepareDistribution -Djdk="$JDK8_HOME" -Dbuild.number=9876
```

Run the script from the [ant](.) directory          
    
### Files
 * [build.xml](build.xml) main build script
 * [build.sh](build.sh) sample shell script which launches a build process and provides it all parameters
 * [genHeader.xml](genHeader.xml), [properties.xml](properties.xml), [runGenerated.xml](runGenerated.xml) supplementary build files
 * [meta.xml](meta.xml) describes source modules, external libraries, source dependencies and distribution layout
 * [transform.xsl](transform.xsl) used to transform the [meta.xml](meta.xml) file to Ant build script
 * [generated.xml](generated.xml) temporary build script produced by [transform.xsl](transform.xsl) applied to [meta.xml](meta.xml)
 * [lib](lib) directory contains third-party Java libraries required by the build
    * [javac2.jar](lib/javac2.jar), [bcel.jar](lib/bcel.jar), [asm-all.jar](lib/asm-all.jar) are required to define the _javac2_ task
    * [saxon9he.jar](lib/saxon9he.jar) is required for XSL transformation of the [meta.xml](meta.xml) file

## meta.xml file format
The [meta.xml](meta.xml) file describes:

 * Modules - Java Sources

 * Libraries - external JAR files
 
 * Product Description - JARs the product distribution consists of and their locations

### Modules
Java source and resource are organized into modules. Module is a unit of dependency.
The [meta.xml](meta.xml) file include the **module** tag for each module.
The **name** parameter of the tag defines module root directory and module name.

#### Child Tags
 * **depends** describes module dependency on other modules.
  The tag has the only **module** parameter. 
  The value of the parameter is the name of a module this module depends on.
 * **uselib** describes module dependency on external libraries.
  The tag has the single parameter **lib**. 
  The value of the parameter is the name of a library this module depends on.

Note, dependencies are not transitive. 
If a module M1 depends on a module M2 and the M2 module depends on module M3 and library L1,
the module M1 has no automatic dependency neither on module M3 nor on library L1.
If the M1 module requires this dependencies, they must be explicitly described.
   
#### Module Direcories
Each module may have the following directories:

 * **src** - contains production java source files
 * **tests** - contains java sources with tests and supplementary classes.
   * The build compiles these sources, but does not include them in distributable JARs.
   * Test classes must end with "Test" or "Tests" suffix.
 * **rc** - contains production resources. Build copies all these files
 to  the destination JAR as is and preserving packages.
 * **test.rc** - contains test resources. These resources are available
 during execution of tests, but are not included in distributable JARs.
 
All directories above are optional.

### Libraries

A library is one or more external JAR files.
The [meta.xml](meta.xml) file include the **lib** tag for each library.
The **name** parameter of the library defines library's name, it is used to refer to this library.

A **lib** tag has one or more **jar** child tag.
Each **jar** tag has the only parameter **jar** which contains a path to a single JAR file.
The path is relative to the [lib](..\lib) directory.

### Product Description

The **product** tag describe layout of a distribution.

#### JARs Built from Sources

The [meta.xml](meta.xml) file describes distributable JARs with **distjar** tags.
The tag has single **jar** parameter - the name of the JAR file (it does not include path).

##### Child Tags
 * **place** - location of the JAR in the distribution. There must be only one such child.
   The only parameter is **dir** - directory where to place the JAR.
 * **include** - includes a module into the JAR. One child tag for one module to include.
   The only parameter is **module** - the name of a module to include.
 * **manifest** - defines META-INF/MANIFEST.MF file content.
   The **attribute** child tag instructs the build to add one manifest attribute.
   The **name** parameter is the name of the attribute.
   The **value** parameter is the value of the attribute. 

#### Redistributing Libraries

The build copies libraries to the **lib** directory of the distribution.
The build includes only libraries those explicitly mentioned with the **distlib** tag (regardless of declared module dependencies).
Each **distlib** tag has the only parameter **lib** which refers a library by its name.
The build copies all library JAR files.             