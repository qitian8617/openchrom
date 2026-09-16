Baijiu FID pilot p2 category / update site
==========================================

Two vehicles:

1. **Dedicated product (target operator UI):**
   products/net.openchrom.rcp.compilation.baijiu.product
   Window title 白酒 FID 工作站. Architecture:
   plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/白酒FID专用壳架构.md

2. **This p2 site** — install onto an existing OpenChrom community build.
   Community UI remains supported. Do not rebuild/replace the community
   product for every plant drop.

Feature: net.openchrom.xxd.processor.supplier.baijiu.pilot.feature
  includes 白酒分析 + temperature.ui reverse-control

Engineer steps (Windows): see
  plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/GCWS-INSTALL.md

Eclipse: File → Export → Deployable Features → Baijiu FID Pilot
  or Tycho: this module packages an eclipse-repository.

Plant: Help → Install New Software → Local → this folder.
License: demo/sample-pilot.bjlic on the Baijiu workbench.
Operator handbook (Chinese):
  plugins/net.openchrom.xxd.processor.supplier.baijiu.ui/docs/白酒FID试点操作手册.md
