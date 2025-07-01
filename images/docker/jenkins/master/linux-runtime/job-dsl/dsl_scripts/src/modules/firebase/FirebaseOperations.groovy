package modules.firebase

class FirebaseOperations {

    // Class properties for env variables
    String ENGINE_DIR
    String AWS_ACCESS_KEY
    String AWS_SECRET_ACCESS_KEY
    String AWS_REGION
    def steps
    def env

    // Constructor to initialize the env variables
    FirebaseOperations(def steps, def env) {
        this.env = env
        this.ENGINE_DIR = env.ENGINE_DIR
        this.AWS_ACCESS_KEY = env.AWS_ACCESS_KEY
        this.AWS_SECRET_ACCESS_KEY = env.AWS_SECRET_ACCESS_KEY
        this.AWS_REGION = env.AWS_DEFAULT_REGION
        this.steps = steps
    }

    // Method to upload APK to Firebase
    void uploadApkToFirebase() {
        steps.bat """
        @echo off
        // Create AWS config directory
        mkdir %USERPROFILE%\\\\.aws

        // Configure AWS CLI credentials
        echo [default] > %USERPROFILE%\\\\.aws\\\\config
        echo region=${AWS_REGION} >> %USERPROFILE%\\\\.aws\\\\config
        echo output=json >> %USERPROFILE%\\\\.aws\\\\config

        // Configure AWS CLI credentials
        echo [default] > %USERPROFILE%\\\\.aws\\\\credentials
        @echo off
        echo aws_access_key_id=${AWS_ACCESS_KEY} >> %USERPROFILE%\\\\.aws\\\\credentials
        echo aws_secret_access_key=${AWS_SECRET_ACCESS_KEY} >> %USERPROFILE%\\\\.aws\\\\credentials

        REM Save the original PYTHONPATH
        set ORIGINAL_PYTHONPATH=%PYTHONPATH%

        REM Append new value
        set PYTHONPATH=%PYTHONPATH%;${ENGINE_DIR}\\.lightfury

        python ${ENGINE_DIR}\\\\.lightfury\\\\ci\\\\scripts\\\\jobs\\\\firebase_release_handler.py

        REM Restore the original PYTHONPATH
        set PYTHONPATH=%ORIGINAL_PYTHONPATH%
        """
    }
}
