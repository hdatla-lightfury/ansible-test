package modules.ugs

class UGSOperations {

    // Class property for storing environment variable
    String ENGINE_DIR
    def steps

    // Constructor to initialize the environment variable
    UGSOperations(def steps, def env) {
        this.steps = steps
        this.ENGINE_DIR = env.ENGINE_DIR
    }

    // Method for the build and package operation for UGS
    void buildAndPackageForUgs() {
        steps.bat """
        python ${ENGINE_DIR}\\\\.lightfury\\\\ci\\\\scripts\\\\jobs\\\\build_and_package_for_ugs.py
        """
    }
}
