ARG BASE_IMAGE
FROM $BASE_IMAGE


COPY ./scripts/android_sdk_dependencies.ps1 .

# Run the PowerShell script with the Set-ExecutionPolicy command
RUN powershell -Command "Set-ExecutionPolicy Bypass -Scope Process -Force; ./android_sdk_dependencies.ps1"

# Set AWS variables
ARG AWS_ACCESS_KEY AWS_SECRET_ACCESS_KEY AWS_DEFAULT_REGION ANDROID_HOME ANDROID_SDK_ROOT
ENV AWS_ACCESS_KEY=$AWS_ACCESS_KEY AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
    AWS_DEFAULT_REGION=$AWS_DEFAULT_REGION ANDROID_HOME=$ANDROID_HOME ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT

ENV ANDROID_HOME="C:\Users\ContainerAdministrator\AppData\Local\Android\Sdk" \
    ANDROID_SDK_ROOT="C:\Users\ContainerAdministrator\AppData\Local\Android\Sdk"

# Install Python and Firebase CLI
RUN choco install -y python3 --version=3.12
