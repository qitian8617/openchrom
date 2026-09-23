; Baijiu FID workstation installer.
; SourceDir is the materialized Tycho win64 product (baijiu-fid.exe + JustJ),
; not a PDE Product Export folder.
;
; Default layout after packaging\build-baijiu-win64.ps1 -Stage:
;   E:\OpenChrom\baijiu-fid-workstation\baijiu-fid.exe
;   E:\OpenChrom\baijiu-fid-workstation\baijiu-fid.ini   (-Xmx4096m)
;   E:\OpenChrom\baijiu-fid-workstation\jre\             (JustJ 25, installMode root)
;   E:\OpenChrom\baijiu-fid-workstation\plugins\
;   E:\OpenChrom\baijiu-fid-workstation\features\
;
; Override the source tree without editing this file:
;   ISCC /DSourceRoot=D:\some\win32\win32\x86_64 packaging\BaijiuFID-Setup.iss

#ifndef SourceRoot
#define SourceRoot "E:\OpenChrom\baijiu-fid-workstation"
#endif

#define MyAppName "白酒 FID 工作站"
#define MyAppVersion "1.6.32"
#define MyAppExeName "baijiu-fid.exe"

[Setup]
AppId={{7C4E9A21-6B18-4F0D-9E55-3A8C1D0B6F42}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher=OpenChrom
DefaultDirName={autopf}\BaijiuFID
DefaultGroupName={#MyAppName}
OutputBaseFilename=BaijiuFID-Setup
OutputDir=..\..\installer
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
SourceDir={#SourceRoot}
UninstallDisplayIcon={app}\{#MyAppExeName}

[Files]
Source: "*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "启动 {#MyAppName}"; Flags: nowait postinstall skipifsilent
