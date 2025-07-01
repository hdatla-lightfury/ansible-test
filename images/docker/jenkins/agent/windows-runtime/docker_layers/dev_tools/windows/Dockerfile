ARG BASE_IMAGE
FROM ${BASE_IMAGE}

# Set environment variables for non-interactive installations
ENV ChocolateyUseWindowsCompression=false

# Set the execution policy to RemoteSigned and install Chocolatey
RUN powershell -Command Set-ExecutionPolicy RemoteSigned -Scope Process -Force; \
    Invoke-WebRequest -Uri https://chocolatey.org/install.ps1 -OutFile install.ps1; \
    powershell -File install.ps1

# Increase the paging file size
RUN powershell -Command "Set-ItemProperty -Path 'HKLM:\SYSTEM\CurrentControlSet\Control\Session Manager\Memory Management' -Name 'PagingFiles' -Value 'C:\pagefile.sys 16384 32768'"

## To install gcc for cargo package manager
RUN choco install mingw -y

# Install essential tools for p4cli
# Install OpenJDK 17 and P4
RUN choco install -y p4 --install-arguments '"--passive --includeRecommended"' --ignore-checksums
RUN choco install -y openjdk --version=17.0.2 --ignore-checksums

RUN choco install -y git --params "/GitAndUnixToolsOnPath"

# Install Visual Studio Build Tools for UE 5.4
RUN curl -L -o vs_buildtools.exe https://aka.ms/vs/17/release.ltsc.17.8/vs_buildtools.exe && \
    vs_buildtools.exe --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --quiet --wait && \
    del vs_buildtools.exe

# Install CMake
RUN choco install -y cmake

# Install .NET Framework SDK 4.8
RUN choco install -y netfx-4.8-devpack

# Install Windows SDK
RUN choco install -y windows-sdk-10.1 --params "'--add Microsoft.VisualStudio.Component.Windows10SDK --includeRecommended --quiet --wait'" --ignore-checksums -y || type C:\ProgramData\chocolatey\logs\chocolatey.log

# Python to run custom scripts in jenkins pipelines
RUN choco install -y python3 --version=3.12

# Update PATH to include Debug Tools
ENV DebugToolsPath="C:\\Program Files (x86)\\Windows Kits\\10\\Debuggers\\x64"

## TODO: Move all these to AWS secrets manager and use python script to retrieve on the run
## Tracking Ticket : https://lightfurygames.atlassian.net/browse/TITAN-2114
ARG P4_USER P4_PASS P4_PORT P4_CLIENT P4_BASE_CLIENT P4_STREAM_GAME P4_STREAM_ENGINE P4_WORKSPACE ENGINE_DIR PERFORCE_AGENT_WORKSPACE

ENV P4_USER=$P4_USER P4_PASS=$P4_PASS P4_PORT=$P4_PORT P4_CLIENT=$P4_CLIENT \
    P4_BASE_CLIENT=$P4_BASE_CLIENT P4_STREAM_GAME=$P4_STREAM_GAME P4_STREAM_ENGINE=$P4_STREAM_ENGINE \
    P4_WORKSPACE=$P4_WORKSPACE ENGINE_DIR=$ENGINE_DIR PERFORCE_AGENT_WORKSPACE=${PERFORCE_AGENT_WORKSPACE}

## Enables long paths and long commands to work in windows beyond the 256 char limit
RUN reg add HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\FileSystem /v LongPathsEnabled /t REG_DWORD /d 1 /f

RUN setx PATH "%PATH%;%DebugToolsPath%"
