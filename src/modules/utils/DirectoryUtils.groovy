package modules.utils

class DirectoryUtils {

    // Required env variables
    String ENGINE_DIR
    def steps

    // Constructor to initialize the env variables
    DirectoryUtils(def steps, def env) {
        this.steps = steps
        this.ENGINE_DIR = env.ENGINE_DIR
    }

    // Method to clean Unreal Engine directories
    void cleanUnrealEngine() {
        def engineBasePath = "${ENGINE_DIR}\\\\Engine" // Append \Engine only inside this method
        def engineDirs = ['Binaries', 'Intermediate', 'DerivedDataCache']

        steps.echo "Cleaning Unreal Engine build files in: ${engineBasePath}"
        engineDirs.each { dir ->
            def fullPath = "${engineBasePath}\\\\${dir}"
            steps.bat """
            if exist "${fullPath}" rd /s /q "${fullPath}"
            """
            steps.echo "Deleted: ${fullPath}"
        }
        steps.echo "Unreal Engine cleanup complete!"
    }

    // Method to clean Unreal Project directories (takes game/project name as a parameter)
    void cleanGameProject(String gameName) {
        def projectBasePath = "${ENGINE_DIR}\\\\${gameName}" // Use game name in path
        def projectDirs = ['Binaries', 'Intermediate', 'Saved', 'DerivedDataCache']

        steps.echo "Cleaning Unreal Project build files in: ${projectBasePath}"
        projectDirs.each { dir ->
            def fullPath = "${projectBasePath}\\\\${dir}"
            steps.bat """
            if exist "${fullPath}" rd /s /q "${fullPath}"
            """
            steps.echo "Deleted: ${fullPath}"
        }
        steps.echo "Unreal Project cleanup complete!"
    }

    //Method to clean any directory
    void cleanFolder(String folderPath) {
        steps.bat """
        if exist "${folderPath}" rd /s /q "${folderPath}"
        """
        steps.echo "Deleted: ${folderPath}"
        steps.echo "Unreal Project cleanup complete!"
    }

}