@echo off
net stop smartdine-heart
timeout /t 3 /nobreak > null
copy /y "C:\SmartDine\temp\smartdine-heart.jar" "C:\Program Files\SurabhiSmartDine\smartdine-heart.jar"
del /q "C:\SmartDine\temp\*.*"
net start smartdine-heart
exit
