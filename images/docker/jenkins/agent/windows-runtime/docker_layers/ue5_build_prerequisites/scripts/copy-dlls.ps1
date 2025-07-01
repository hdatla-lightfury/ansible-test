$sourceDir = "C:\\Windows\\System32"
$destinationDir = "C:\\GatheredDLLs"
$files = Get-Content "C:\\dll-files.txt"


# Ensure the destination directory exists
if (-not (Test-Path -Path $destinationDir)) {
    New-Item -Path $destinationDir -ItemType Directory
}
# Create destination directory if it doesn't exist
if (-Not (Test-Path -Path $destinationDir)) {
    New-Item -ItemType Directory -Path $destinationDir
}

# Loop through each DLL file and copy
foreach ($file in $files) {
    $sourcePath = Join-Path $sourceDir $file
    $destinationPath = Join-Path $destinationDir $file
    Copy-Item -Path $sourcePath -Destination $destinationPath -Force
}
