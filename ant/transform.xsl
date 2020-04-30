<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>
  <xsl:template match="/project">
    <project name="Generated build" default="ALL.compile">
    <import file="genHeader.xml"/>

      <!-- ============================ GLOBAL ==================================== -->

      <target name="ALL.compile">
        <xsl:attribute name="depends">
          <xsl:for-each select="module">
            <xsl:value-of select="@name"/>
            <xsl:text>.compile</xsl:text>
            <xsl:if test="position()!=last()">
              <xsl:text>,</xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:attribute>
      </target>

      <target name="ALL.compileTests" unless="without.tests">
        <xsl:attribute name="depends">
          <xsl:for-each select="module">
            <xsl:value-of select="@name"/>
            <xsl:text>.compileTests</xsl:text>
            <xsl:if test="position()!=last()">
              <xsl:text>,</xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:attribute>
      </target>

      <target name="ALL.test" unless="without.tests">
        <xsl:attribute name="depends">
          <xsl:for-each select="module">
            <xsl:value-of select="@name"/>
            <xsl:text>.test</xsl:text>
            <xsl:if test="position()!=last()">
              <xsl:text>,</xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:attribute>
      </target>

      <xsl:for-each select="product[@id]">
        <macrodef name="buildDist.{@id}">
          <attribute name="source"/>
          <attribute name="target"/>
          <sequential>
            <echo message="Building distribution from: @{{source}} to: @{{target}}."/>
            <delete dir="@{{target}}/${{product.name}}"/>
            <mkdir dir="@{{target}}"/>
            <mkdir dir="@{{target}}/${{product.name}}"/>
            <mkdir dir="@{{target}}/${{product.name}}/lib"/>
            <mkdir dir="@{{target}}/${{product.name}}/components"/>

            <copy todir="@{{target}}/${{product.name}}/lib" failonerror="true" overwrite="true" preservelastmodified="true" flatten="true">
              <fileset dir="${{dir.lib}}">
                <xsl:for-each select="distlib[@lib]">
                  <xsl:variable name="libname" select="@lib"/>
                  <xsl:for-each select="/project/lib[@name=$libname]">
                    <xsl:for-each select="jar">
                      <include name="{@jar}"/>
                    </xsl:for-each>
                    <xsl:for-each select="native">
                      <include name="{@file}"/>
                    </xsl:for-each>
                  </xsl:for-each>
                </xsl:for-each>
              </fileset>
            </copy>

            <xsl:for-each select="distjar">
              <jar destfile="@{{target}}/${{product.name}}/{place/@dir}{@jar}" update="true" duplicate="preserve">
                <xsl:for-each select="include[@module]">
                  <fileset dir="@{{source}}/{@module}">
                    <xsl:for-each select="only[@files]">
                      <include name="{@files}"/>
                    </xsl:for-each>
                  </fileset>
                </xsl:for-each>
                <xsl:for-each select="manifest">
                  <xsl:copy>
                    <xsl:copy-of select="attribute"/>
                    <attribute name="Implementation-Version" value="${{product.buildno}}"/>
                  </xsl:copy>
                </xsl:for-each>
              </jar>
            </xsl:for-each>

            <xsl:for-each select="sourcejar">
              <jar destfile="@{{target}}/${{product.name}}/{place/@dir}{@jar}" update="true" duplicate="preserve">
                <xsl:for-each select="include[@module]">
                  <fileset dir="{@module}/${{subdir.java.production}}">
                    <xsl:for-each select="include[@name]">
                      <include name="{@name}"/>
                    </xsl:for-each>
                  </fileset>
                </xsl:for-each>
                <xsl:for-each select="manifest">
                  <xsl:copy>
                    <xsl:copy-of select="attribute"/>
                    <attribute name="Implementation-Version" value="${{product.buildno}}"/>
                  </xsl:copy>
                </xsl:for-each>
              </jar>
            </xsl:for-each>

            <copy todir="@{{target}}/${{product.name}}" failonerror="false" overwrite="true" preservelastmodified="true">
              <fileset dir="${{dir.distimage}}" excludes="**/.svn/**/*.*"/>
            </copy>

            <copy todir="@{{target}}/${{product.name}}" failonerror="false" overwrite="true" preservelastmodified="true">
              <fileset dir="${{dir.distimage}}.{@id}" excludes="**/.svn/**/*.*" />
            </copy>

            <echo file="@{{target}}/${{product.name}}/${{product.name}}.${{product.version}}"
                  message="build ${{product.buildno}}"/>
          </sequential>
        </macrodef>
      </xsl:for-each>

      <xsl:for-each select="tool[@id]">
        <xsl:variable name="source" select="'${dir.class.prodcode}'"/>
        <xsl:variable name="target" select="'${dir.dist}'"/>
        <target name="buildDist.{@id}.standalone" if="tool.standalone" unless="tool.dev">
          <delete dir="{$target}/${{product.name}}"/>
          <mkdir dir="{$target}/${{product.name}}"/>
          <copy todir="{$target}/${{product.name}}/lib" failonerror="true" overwrite="true" preservelastmodified="true" flatten="true">
            <fileset dir="${{dir.lib}}">
              <xsl:for-each select="distjar/include">
                <xsl:variable name="moduleName" select="@module"/>
                <xsl:for-each select="/project/module[@name=$moduleName]/uselib">
                  <xsl:variable name="libname" select="@lib"/>
                  <xsl:for-each select="/project/lib[@name=$libname]/jar">
                    <include name="{@jar}"/>
                  </xsl:for-each>
                  <xsl:for-each select="/project/lib[@name=$libname]/native">
                    <include name="{@file}"/>
                  </xsl:for-each>
                </xsl:for-each>
              </xsl:for-each>
            </fileset>
          </copy>
          <xsl:for-each select="distjar">
            <xsl:variable name="includedModules" select="include"/>
            <jar destfile="{$target}/${{product.name}}/{@jar}" update="true" duplicate="preserve">
              <xsl:for-each select="include[@module]">
                <fileset dir="{$source}/{@module}">
                  <xsl:for-each select="only[@files]">
                    <include name="{@files}"/>
                  </xsl:for-each>
                </fileset>
              </xsl:for-each>
              <xsl:for-each select="manifest">
                <xsl:copy>
                  <xsl:copy-of select="attribute"/>
                  <attribute name="Implementation-Version" value="${{product.buildno}}"/>
                  <attribute name="Class-Path">
                    <xsl:attribute name="value">
                      <xsl:text>lib/</xsl:text>
                      <xsl:for-each select="$includedModules">
                        <xsl:variable name="moduleName" select="@module"/>
                        <xsl:for-each select="/project/module[@name=$moduleName]/uselib">
                          <xsl:variable name="libname" select="@lib"/>
                          <xsl:for-each select="/project/lib[@name=$libname]/jar">
                            <xsl:value-of select="concat(' lib/', replace(@jar, '(.*/)*(.+)', '$2'))"/>
                          </xsl:for-each>
                        </xsl:for-each>
                      </xsl:for-each>
                    </xsl:attribute>
                  </attribute>
                </xsl:copy>
              </xsl:for-each>
            </jar>
          </xsl:for-each>
          <copy todir="{$target}/${{product.name}}" failonerror="false" overwrite="true" preservelastmodified="true">
            <fileset dir="${{dir.distimage}}.{@id}" excludes="**/.svn/**/*.*" />
          </copy>
        </target>

        <target name="buildDist.{@id}.bundled" unless="tool.standalone">
          <xsl:for-each select="distjar">
            <xsl:variable name="includedModules" select="include"/>
            <xsl:variable name="bundleDependency" select="bundleDependency"/>
            <jar destfile="{$target}/${{product.name}}/{place[@bundle=true()]/@dir}{@jar}" update="true" duplicate="preserve">
              <xsl:for-each select="include[@bundle=true()]">
                <fileset dir="{$source}/{@module}">
                  <xsl:for-each select="only[@files]">
                    <include name="{@files}"/>
                  </xsl:for-each>
                </fileset>
              </xsl:for-each>
              <xsl:for-each select="manifest">
                <xsl:copy>
                  <xsl:copy-of select="attribute"/>
                  <attribute name="Class-Path">
                    <xsl:attribute name="value">
                      <xsl:text>../../lib</xsl:text>
                      <xsl:for-each select="$includedModules">
                        <xsl:variable name="moduleName" select="@module"/>
                        <xsl:for-each select="/project/module[@name=$moduleName]/uselib">
                          <xsl:variable name="libname" select="@lib"/>
                          <xsl:for-each select="/project/lib[@name=$libname]/jar">
                            <xsl:value-of select="concat(' ../../lib/', replace(@jar, '(.*/)*(.+)', '$2'))"/>
                          </xsl:for-each>
                        </xsl:for-each>
                      </xsl:for-each>
                      <xsl:for-each select="$bundleDependency">
                        <xsl:variable name="depJar" select="@jar"/>
                        <xsl:for-each select="/project/product/distjar[@jar=$depJar]">
                          <xsl:value-of select="concat(' ../../', place/@dir, $depJar)"/>
                        </xsl:for-each>
                      </xsl:for-each>
                      <xsl:for-each select="/project/product">
                        <xsl:variable name="productLauncherJar" select="concat(@id, '.jar')"/>
                        <xsl:value-of select="concat(' ../../', distjar[@jar=$productLauncherJar]/place/@dir, $productLauncherJar)"/>
                      </xsl:for-each>
                    </xsl:attribute>
                  </attribute>
                </xsl:copy>
              </xsl:for-each>
            </jar>
          </xsl:for-each>
        </target>

        <target name="buildDist.{@id}.dev" if="tool.dev">
          <xsl:for-each select="distjar">
            <xsl:variable name="includedModules" select="include"/>
            <xsl:variable name="bundleDependency" select="bundleDependency"/>
            <jar destfile="{$target}/{@jar}" update="true" duplicate="preserve">
              <xsl:for-each select="include[@bundle=true()]">
                <fileset dir="{$source}/{@module}">
                  <xsl:for-each select="only[@files]">
                    <include name="{@files}"/>
                  </xsl:for-each>
                </fileset>
              </xsl:for-each>
            </jar>
          </xsl:for-each>
        </target>
      </xsl:for-each>

      <!-- ============================ LIBRARIES ==================================== -->

      <xsl:for-each select="lib">
        <path id="{@name}">
          <xsl:for-each select="jar">
            <pathelement location="${{dir.lib}}/{@jar}"/>
          </xsl:for-each>
          <xsl:for-each select="extjar">
            <pathelement location="{@jar}"/>
          </xsl:for-each>
        </path>
      </xsl:for-each>

      <!-- ============================ MODULES ==================================== -->

      <xsl:for-each select="module">
        <xsl:variable name="MODULE_NAME" select="@name"/>
        <xsl:comment>
          <xsl:text>============================</xsl:text>
          <xsl:value-of select="$MODULE_NAME"/>
          <xsl:text>===========</xsl:text>
        </xsl:comment>

        <xsl:variable name="MODULE_DIR">
          <xsl:choose>
            <xsl:when test="@external">
              <xsl:text>external/</xsl:text><xsl:value-of select="@external"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$MODULE_NAME"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <path id="classpath.{$MODULE_NAME}">
          <xsl:for-each select="uselib">
            <path refid="{@lib}"/>
          </xsl:for-each>
          <xsl:for-each select="depends">
            <pathelement location="${{dir.class.prodcode}}/{@module}"/>
          </xsl:for-each>
        </path>

        <path id="classpath.tests.{$MODULE_NAME}">
          <path refid="junit"/>
          <path refid="classpath.{$MODULE_NAME}"/>
          <pathelement location="${{dir.class.prodcode}}/{$MODULE_NAME}"/>
          <pathelement location="${{dir.class.testcode}}/{$MODULE_NAME}"/>
          <xsl:for-each select="depends">
            <path refid="classpath.tests.{@module}"/>
          </xsl:for-each>
        </path>

        <xsl:if test="checkout">
          <target name="{$MODULE_NAME}.checkCheckout">
            <available file="{$MODULE_DIR}" property="{$MODULE_NAME}.checkoutNotNeeded"/>
          </target>
          <target name="{$MODULE_NAME}.checkout.head" unless="branch">
            <xsl:choose>
              <xsl:when test="checkout[@type='svn'] and checkout[@command]">
                <exec command="{checkout/@command}" dir="external" failonerror="true" failifexecutionfails="true"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:message terminate="yes">checkout of this type is not supported</xsl:message>
              </xsl:otherwise>
            </xsl:choose>
          </target>
          <target name="{$MODULE_NAME}.checkout.branch" if="branch">
            <xsl:choose>
              <xsl:when test="checkout[@type='svn'] and checkout[@branchCommand]">
                <exec command="{checkout/@branchCommand}" dir="external" failonerror="true" failifexecutionfails="true"/>
              </xsl:when>
              <xsl:when test="checkout[@type='svn'] and checkout[@command]">
                <exec command="{checkout/@command}" dir="external" failonerror="true" failifexecutionfails="true"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:message terminate="yes">checkout of this type is not supported</xsl:message>
              </xsl:otherwise>
            </xsl:choose>
          </target>
          <target name="{$MODULE_NAME}.checkout" depends="{$MODULE_NAME}.checkCheckout"
                  unless="{$MODULE_NAME}.checkoutNotNeeded">
            <antcall target="{$MODULE_NAME}.checkout.head"/>
            <antcall target="{$MODULE_NAME}.checkout.branch"/>
          </target>
        </xsl:if>

        <target name="{$MODULE_NAME}.checkDirs">
          <xsl:if test="checkout">
            <xsl:attribute name="depends">
              <xsl:value-of select="$MODULE_NAME"/>
              <xsl:text>.checkCheckout, </xsl:text>
              <xsl:value-of select="$MODULE_NAME"/>
              <xsl:text>.checkout</xsl:text>
            </xsl:attribute>
          </xsl:if>
          <available file="${{dir.project}}/{$MODULE_DIR}/${{subdir.java.production}}" property="available.src.{$MODULE_NAME}"/>
          <available file="${{dir.project}}/{$MODULE_DIR}/${{subdir.java.tests}}" property="available.tests.{$MODULE_NAME}"/>
          <available file="${{dir.project}}/{$MODULE_DIR}/${{subdir.resources}}"
                     property="available.rc.{$MODULE_NAME}"/>
          <available file="${{dir.project}}/{$MODULE_DIR}/${{subdir.resources}}.${{product.name}}"
                     property="available.rc.product.{$MODULE_NAME}"/>
          <available file="${{dir.project}}/{$MODULE_DIR}/${{subdir.resources.tests}}"
                     property="available.tests.rc.{$MODULE_NAME}"/>
        </target>

        <target name="{$MODULE_NAME}.copyResources" depends="{$MODULE_NAME}.checkDirs" if="available.rc.{$MODULE_NAME}">
          <copy todir="${{dir.class.prodcode}}/{$MODULE_NAME}" failonerror="true" overwrite="true" preservelastmodified="true">
            <fileset dir="${{dir.project}}/{$MODULE_DIR}/${{subdir.resources}}"/>
          </copy>
        </target>

        <target name="{$MODULE_NAME}.overrideResources" depends="{$MODULE_NAME}.checkDirs, {$MODULE_NAME}.copyResources"
                if="available.rc.product.{$MODULE_NAME}">
          <copy todir="${{dir.class.prodcode}}/{$MODULE_NAME}" failonerror="true" overwrite="true" preservelastmodified="true">
            <fileset dir="${{dir.project}}/{$MODULE_DIR}/${{subdir.resources}}.${{product.name}}"/>
          </copy>
        </target>

        <target name="{$MODULE_NAME}.copyTestResources" depends="{$MODULE_NAME}.checkDirs"
                if="available.tests.rc.{$MODULE_NAME}">
          <copy todir="${{dir.class.testcode}}/{$MODULE_NAME}" failonerror="true" overwrite="true" preservelastmodified="true">
            <fileset dir="${{dir.project}}/{$MODULE_DIR}/${{subdir.resources.tests}}"/>
          </copy>
        </target>

        <target name="{$MODULE_NAME}.doCompile" if="available.src.{$MODULE_NAME}">
          <xsl:attribute name="depends">
            <xsl:value-of select="$MODULE_NAME"/>
            <xsl:text>.checkDirs</xsl:text>
            <xsl:for-each select="depends">
              <xsl:text>,</xsl:text>
              <xsl:value-of select="@module"/>
              <xsl:text>.compile</xsl:text>
            </xsl:for-each>
          </xsl:attribute>

          <mkdir dir="${{dir.class.prodcode}}/{$MODULE_NAME}"/>
          <gjc
            srcdir="${{dir.project}}/{$MODULE_DIR}/${{subdir.java.production}}"
            destdir="${{dir.class.prodcode}}/{$MODULE_NAME}"
            classpathref="classpath.{$MODULE_NAME}"
            />
        </target>

        <target name="{$MODULE_NAME}.compile" depends="{$MODULE_NAME}.doCompile, {$MODULE_NAME}.copyResources, {$MODULE_NAME}.overrideResources"/>

        <target name="{$MODULE_NAME}.doCompileTests" if="available.tests.{$MODULE_NAME}" unless="without.tests">
          <xsl:attribute name="depends">
            <xsl:value-of select="$MODULE_NAME"/>
            <xsl:text>.checkDirs,</xsl:text>
            <xsl:value-of select="$MODULE_NAME"/>
            <xsl:text>.compile</xsl:text>
            <xsl:for-each select="depends">
              <xsl:text>,</xsl:text>
              <xsl:value-of select="@module"/>
              <xsl:text>.compileTests</xsl:text>
            </xsl:for-each>
          </xsl:attribute>

          <mkdir dir="${{dir.class.testcode}}/{$MODULE_NAME}"/>
          <gjc
            srcdir="${{dir.project}}/{$MODULE_DIR}/${{subdir.java.tests}}"
            destdir="${{dir.class.testcode}}/{$MODULE_NAME}"
            classpathref="classpath.tests.{$MODULE_NAME}"
            />
        </target>

        <target name="{$MODULE_NAME}.compileTests"
                depends="{$MODULE_NAME}.doCompileTests, {$MODULE_NAME}.copyTestResources"/>

        <target name="{$MODULE_NAME}.test" if="available.tests.{$MODULE_NAME}" unless="without.tests">
          <xsl:attribute name="depends">
            <xsl:value-of select="$MODULE_NAME"/>
            <xsl:text>.checkDirs</xsl:text>
            <xsl:for-each select="depends">
              <xsl:text>,</xsl:text>
              <xsl:value-of select="@module"/>
              <xsl:text>.test</xsl:text>
            </xsl:for-each>
            <xsl:text>,</xsl:text>
            <xsl:value-of select="$MODULE_NAME"/>
            <xsl:text>.compileTests</xsl:text>
          </xsl:attribute>

          <mkdir dir="${{dir.test.results}}"/>
          <echo message="Testing with: ${{jdk}}/bin/java"/>
          <junit fork="true" forkmode="once" printsummary="true" haltonfailure="true">
            <jvmarg value="-Djava.awt.headless=true"/>
            <formatter type="xml"/>
            <classpath refid="classpath.tests.{$MODULE_NAME}"/>
            <batchtest todir="${{dir.test.results}}">
              <fileset dir="${{dir.project}}/{$MODULE_DIR}/${{subdir.java.tests}}" includes="**/*Test.java"/>
              <fileset dir="${{dir.project}}/{$MODULE_DIR}/${{subdir.java.tests}}" includes="**/*Tests.java"/>
            </batchtest>
          </junit>
        </target>
      </xsl:for-each>
    </project>
  </xsl:template>
</xsl:stylesheet>
