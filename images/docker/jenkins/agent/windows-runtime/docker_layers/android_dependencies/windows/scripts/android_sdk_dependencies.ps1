# Install Android Studio and SDK via Chocolatey if not already installed
function Install-AndroidStudio {
    choco upgrade androidstudio -y --version=2024.1.1.13 --force
}

# Install Android Studio via Chocolatey if not already installed
Install-AndroidStudio

# # Download Command Line Tools
$downloadUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$destinationPath ="C:\AndroidSDK\"
$sdkRootPath = "C:\Users\ContainerAdministrator\AppData\Local\Android\Sdk"
New-Item -ItemType Directory -Path $destinationPath -Force
New-Item -ItemType Directory -Path "$sdkRootPath\platform-tools" -Force
New-Item -ItemType Directory -Path "$sdkRootPath\build-tools\33.0.1" -Force
New-Item -ItemType Directory -Path "$sdkRootPath\cmdline-tools\latest" -Force

$vulkanArchivePath = "$env:TEMP"

$tempZipPath = "$vulkanArchivePath\commandlinetools.zip"

# Create destination directory if it doesn't exist
if (!(Test-Path -Path $destinationPath)) {
    Write-Host "Creating destination directory..."
    New-Item -ItemType Directory -Force -Path $destinationPath
}

# Download the Command Line Tools zip
Write-Host "Downloading Android SDK Command Line Tools..."
Invoke-WebRequest -Uri $downloadUrl -OutFile $tempZipPath

# Unzip the downloaded file
Write-Host "Extracting files to destination directory..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($tempZipPath, "$destinationPath")

$destinationDirectory = "$sdkRootPath\cmdline-tools\latest"
# Move everything, including directories
Move-Item -Path "$destinationPath\cmdline-tools\*" -Destination $destinationDirectory

# Cleanup
Write-Host "Cleaning up temporary files..."
Remove-Item $tempZipPath -Force

$vulkanZipPath = "$vulkanArchivePath\vulkan-runtime-components.zip"

Invoke-WebRequest -Uri "https://sdk.lunarg.com/sdk/download/latest/windows/vulkan-runtime-components.zip?u=" -OutFile $vulkanZipPath

# Define variables for the source ZIP file and destination archive folder


# Expand the archive using the variables
Expand-Archive -Path $vulkanZipPath -DestinationPath $vulkanArchivePath

# Store the source and destination path in a variable
$vulkanSourcePath = "*\x64\vulkan-1.dll"
$vulkanDestinationPath = "C:\Windows\System32\"

# Use the variables in the Copy-Item command
Copy-Item -Path $vulkanSourcePath -Destination $vulkanDestinationPath

Write-Host "Android SDK setup complete."
