package modules.aws

class AwsOperations {

    // Class properties for env variables
    String ENGINE_DIR
    String AWS_ACCESS_KEY
    String AWS_SECRET_ACCESS_KEY
    String AWS_REGION
    def steps

    // Constructor to initialize the env variables
    AwsOperations(def steps, def env) {
        this.ENGINE_DIR = env.ENGINE_DIR
        this.AWS_ACCESS_KEY = env.AWS_ACCESS_KEY
        this.AWS_SECRET_ACCESS_KEY = env.AWS_SECRET_ACCESS_KEY
        this.AWS_REGION = env.AWS_DEFAULT_REGION
        this.steps = steps
    }

    // Method to upload APK to Firebase
	void uploadToS3(String bucketName, String subFolderName) {
		String path = "${ENGINE_DIR}\\\\LocalBuilds\\\\${subFolderName}"
		
		steps.bat """
		@echo off
		setlocal EnableDelayedExpansion
		
		REM Define path variable
		set "BUILD_PATH=${path}"
	
		REM Create AWS config directory if not exists
		if not exist %USERPROFILE%\\\\.aws mkdir %USERPROFILE%\\\\.aws
	
		REM Configure AWS CLI region and output format securely
		echo [default] > %USERPROFILE%\\\\.aws\\\\config
		echo region=${AWS_REGION} >> %USERPROFILE%\\\\.aws\\\\config
		echo output=json >> %USERPROFILE%\\\\.aws\\\\config
	
		REM Configure AWS CLI credentials securely
		echo [default] > %USERPROFILE%\\\\.aws\\\\credentials
		(
			echo aws_access_key_id=${AWS_ACCESS_KEY}
			echo aws_secret_access_key=${AWS_SECRET_ACCESS_KEY}
		) >> %USERPROFILE%\\\\.aws\\\\credentials
	
		REM Run the Python script to upload the game to AWS S3
		python ${ENGINE_DIR}\\\\.lightfury\\\\ci\\\\scripts\\\\jobs\\\\aws_services_handler.py upload_to_s3 --bucket-name """ + bucketName + """ --path "!BUILD_PATH!" --region ${AWS_REGION}
	
		endlocal
		"""
	}
}