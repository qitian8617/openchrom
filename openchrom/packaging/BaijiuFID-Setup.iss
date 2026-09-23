; Baijiu FID workstation installer.
; SourceDir is the materialized Tycho win64 product (baijiu-fid.exe + JustJ),
; not a PDE Product Export folder.
;
; Default layout after packaging\build-baijiu-win64.ps1 -Stage:
;   E:\OpenChrom\baijiu-fid-workstation\baijiu-fid.exe
;   E:\OpenChrom\baijiu-fid-workstation\baijiu-fid.ini   (-Xmx4096m)
;   E:\OpenChrom\baijiu-fid-workstation\BaijiuFID.ico   (company logo; -Stage copies packaging\BaijiuFID.ico)
;   E:\OpenChrom\baijiu-fid-workstation\jre\             (JustJ 25, installMode root)
;   E:\OpenChrom\baijiu-fid-workstation\plugins\
;   E:\OpenChrom\baijiu-fid-workstation\features\
;
; Desktop and Start Menu shortcuts set IconFilename to {app}\BaijiuFID.ico.
; Do not let them inherit baijiu-fid.exe: if the launcher brand step misses
; an icon, Windows still shows the Eclipse icon from the exe template.
; The .ico is taken from this script's directory (compiler:), so it ships
; even when SourceRoot is a raw Tycho folder.
;
; Install under {sd}\BaijiuFID (C:\BaijiuFID), not {autopf}\BaijiuFID.
; Program Files plus the Eclipse plugin tree exceeds MAX_PATH on plant PCs.
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
DefaultDirName={sd}\BaijiuFID
DefaultGroupName={#MyAppName}
OutputBaseFilename=BaijiuFID-Setup
OutputDir=..\..\installer
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
SourceDir={#SourceRoot}
UninstallDisplayIcon={app}\BaijiuFID.ico

[Files]
Source: "*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion; Excludes: "BaijiuFID.ico"
Source: "compiler:BaijiuFID.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\BaijiuFID.ico"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\BaijiuFID.ico"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "启动 {#MyAppName}"; Flags: nowait postinstall skipifsilent
