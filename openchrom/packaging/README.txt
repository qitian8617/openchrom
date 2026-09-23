Baijiu FID workstation — Windows product (no PDE Export)
========================================================

PDE Product Export fails when the IDE looks up a timestamped ChemClipse
bundle that is not in the workspace, for example:

  Unable to find plug-in org.eclipse.chemclipse.rcp.app_0.9.0.202609170830

Tycho resolves that feature from the target platform (0.0.0) and
tycho-p2-director-plugin:materialize-products writes the win64 tree.
Same mechanism as the community product under
products/net.openchrom.rcp.compilation.community.product/target/products.

JDK
---
Maven uses JAVA_HOME, not whichever java.exe is first on PATH.
This tree compiles as JavaSE-25 (CI image: maven:3.9-eclipse-temurin-25).
If `mvn -version` prints 1.8, or JAVA_HOME is a JDK 21, the build will not
compile. Set JAVA_HOME to JDK 25 and put %JAVA_HOME%\bin first on PATH.

Command (from the openchrom/ directory)
---------------------------------------
mvn -f releng/net.openchrom.aggregator/pom.xml -pl net.openchrom:net.openchrom.targetplatform,net.openchrom:openchrom.compilation.baijiu -am install -Pwin32-x86_64 -DskipTests

- install, not package. materialize-products and archive-products are
  bound to the install phase in openchrom/pom.xml.
- Do not pass -Pci. That profile sets those director goals to phase none
  (CI only verifies and tests; it does not assemble products).
- -Pwin32-x86_64 keeps resolution on win32/win32/x86_64. The baijiu product
  pom sets that same single environment, so the director does not also
  assemble linux, macOS, or Windows aarch64.
- -pl uses Maven coordinates. A path such as products/... is relative to
  releng/net.openchrom.aggregator and does not select this module.
  -am builds the OSGi/feature closure Tycho wires onto the product, not
  the whole OpenChrom reactor. The target-platform module is listed
  explicitly so a clean machine builds net.openchrom.targetplatform
  before resolution. That module has its own pom.xml parented at the
  current master (1.6.32 / Tycho 5.0.4). Do not delete it: without it,
  pomless parents the target at releng/.polyglot.pom.tycho, and a
  leftover from an OpenChrom 1.6.20 build pulls Tycho 5.0.3 into this
  reactor ("Several versions of Tycho plugins are configured").
- -DskipTests skips test execution. This repo does not run the product
  materialize in GitHub Actions.

Where the win64 folder lands
-----------------------------
products/net.openchrom.rcp.compilation.baijiu.product/target/products/net.openchrom.rcp.compilation.baijiu.product.id/win32/win32/x86_64/

That directory is the product root:
  baijiu-fid.exe
  baijiu-fid.ini          (-Xms512m -Xmx4096m from the .product vmArgs)
  jre\                    JustJ 25 (org.eclipse.justj.openjdk.hotspot.jre.full.stripped, installMode root)
  plugins\  features\

archive-products also runs on install. Any zip is secondary; Inno Setup
consumes the materialized directory.

Stage and installer
-------------------
powershell -File packaging\build-baijiu-win64.ps1
powershell -File packaging\build-baijiu-win64.ps1 -Stage

-Stage mirrors the win64 folder to E:\OpenChrom\baijiu-fid-workstation, then
copies packaging\BaijiuFID.ico into that folder. Then compile
packaging\BaijiuFID-Setup.iss (Inno Setup). The script also installs
compiler:BaijiuFID.ico (next to the .iss) into {app}, so a raw Tycho
folder still ships the logo. Desktop and Start Menu shortcuts set
IconFilename to {app}\BaijiuFID.ico. They must not inherit baijiu-fid.exe:
Eclipse's launcher brander only replaces icons from an uncompressed BMP
ICO, and a missed brand leaves the Eclipse icon on the shortcut.
DefaultDirName is {sd}\BaijiuFID (C:\BaijiuFID), not {autopf}\BaijiuFID,
so the plugin tree stays under MAX_PATH. License files stay a per-user
drop-in (%USERPROFILE%\OpenChrom\licenses\baijiu-fid.bjlic) and are not
part of the installer tree.

Heap
----
The .product vmArgs include -Xms512m -Xmx4096m so a plant PC is not left
on the JVM default (an IDE launch with no -Xmx has been observed around
94M). PDE Run As uses a saved launch config: Synchronize from
openchrom.compilation.baijiu.product, or create a new launch, so the
config picks up the new vmArgs. The Tycho ini is written from the
.product directly and does not need Synchronize.

Full reactor (also materializes the community product for win64 only):
mvn -f releng/net.openchrom.aggregator/pom.xml install -Pwin32-x86_64 -DskipTests
