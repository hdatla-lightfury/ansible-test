library(
  identifier: "dsl_scripts@${params.SHARED_LIB_BRANCH ?: 'main'}",
  retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/LightFuryGames/lfg-titan-jenkins.git',
    credentialsId: 'github-creds'
  ])
)

import modules.jenkins.JenkinsOperations
import modules.aws.AwsOperations
import modules.firebase.FirebaseOperations
import modules.slack.SlackNotifier
import modules.utils.DirectoryUtils
def capturedBuildUrl = ''
pipeline {
    parameters {
        // Required Parameters
        string(name: 'P4_STREAM', defaultValue: '', description: 'perforce Stream to build')
        string(name: 'NODE_NAME', defaultValue: '', description: 'Node name to run on')
    }
    agent { label "${params.NODE_NAME}" }
    stages{

        stage('Skip build to refresh UI and Check for required params') {
            steps {
                script{
                    def jenkinsOps = new JenkinsOperations(this, env)
                    jenkinsOps.checkAndSkipBuild(currentBuild,params)
                    jenkinsOps.validateBuildParameters(params, ['P4_STREAM', 'NODE_NAME'])
                    def paramList = params.collect { key, value -> "${key}=${value}" }
                    def paramsString = paramList.join("\\n")
                    buildDescription paramsString
                }
            }
        }

        stage('Set env variables') {
            steps {
                script {
                    def jenkinsOps = new JenkinsOperations(this,env)
                    def envVars = jenkinsOps.retrieveEnvVarsFromFile("C:\\\\Jenkins\\\\env_vars.txt")
                    env.TOP_LEVEL_DIR = envVars['TOP_LEVEL_DIR']
                    env.SUB_PATH = envVars['SUB_PATH']
                    env.ENGINE_DIR = envVars['ENGINE_DIR']
                    env.RELEASE_NOTES = envVars['RELEASE_NOTES']
                    env.SUB_BUILD_FOLDER_PATH = envVars['SUB_BUILD_FOLDER_PATH']
                    env.BUILD_CONFIG = envVars['BUILD_CONFIG']
                    capturedBuildUrl = env.BUILD_URL
                }
            }
        }

        stage('Upload Debug Symbols to AWS S3') {
            steps {
                script {
                    if(env.BUILD_CONFIG == "Test"  || env.BUILD_CONFIG == "Shipping"){
                        def folderToUpload = env.SUB_BUILD_FOLDER_PATH + "\\\\android"
                        def AwsOps = new AwsOperations(this, env)
                        AwsOps.uploadToS3("titan-debugsymbols", folderToUpload)
                        def dirUtils = new DirectoryUtils(this, env)
                        String path = "${ENGINE_DIR}\\\\LocalBuilds\\\\${folderToUpload}"
                        dirUtils.cleanFolder(path)
                    }
                }
            }
        }


        stage('Upload to firebase app distribution') {
            steps {
                script {
                    def firebaseOps = new FirebaseOperations(this, env)
                    firebaseOps.uploadApkToFirebase()
                }
            }
        }

        stage('Delete Env Vars File') {
            steps {
                script {
                    def jenkinsOps = new JenkinsOperations(this,env)
                    jenkinsOps.deleteEnvVarsFile()
                }
            }
        }
    }
    post {
        failure {
            script {
                def notifier = new SlackNotifier(this, env)
                notifier.notifyStatus(
                    "failure",
                    notifier.craftMessageForStatus(
                        "failure",
                        "devops",
                        env.JOB_NAME,
                        env.BUILD_NUMBER,
                        "",
                        capturedBuildUrl,
                        false,
                        false
                    )
                )
            }
        }
        aborted {
            script {
                def notifier = new SlackNotifier(this, env)
                notifier.notifyStatus(
                    "aborted",
                    notifier.craftMessageForStatus(
                        "aborted",
                        commiterSlackId,
                        env.JOB_NAME,
                        env.BUILD_NUMBER,
                        env.P4_CHANGELIST,
                        capturedBuildUrl,
                        false,
                        false
                    )
                )
            }
        }
    }
}
