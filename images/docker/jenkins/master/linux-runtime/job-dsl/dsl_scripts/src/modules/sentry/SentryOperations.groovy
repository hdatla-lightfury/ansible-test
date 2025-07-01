package modules.sentry

class SentryOperations {
    // Class properties for env variables
    String ENGINE_DIR
    def steps
    def env

    // Constructor to initialize the env variables 
    SentryOperations(def steps, def env) {
        this.env = env
        this.ENGINE_DIR = env.ENGINE_DIR
        this.steps = steps
    }

    void prepareSentrySymbols(String platform, String buildConfig, String buildTarget, String streamName, String changelist, String buildType = '', String prefix) {
        def sub_folder = "\\\\LocalBuilds\\\\${platform}_${buildTarget}\\\\CL-${changelist}-${streamName}-${buildTarget}-${buildConfig}-${platform}"
        def bundle_id = "${changelist}-${streamName}-${buildTarget}-${buildConfig}-${platform}"
        if (buildType == "Nightly") {
            sub_folder += "-Nightly"
            bundle_id += "-Nightly"
        }
        def input_folder = this.ENGINE_DIR + sub_folder
        def output_folder = input_folder
        steps.bat """
            python ${ENGINE_DIR}\\\\.lightfury\\\\ci\\\\scripts\\\\jobs\\\\sentry_handler.py --input-folder ${input_folder} --output-folder ${output_folder} --prefix ${prefix} --bundle-id ${bundle_id}
        """
    }
}
