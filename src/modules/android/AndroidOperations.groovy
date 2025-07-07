        package modules.android
        import modules.unreal.UnrealOperations

        class AndroidOperations {

            // Environment variables as class properties
            def steps
            UnrealOperations unrealOps
            String P4_STREAM
            String STREAM_NAME
            String P4_WORKSPACE_DIR
            String ENGINE_DIR

            // Constructor to initialize the environment variables
            AndroidOperations(def steps, def env) {
                this.unrealOps = new UnrealOperations(steps, env)
                this.steps = steps
                this.P4_STREAM = env.P4_STREAM
                this.STREAM_NAME = env.STREAM_NAME
                this.P4_WORKSPACE_DIR =  env.P4_WORKSPACE_DIR
                this.ENGINE_DIR = env.ENGINE_DIR  // Setting the environment variable dynamically
            }

            // Install SDK method
            void installSdk() {
                steps.bat """
                powershell -Command "(1..10 | ForEach-Object {'Yes'; Start-Sleep -Milliseconds 100}) | & 'C:\\Users\\ContainerAdministrator\\AppData\\Local\\Android\\Sdk\\cmdline-tools\\latest\\bin\\sdkmanager.bat' --licenses"
                powershell -Command " C:\\Users\\ContainerAdministrator\\AppData\\Local\\Android\\Sdk\\cmdline-tools\\latest\\bin\\sdkmanager.bat  --install 'platforms;android-33' 'platform-tools' 'build-tools;33.0.1' 'cmdline-tools;latest' 'ndk;25.1.8937393' 'cmake;3.10.2.4988404' 'emulator' 'extras;google;usb_driver' "
                """
            }

            void buildUnrealGameForAndroid(String targetConfig="Test") {
                setupAndroidJavaEnv()
                setupUnrealAndroidDevEnv()
                unrealOps.buildGame("Android", targetConfig, "-notools")
            }
            void setupAndroidJavaEnv(){
                steps.bat """
                set "JAVA_HOME=C:\\Program Files\\Android\\Android Studio\\jbr"
                set "PATH=%JAVA_HOME%\\bin;%PATH%"
                echo Using Java version:
                java -version
                """
            }

            void setupUnrealAndroidDevEnv(){
                steps.bat """
                set "JAVA_HOME=C:\\Program Files\\Android\\Android Studio\\jbr"
                set "PATH=%JAVA_HOME%\\bin;%PATH%"
                echo Using Java version:
                java -version

                :: to check if this is really needed, was initially getting some errors here, so added it to just be sure
                call ${ENGINE_DIR}\\Engine\\Build\\BatchFiles\\RunUAT.bat turnkey -command=InstallSdk -platform=Android -NeededOnly -BestAvailable

                REM Set the base NDK path
                set "NDK_BASE_PATH=C:\\Users\\ContainerAdministrator\\AppData\\Local\\Android\\Sdk\\ndk"
                REM Check if the directory exists
                if not exist "%NDK_BASE_PATH%" (
                    echo NDK directory not found: %NDK_BASE_PATH%
                    exit /b 1
                )
                REM Find the first version directory in the NDK path
                for /d %%D in ("%NDK_BASE_PATH%\\*") do (
                    set NDK_VERSION=%%~nxD
                    goto :found
                )
                :found
                REM If no version is found, display an error and exit
                if "%NDK_VERSION%"=="" (
                    echo No NDK version found in directory: %NDK_BASE_PATH%
                    exit /b 1
                )
                REM Set NDK_ROOT and NDKROOT based on the found version
                set NDK_ROOT=%NDK_BASE_PATH%\\%NDK_VERSION%
                set NDKROOT=%NDK_BASE_PATH%\\%NDK_VERSION%
                REM Output the result
                echo NDK_ROOT environment variable set to: %NDK_ROOT%
                echo NDKROOT environment variable set to: %NDKROOT%
                """
            }

            // Build, cook, and run for android ASTC shipping build
            void buildCookRunAndroidASTC(String buildTarget="Titan", String targetConfig="Test", String additionalFlags = "") {
                setupAndroidJavaEnv()
                setupUnrealAndroidDevEnv()
                // To see if need to add stage, compress flags as well
                unrealOps.buildCookRun("Android", buildTarget, targetConfig, "ASTC", """\
                                                                        -notools \
                                                                        -nop4 \
                                                                        -utf8output \
                                                                        -nocompileeditor \
                                                                        -skipbuildeditor \
                                                                        -unattended \
                                                                        -utf8output \
                                                                        -build \
                                                                        -cook \
                                                                        -stage \
                                                                        -pak \
                                                                        -prereqs \
                                                                        -package \
                                                                        -archive \
                                                                        -iostore \
                                                                  -nocompile \
                                                                        -nocompileuat \
                                                                        -FastCook\
                                                                """ + " ${additionalFlags}"
                )
            }

            void cleanBuildCookRunAndroidASTC(String buildTarget="Titan", String targetConfig="Test")
            {
                 buildCookRunAndroidASTC(buildTarget, targetConfig, "-clean")
            }

            void iterativeBuildCookRunAndroidASTC(String buildTarget="Titan", String targetConfig="Test")
            {
                buildCookRunAndroidASTC(buildTarget, targetConfig, "-iterativecooking")
            }
        }
