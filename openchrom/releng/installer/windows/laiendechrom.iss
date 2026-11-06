#ifndef ProductDir
  #error ProductDir must be defined. Pass /DProductDir=... to ISCC.exe.
#endif

#ifndef OutputDir
  #define OutputDir "."
#endif

#ifndef RepoRoot
  #define RepoRoot "..\..\.."
#endif

#define AppName "莱恩德色谱工作站"
#define AppVersion "1.0.0"
#define AppPublisher "Laiende"
#define AppExeName "laiendechrom.exe"
#define MaxInstallPathLength 20

[Setup]
AppId={{A6B6E6E3-2F16-4C9F-B86B-8D72BDF0ED22}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher={#AppPublisher}
DefaultDirName={sd}\LaiendeChrom
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
OutputDir={#OutputDir}
OutputBaseFilename=LaiendeChrom_Setup
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
PrivilegesRequired=admin
SetupIconFile={#RepoRoot}\products\net.openchrom.rcp.compilation.community.product\icons\windows\Icon.ico
UninstallDisplayIcon={app}\laiendechrom.ico
VersionInfoVersion={#AppVersion}
VersionInfoCompany={#AppPublisher}
VersionInfoDescription={#AppName} Setup
VersionInfoProductName={#AppName}
VersionInfoProductVersion={#AppVersion}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#ProductDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#RepoRoot}\products\net.openchrom.rcp.compilation.community.product\icons\windows\Icon.ico"; DestDir: "{app}"; DestName: "laiendechrom.ico"; Flags: ignoreversion

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExeName}"; IconFilename: "{app}\laiendechrom.ico"
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExeName}"; IconFilename: "{app}\laiendechrom.ico"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExeName}"; Description: "{cm:LaunchProgram,{#AppName}}"; Flags: nowait postinstall skipifsilent

[Code]
procedure CleanupWorkspaceLogs;
var
  MetadataDir: String;
  FindRec: TFindRec;
begin
  MetadataDir := GetEnv('USERPROFILE') + '\LaiendeChrom\.metadata';
  if DirExists(MetadataDir) then begin
    DeleteFile(MetadataDir + '\.log');
    if FindFirst(MetadataDir + '\.bak_*.log', FindRec) then begin
      try
        repeat
          DeleteFile(MetadataDir + '\' + FindRec.Name);
        until not FindNext(FindRec);
      finally
        FindClose(FindRec);
      end;
    end;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssInstall then begin
    CleanupWorkspaceLogs;
  end;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  InstallDir: String;
begin
  Result := True;
  if CurPageID = wpSelectDir then begin
    InstallDir := ExpandConstant('{app}');
    if Length(InstallDir) > {#MaxInstallPathLength} then begin
      MsgBox('安装目录路径过长，可能导致 Eclipse 插件文件解压失败。' + #13#10 + #13#10 +
        '请安装到较短路径，例如：C:\LaiendeChrom 或 E:\LC。', mbError, MB_OK);
      Result := False;
    end;
  end;
end;
