package modules.unreal
class UnrealOperations {
    // Class property for storing the environment variable
    String ENGINE_DIR
    String P4_CHANGELIST
    String STREAM_NAME
    String BUILD_TYPE
    String P4_STREAM
    def steps
    // Constructor to initialize the environment variable
    UnrealOperations(def steps, def env) {
        this.steps = steps
        this.ENGINE_DIR = env.ENGINE_DIR
        this.P4_CHANGELIST = env.P4_CHANGELIST
        this.P4_STREAM = env.P4_STREAM
        this.STREAM_NAME = env.STREAM_NAME
        this.BUILD_TYPE = env.BUILD_TYPE
    }
    // Method for the setup operation
    void setup() {
        steps.bat """
        python ${ENGINE_DIR}\\.lightfury\\ci\\scripts\\jobs\\modify_setup_bat_file.py
        call ${ENGINE_DIR}\\Setup.bat --force
        """
    }

    // Method for generating project files
    void generateProjectFiles() {
        steps.bat """
        call ${ENGINE_DIR}\\GenerateProjectFiles.bat
        """
    }

    // Common method to execute Python scripts
    private void runPythonScript(String scriptPath) {
        steps.bat "python ${ENGINE_DIR}\\.lightfury\\ci\\scripts\\jobs\\${scriptPath}"
    }

    // Common method to run UAT commands
    private void runUAT(String command, Map<String, String> params, boolean allowEmptyFlags = false) {
        def extraFlags = params.remove('additionalFlags')
        def paramString = params.collect { key, value -> 
            if (value) {
                "-${key}=\"${value}\""
            } else if (allowEmptyFlags) {
                "-${key}"
            } else {
                ""
            }
        }.join(" ")
		// Append additional flags (if any) at the end.
        if (extraFlags) {
            paramString = paramString + " " + extraFlags
        }
        command += " " + paramString

        steps.bat """
        call ${ENGINE_DIR}\\Engine\\Build\\BatchFiles\\RunUAT.bat ${command}
        """
    }

    // Method to build the Editor
    void buildEditor(String platform = "Win64") {
        runPythonScript("set_version_name.py")
        runUAT("BuildEditor", [
            project  : "${ENGINE_DIR}\\Titan\\Titan.uproject",
            platform : platform
        ])
    }

    void deleteLocalBuilds(String baseBuildDir){
     // Clear the LocalBuilds directory before starting the build
        steps.bat """
        if exist "${baseBuildDir}" (
            echo Deleting existing directory...
            rmdir /s /q "${baseBuildDir}"
        ) else (
            echo Directory does not exist, skipping delete.
        )
        """

        // Ensure baseBuildDir is created
        steps.bat """
        if not exist "${baseBuildDir}" (
            echo Creating directory...
            mkdir "${baseBuildDir}"
        ) else (
            echo Directory already exists, skipping creation.
        )
        """
    }

    void runTurnKey(String command = "InstallSdk", String platform = "Android",String additionalFlags = ""){
        runUAT("turnkey", [
            command  : command,
            platform : platform
        ]+ (additionalFlags ? [additionalFlags: additionalFlags] : [:]))
    }

    // Method to build the Game
    void buildGame(String platform = "Win64", String configuration = "Test", String additionalFlags = "-notools") {
        runUAT("BuildGame", [
            project       : "${ENGINE_DIR}\\Titan\\Titan.uproject",
            platform      : platform,
            configuration : configuration
        ] + (additionalFlags ? [additionalFlags: additionalFlags] : [:]))
    }

    // Method to build, cook, and run the game
    void buildCookRun(String platform = "Win64", String target= "Titan", String configuration = "Test", String cookFlavor = "ASTC", String additionalFlags = "") {
          def baseBuildDir = "${ENGINE_DIR}\\LocalBuilds"
          deleteLocalBuilds(baseBuildDir)
          def archiveDirectory = "${baseBuildDir}\\${platform}_${target}\\CL-${P4_CHANGELIST}-${STREAM_NAME}-${target}-${configuration}-${platform}"
          if (BUILD_TYPE ==  "Nightly" ){
            archiveDirectory+="-Nightly"
          }
          runUAT("BuildCookRun", [
            project          : "${ENGINE_DIR}\\Titan\\Titan.uproject",
            platform         : platform,
            target           : target,
            configuration    : configuration,
            cookflavor       : cookFlavor,
            archivedirectory : archiveDirectory
        ] + (additionalFlags ? [additionalFlags: additionalFlags] : [:]))
    }

	// runBuildGraph Parameters:
	// 1. script        : Required. Path to the BuildGraph script XML file.
	// 2. target        : Required. BuildGraph target to execute.
	//                    - Use "Submit To Perforce for UGS" to submit binaries.
	//                    - Use "Stage for UGS" to skip Perforce submission.
	// 3. parameters    : Optional. Map of additional BuildGraph args.
	//                    - Use key-value for set: params: ["set:EditorTarget": "TitanEditor"]
	//                    - Use empty value for flags: ["submit": "", "p4": "", "BuildMachine": ""]

    void runBuildGraph(String script, String target, Map<String, String> parameters = [:]) {
        // Add required parameters
        def args = [
            Script: script,
            Target: target
        ]
        // Merge with user-provided parameters
        args.putAll(parameters)

        // Enable flag handling for empty values only for BuildGraph
        runUAT("BuildGraph", args, true)
    }
}

