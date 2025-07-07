@echo off
REM Accept Android SDK licenses
powershell -Command "(1..10 | ForEach-Object {'Yes'; Start-Sleep -Milliseconds 100}) | & 'C:\Users\ContainerAdministrator\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat' --licenses"

REM Install required Android SDK components
powershell -Command "C:\Users\ContainerAdministrator\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat --install 'platforms;android-33' 'platform-tools' 'build-tools;33.0.1' 'cmdline-tools;latest' 'ndk;25.1.8937393' 'cmake;3.10.2.4988404' 'emulator' 'extras;google;usb_driver'"

REM Installing essential python libraries
pip install -r "C:\jenkins\pip-requirements.txt"

REM Install Rust 1.86.0
choco install rust --version 1.86.0 -y

REM Build symsorter from source
python C:\jenkins\install_symsorter.py

REM Start Jenkins Swarm Agent
java -jar C:\jenkins\swarm-client.jar ^
    -master %SWARM_MASTER% ^
    -name %AGENT_NAME% ^
    -executors %SWARM_EXECUTORS% ^
    -labels "%LABELS%" ^
    -disableSslVerification ^
    -username %SWARM_USER% ^
    -password %SWARM_API_TOKEN%