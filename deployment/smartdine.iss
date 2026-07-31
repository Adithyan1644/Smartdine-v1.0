; =====================================================================
; SURABHI SMARTDINE — WINDOWS SYSTEM DEPLOYMENT BUILDER
; Build: Inno Setup 6.x  |  Target: Windows 10/11 x64
; TEST BUILD — uses system Java, no bundled JRE or PostgreSQL
; =====================================================================

[Setup]
AppId={{A8F9C1D2-3B4E-5F6A-7B8C-9D0E1F2A3B4C}
AppName=Surabhi SmartDine
AppVersion=1.0.4
AppPublisher=Surabhi Software Solutions
AppPublisherURL=https://smartdine-saas.web.app
AppSupportURL=https://smartdine-saas.web.app/support
AppUpdatesURL=https://smartdine-saas.web.app/updates
DefaultDirName={autopf}\SurabhiSmartDine
DefaultGroupName=Surabhi SmartDine
DisableProgramGroupPage=yes
OutputBaseFilename=SmartDine_Setup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=admin

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; ── Core application JAR (actual Maven output filename) ──────────────────────
Source: "..\target\core-heart-1.0.4.jar"; DestDir: "{app}"; DestName: "smartdine-heart.jar"; Flags: ignoreversion

; ── Auto-updater batch script ─────────────────────────────────────────────────
Source: "update.bat"; DestDir: "C:\SmartDine"; Flags: ignoreversion

; ── WinSW service config XML ──────────────────────────────────────────────────
Source: "smartdine-service.xml"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
; Start Menu shortcut — uses system javaw (Java must be installed on POS PC)
Name: "{group}\Surabhi SmartDine"; \
  Filename: "javaw.exe"; \
  Parameters: "-jar ""{app}\smartdine-heart.jar"""

; Desktop shortcut (optional — user must tick the checkbox during install)
Name: "{commondesktop}\Surabhi SmartDine"; \
  Filename: "javaw.exe"; \
  Parameters: "-jar ""{app}\smartdine-heart.jar"""; \
  Tasks: desktopicon

[Run]
; Open port 8080 for inbound connections (Spring Boot local gateway)
Filename: "netsh"; \
  Parameters: "advfirewall firewall add rule name=""SmartDine POS Gateway"" dir=in action=allow protocol=TCP localport=8080 profile=any"; \
  StatusMsg: "Configuring network firewall rule..."; \
  Flags: runhidden

; Launch SmartDine after install (user can uncheck if preferred)
Filename: "javaw.exe"; \
  Parameters: "-jar ""{app}\smartdine-heart.jar"""; \
  Description: "Launch Surabhi SmartDine POS now"; \
  Flags: nowait postinstall skipifsilent

[UninstallRun]
; Remove firewall rule on uninstall
Filename: "netsh"; \
  Parameters: "advfirewall firewall delete rule name=""SmartDine POS Gateway"""; \
  RunOnceId: "RemoveFirewallRule"; \
  Flags: runhidden
