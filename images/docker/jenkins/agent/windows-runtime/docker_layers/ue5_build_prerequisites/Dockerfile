# Use the base Windows Server Core image for Unreal dependencies
ARG BASE_IMAGE
FROM mcr.microsoft.com/windows/server:ltsc2022 as dlls

SHELL ["cmd", "/S", "/C"]

RUN mkdir "C:\\GatheredDlls"

# Copy necessary files
COPY ./config/dll-files.txt C:\\dll-files.txt
COPY ./scripts/copy-dlls.ps1 C:\\copy-dlls.ps1

# Run PowerShell script to copy DLLs
RUN powershell -ExecutionPolicy Bypass -File C:\\copy-dlls.ps1

# Copy the DLL files into the base image
FROM  ${BASE_IMAGE} AS prerequisites

SHELL ["cmd", "/S", "/C"]
RUN mkdir "C:\\GatheredDlls"
COPY --from=dlls "C:\\GatheredDlls\\" "C:\\GatheredDlls\\"

## Remove Duplicate DLL's
COPY ./scripts/remove-duplicate-dlls.ps1 "C:\\remove-duplicate-dlls.ps1"

RUN powershell -ExecutionPolicy Bypass -File C:\\remove-duplicate-dlls.ps1

## TODO: Dont know why but sleep is working, the docker is unable to recognize the GatheredDlls directory, eventhough its in the image if we just run the COPY command
## https://lightfurygames.atlassian.net/browse/TITAN-1774 
RUN sleep 5 && COPY "C:\\GatheredDlls\\" "C:\\Windows\\System32\\"

RUN rmdir /S /Q "C:\\GatheredDlls"
