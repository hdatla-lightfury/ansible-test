pipelineJob('Job_DSL_Seed') {
    description('Seed Job to setup and refresh jenkins jobs')
    definition {
        cps {
            script('''
pipeline {
    agent {
        label 'built-in'
    }
    options {
        // This is required if you want to clean before build
        skipDefaultCheckout(true)
    }
    stages {
        stage('Clean Workspace'){
            steps{
                cleanWs()
            }
        }
        stage('Create Folders') {
            steps {
                // Create folders using Job DSL to ensure they exist every run
                jobDsl(
                    scriptText: """
                        folder('build-jobs') {
                            displayName('Build Jobs')
                            description('This folder contains all build-related build jobs.')
                        }
                        folder('publish-jobs') {
                            displayName('Publish Jobs')
                            description('This folder contains all publish-related build jobs.')
                        }
                    """,
                    sandbox: false
                )
            }
        }
        stage('Copy Job Files') {
            steps {
                // Run the file-copying code every time using a system Groovy command.
                jobDsl(
                    scriptText:"""
                    import java.nio.file.Files
                    import java.nio.file.Paths
                    import java.nio.file.StandardCopyOption
                    def jobsSourceDir = new File('/var/jenkins_home/job-dsl')
                    def jobsTargetDir = new File('/var/jenkins_home/workspace/Job_DSL_Seed/jobs')
                    if (!jobsTargetDir.exists()) {
                        jobsTargetDir.mkdirs()
                        println "Created build-jobs target directory: " + jobsTargetDir.absolutePath
                    }
                    jobsSourceDir.eachFile { file ->
                        if (file.isFile()) {
                            try {
                                def sourcePath = file.toPath()
                                def targetPath = Paths.get(jobsTargetDir.absolutePath, file.name)
                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                                println "Copied build job file: " + file.name
                            } catch (Exception e) {
                                println "Failed to copy build job file: " + file.name + ". Error: " + e.message
                            }
                        }
                    }
                    // Copy build job files
                    def buildSourceDir = new File('/var/jenkins_home/job-dsl/build-jobs')
                    def buildTargetDir = new File('/var/jenkins_home/workspace/Job_DSL_Seed/jobs/build-jobs')
                    if (!buildTargetDir.exists()) {
                        buildTargetDir.mkdirs()
                        println "Created build-jobs target directory: " + buildTargetDir.absolutePath
                    }
                    buildSourceDir.eachFile { file ->
                        if (file.isFile()) {
                            try {
                                def sourcePath = file.toPath()
                                def targetPath = Paths.get(buildTargetDir.absolutePath, file.name)
                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                                println "Copied build job file: " + file.name
                            } catch (Exception e) {
                                println "Failed to copy build job file: " + file.name + ". Error: " + e.message
                            }
                        }
                    }
                    // Copy publish job files
                    def publishSourceDir = new File('/var/jenkins_home/job-dsl/publish-jobs')
                    def publishTargetDir = new File('/var/jenkins_home/workspace/Job_DSL_Seed/jobs/publish-jobs')
                    if (!publishTargetDir.exists()) {
                        publishTargetDir.mkdirs()
                        println "Created publish-jobs target directory: " + publishTargetDir.absolutePath
                    }
                    publishSourceDir.eachFile { file ->
                        if (file.isFile()) {
                            try {
                                def sourcePath = file.toPath()
                                def targetPath = Paths.get(publishTargetDir.absolutePath, file.name)
                                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                                println "Copied publish job file: " + file.name
                            } catch (Exception e) {
                                println "Failed to copy publish job file: " + file.name + ". Error: " + e.message
                            }
                        }
                    }
                    def scriptPath = "/var/jenkins_home/init.groovy.d/auto_approve_scripts.groovy"
                    // Read the content of the external Groovy script
                    def scriptContent = new File(scriptPath).text
                    // Evaluate the script
                    evaluate(scriptContent)
                    """,
                    sandbox: false
                )
            }
        }

        stage('Process Parent Jobs') {
            steps {
                // Process the DSL scripts for client jobs
                jobDsl targets: 'jobs/*.groovy'
            }
        }
        stage('Process Build Job DSL') {
            steps {
                // Process the DSL scripts for build jobs
                jobDsl targets: 'jobs/build-jobs/**/*.groovy'
            }
        }
        stage('Process publish Job DSL') {
            steps {
                // Process the DSL scripts for publish jobs
                jobDsl targets: 'jobs/publish-jobs/**/*.groovy'
            }
        }
    }
}
            ''')
        }
    }
 }