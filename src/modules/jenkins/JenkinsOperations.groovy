package modules.jenkins
import jenkins.model.Jenkins
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Collections
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

class JenkinsOperations {

    // Constants for numeric values
    private static final int DEMO_BUILD_NUMBER = 1
    private static final int DEMO_SLEEP_DURATION_MS = 100
    private static final int NO_FREE_NODE_SLEEP_DURATION_MS = 300000
    private static final int DROP_OFFSET = 1
    private static final Map<String, List<String>> PIPELINE_NAME_TO_LABEL_MAPPING = [
        'BuildPublish-WindowsEditor-UGS'      : [
            'windows-development-ugs',
            'windows-mainline-ugs',
            'windows-dev-engine-ugs'
        ],
        'BuildPublish-Android-Firebase'       : [
            'windows-development',
            'windows-mainline',
            'windows-dev-engine',
            'windows-development-nightly',
            'windows-mainline-nightly',
            'windows-dev-engine-nightly'
        ],
        'BuildPublish-Windows-GameLiftStreams': [
            'windows-development',
            'windows-mainline',
            'windows-dev-engine',
            'windows-development-nightly',
            'windows-mainline-nightly',
            'windows-dev-engine-nightly'
        ]
    ]

    def steps
    def env

    JenkinsOperations(def steps, def env) {
        this.steps = steps
        this.env = env
    }

    /**
     * storeEnvVarsToFile - Stores environment variables to a specified file.
     *
     * Parameters:
     *   @param envVars (Map) A map containing environment variable key-value pairs.
     *   @param fileName (String) The file path where the environment variables should be stored.
     *
     * Behavior:
     *   - Constructs a string with each environment variable in the "KEY=VALUE" format using Windows newlines.
     *   - Writes the constructed string to the specified file using the Jenkins writeFile step.
     *   - Echoes a message indicating that the environment variables have been stored.
     */
    def storeEnvVarsToFile(Map envVars, String fileName) {
        def content = envVars.collect { key, value -> "${key}=${value}" }.join('\r\n')
        steps.writeFile file: fileName, text: content
        steps.echo "Environment variables stored in ${fileName}"
    }

    String getNodeLabel(
        String agentPlatform,
        String streamName,
        String buildType,
        String targetPlatform
    ){
        String nodeLabel = agentPlatform
        nodeLabel += streamName ? "-${streamName}" : ''
        nodeLabel += (buildType == 'Nightly') ?  '-nightly' : ''
        nodeLabel += (targetPlatform == 'ugs') ? '-ugs' : ''
        return nodeLabel
    }

    def deleteEnvVarsFile(String fileName) {
        steps.bat """
            @echo off
            if exist "${fileName}" (
                del /F /Q "${fileName}"
                echo File deleted successfully.
            ) else (
                echo File does not exist, skipping deletion.
            )
        """
    }

    /**
     * retrieveEnvVarsFromFile - Retrieves environment variables from a specified file.
     *
     * Parameters:
     *   @param fileName (String) The file path from which the environment variables should be read.
     *
     * Behavior:
     *   - Reads the file content using the Jenkins readFile step.
     *   - Splits the file content into lines based on Windows or Unix newline characters.
     *   - Parses each line in the "KEY=VALUE" format to build a map of environment variables.
     *   - Returns the map of retrieved environment variables.
     */
    def retrieveEnvVarsFromFile(String fileName) {
        def content = steps.readFile file: fileName
        def lines = content.split(/\r?\n/)
        def envVars = [:]
        lines.each { line ->
            if (line && line.contains('=')) {
                // Splitting into key and value. Limiting the split to 2 parts in case value contains '='.
                def parts = line.split('=', 2)
                envVars[parts[0].trim()] = parts[1].trim()
            }
        }
        return envVars
    }

    /**
     * checkAndSkipBuild - Checks if the build should be skipped based on the build number
     * or a specific parameter flag.
     *
     * Parameters:
     *   @param params (Map) A map containing pipeline parameters.
     *
     * Behavior:
     *   - If the build number is DEMO_BUILD_NUMBER or the SKIP_BUILD_TO_REFRESH_UI flag is true,
     *     it echoes a message, interrupts the build executor, and pauses briefly.
     */
    def checkAndSkipBuild(def currentBuild, Map params) {
        def buildNumber = currentBuild.number
        if (buildNumber == DEMO_BUILD_NUMBER ||  !params || params.SKIP_BUILD_TO_REFRESH_UI) {
            steps.echo "This is a demo run of the job to rebuild UI. Exiting pipeline."
            currentBuild.getRawBuild().getExecutor().interrupt(Result.SUCCESS)
            sleep(DEMO_SLEEP_DURATION_MS)
        }
    }
    /**
     * filterParamList - Filters a map of parameters based on specified keys to exclude.
     *
     * Parameters:
     *   @param params (Map) A map containing pipeline parameters to filter.
     *   @param keysToFilter (List<String>) List of parameter keys to exclude from the result.
     * Returns:
     *   @return filteredParams (Map) A map containing filtered params
     * Behavior:
     *   - Takes a map of parameters and a list of keys to filter out.
     *   - Returns a new map containing only parameters whose keys are not in the filter list.
     */
    Map filterParamList(Map params, List<String> keysToFilter) {
        Map filteredParams = params.findAll { key, value -> 
            !keysToFilter.contains(key)
        }
        return filteredParams
    }

