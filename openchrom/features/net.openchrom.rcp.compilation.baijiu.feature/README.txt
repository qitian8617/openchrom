Baijiu FID Workstation feature (dedicated shell)
================================================

Id: net.openchrom.rcp.compilation.baijiu.feature  1.6.32.qualifier

Includes ChemClipse community compilation (kernel) + Baijiu FID Pilot
(baijiu.ui + temperature.ui) + CSD CDF converter + this product's branding
plug-in. Phase 3 plant home (status + sequence + analysis page). Does
**not** include the OpenChrom community branding product.
Charting (SWTChart) is transitive via the ChemClipse community feature /
target platform — do not require `org.eclipse.swtchart.feature` here
(PDE cannot resolve it unless that feature project is in the workspace).

Always includes OSGi bundle `jakarta.annotation-api` **2.1.1** (Orbit
2026-06). ChemClipse `pcr.ui` / `xxd.ui` Import-Package range is
`[2.1.0,3.0.0)`; SimRel 3.0.0 does **not** satisfy that exclusive upper
bound, so do not rely on 3.0.0 alone. The target platform pins the same
IU (`sequenceNumber` 45). Community `platform.feature` is unchanged.

Used only by openchrom.compilation.baijiu.product.
The community platform.feature is unchanged.

See:
  products/net.openchrom.rcp.compilation.baijiu.product/README.txt
  plugins/.../baijiu.ui/docs/白酒FID专用壳架构.md
