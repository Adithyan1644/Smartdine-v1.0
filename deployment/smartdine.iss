[Setup]
AppName=Surabhi SmartDine
AppVersion=1.0
DefaultDirName={commonpf}\SurabhiSmartDine
DefaultGroupName=Surabhi SmartDine
OutputBaseFilename=SmartDine_Setup
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin

[Files]
Source: "smartdine-heart.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "smartdine-service.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "smartdine-service.xml"; DestDir: "{app}"; Flags: ignoreversion
Source: "jre\*"; DestDir: "{app}\jre"; Flags: recursesubdirs createallsubdirs
Source: "postgresql-16-windows-x64.exe"; DestDir: "{tmp}"; Flags: deleteafterinstall

[Icons]
Name: "{commondesktop}\Surabhi SmartDine"; Filename: "{app}\jre\bin\javaw.exe"; Parameters: "-jar ""{app}\smartdine-heart.jar"""

[Run]
; 1. Silently install PostgreSQL in the background
Filename: "{tmp}\postgresql-16-windows-x64.exe"; \
  Parameters: "--mode unattended --unattendedmodeui none --postgrespassword admin123 --port 5432"; \
  StatusMsg: "Installing Database Engine (This may take a minute)..."; Flags: runhidden

; 2. Silently bypass the Windows Firewall on Port 8080
Filename: "netsh"; \
  Parameters: "advfirewall firewall add rule name=""SmartDine POS Gateway"" dir=in action=allow protocol=TCP localport=8080 profile=any"; \
  StatusMsg: "Configuring Network Security..."; Flags: runhidden

; 3. Register the Spring Boot service
Filename: "{app}\smartdine-service.exe"; Parameters: "install"; StatusMsg: "Configuring Core Services..."; Flags: runhidden

; 4. Start the service
Filename: "{app}\smartdine-service.exe"; Parameters: "start"; StatusMsg: "Starting Core Services..."; Flags: runhidden