    def getNodesForPipeline(String pipelineName){
        List<String> labels = PIPELINE_NAME_TO_LABEL_MAPPING[pipelineName]
        if(!labels){
            steps.error "Pipeline name ${pipelineName} not found in PIPELINE_NAME_TO_LABEL_MAPPING"
        }
        List<String> nodes = []
        for(label in labels){
            nodes.addAll(steps.nodesByLabel(label))
        }
        nodes.add(0, 'NONE')
        nodes.unique()
        return nodes
    }

    def findLockAndRunOnFreeNode(String label, Closure body){
        boolean executed = false

        while(!executed){
            def candidateNodes = steps.nodesByLabel(label)
            Collections.shuffle(candidateNodes)
            for(nodeName in candidateNodes){
                try {
                    steps.lock(resource: "Node-${nodeName}", inversePrecedence: false, skipIfLocked: true){
                        // Once the lock is acquired, immediately run on that node
                        steps.node(nodeName){
                            steps.echo "Running on node ${env.NODE_NAME} with exclusive lock"
                            body()
                            executed = true
                        }
                    }
                }
                catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException fie) {
                    // This exception is thrown when the build is aborted.
                    //  Rethrow it so Jenkins can properly abort the build.
                    throw fie
                }
                catch (err){
                    steps.echo "Caught unknown exception while trying to acquire lock or running job on node ${nodeName} :  ${err}"
                    throw err
                }

                if(executed){
                    return
                }
            }
            steps.echo "No Available Node found for label ${label}. Sleeping for 5 mins"
            sleep(NO_FREE_NODE_SLEEP_DURATION_MS)
        }
    }

    /**
     * validateBuildParameters - Validates that specified parameters in the map are non-empty.
     *
     * Parameters:
     *   @param params        (Map) A map containing pipeline parameters.
     *   @param requiredKeys  (List) A list of keys (String) that must have non-empty values.
     *
     * Behavior:
     *   - If the build is a demo run (buildNumber is DEMO_BUILD_NUMBER or SKIP_BUILD_TO_REFRESH_UI is true),
     *     the build is interrupted.
     *   - Each key in requiredKeys is checked for a non-empty value (after trimming).
     *   - If a parameter is missing or empty, an error is thrown.
     *   - Otherwise, the parameter values are echoed.
     */
    def validateBuildParameters(Map params, List requiredKeys) {
        requiredKeys.each { key ->
            if (!params[key]?.trim()) {
                steps.error "The ${key} parameter is missing or empty. Please provide a valid value."
            }
        }
        requiredKeys.each { key ->
            steps.echo "${key}: ${params[key]}"
        }
    }

    /**
     * Selects a configuration value based on the given parameters.
     *
     * @param selection The selected configuration value (or "ALL" for default).
     * @param normalList The list of valid values for non-nightly builds.
     * @param nightlyList The list of valid values for nightly builds.
     * @param isNightly Whether the build is in nightly mode.
     * @param isTimerTriggered Whether the build was triggered by a timer.
     * @param configName A name for the configuration (for error messages).
     *
     * @return A list containing the selected configuration(s).
     */
    def selectConfig(selection, normalList, nightlyList, boolean isNightly, boolean isTimerTriggered, configName) {
        if (isTimerTriggered) {
            // Force NIGHTLY if build is timer-triggered
            env.NIGHTLY = 'true'
            steps.echo "Build triggered by timer. Forcing NIGHTLY build for ${configName}."
            return nightlyList.drop(DROP_OFFSET)
        }
        if (selection == "ALL") {
            return isNightly ? nightlyList.drop(DROP_OFFSET) : normalList.drop(DROP_OFFSET)
        } else {
            if (isNightly) {
                if (nightlyList.contains(selection)) {
                    return [selection]
                } else {
                    steps.error "Selected ${configName} '${selection}' is not in Config (${nightlyList})"
                }
            } else {
                if (normalList.contains(selection)) {
                    return [selection]
                } else {
                    steps.error "Selected ${configName} '${selection}' is not in Config (${normalList})"
                }
            }
        }
    }
}
