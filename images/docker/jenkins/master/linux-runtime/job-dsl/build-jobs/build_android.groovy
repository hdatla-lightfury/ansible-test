folder('build-jobs')
pipelineJob('/build-jobs/Build-Android') {
    description('Builds Android')
    definition {
        cps {
            script('''
@Library('dsl_scripts') _

def capturedBuildUrl = ''
def commiterSlackId = ''
boolean isNightly = false
boolean isInitialSetup = false

import modules.perforce.PerforceOperations
import modules.unreal.UnrealOperations
import modules.android.AndroidOperations
import modules.jenkins.JenkinsOperations
import modules.utils.DirectoryUtils
import modules.slack.SlackNotifier
import modules.sentry.SentryOperations

pipeline {
    parameters {
        // Required Parameters
        string(name: 'P4_CHANGELIST', defaultValue: '', description: 'perforce CL to build')
        string(name: 'P4_STREAM', defaultValue: '', description: 'perforce Stream to build')
        string(name: 'NODE_NAME', defaultValue: '', description: 'Node name to run on')
        string(name: 'BUILD_CONFIG', defaultValue: '', description: 'Build Config(Test, Shipping etc)')
        string(name: 'BUILD_TYPE', defaultValue: '', description: 'Build Type(Nightly or empty) To Run')
        string(name: 'BUILD_TARGET', defaultValue: '', description: 'Target to build for')
        booleanParam(
            name: 'INITIAL_SETUP',
            defaultValue: false,
            description: 'Set this flag if you are setting a workspace in a particular node for the first time'
        )
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

        stage('Install Android SDK dependencies') {
            steps {
                script {
                    def androidOps = new AndroidOperations(this, env)
                    androidOps.installSdk()
                }
            }
        }

        stage('Set Global Env Variables') {
            steps{
                script{
                    env.STREAM_NAME = params.P4_STREAM.tokenize('/').last()
                    env.P4_WORKSPACE_DIR = "${env.ROOT_DIR}\\\\${env.STREAM_NAME}"
                    env.ENGINE_DIR = "${env.P4_WORKSPACE_DIR}\\\\titan-engine"
                    env.PLATFORM = "Android"
                    env.USE_OLD_VERSION_NAME_METHOD = "False".toBoolean()
                    capturedBuildUrl = env.BUILD_URL
                    isNightly = (params.BUILD_TYPE == "Nightly") ? true : false
                    isInitialSetup = params.INITIAL_SETUP
                }
            }
        }

        stage('Perforce Trust and Login') {
            steps {
                script {
                    def perforceOps = new PerforceOperations(this, env)
                    perforceOps.trust()
                    perforceOps.login()
                }
            }
        }

        stage('Setup Titan Workspace') {
            steps {
                script {
                    def perforceOps = new PerforceOperations(this, env)
                    perforceOps.setupWorkspace()
                }
            }
        }

        stage('Sync Titan Game to Latest Revision') {
            steps {
                script {
                    def perforceOps = new PerforceOperations(this, env)
					def dirUtils = new DirectoryUtils(this, env)

					if (isNightly) {
						dirUtils.cleanGameProject("Titan")
						env.P4_CHANGELIST = perforceOps.syncWorkspace()
					} else {
						env.P4_CHANGELIST = perforceOps.syncWorkspace(isInitialSetup ?: false)
					}
                    env.RELEASE_NOTES = perforceOps.getCommitMessage(env.P4_CHANGELIST)
                    env.COMMITER_USER_EMAIL = perforceOps.getEmailIdFromChangeList(env.P4_CHANGELIST)
                    commiterSlackId = env.COMMITER_USER_EMAIL.isEmpty() ? "" : slackUserIdFromEmail(env.COMMITER_USER_EMAIL)
                    if(isNightly || isInitialSetup){
                        commiterSlackId = "devops"
                    }
                }
            }
        }

        stage('Unreal Setup') {
            steps {
                script {
                    def unrealOps = new UnrealOperations(this, env)
                    if (params.INITIAL_SETUP){
                        unrealOps.setup()
                        unrealOps.generateProjectFiles()
                    }
                }
            }
        }

        stage('Build Unreal Editor For Android') {
            steps {
                script {
                    def unrealOps = new UnrealOperations(this, env)
                    unrealOps.buildEditor()
                }
            }
        }

        stage('Build Game For Android') {
            steps {
                script {
                    def androidOps = new AndroidOperations(this, env)
                    androidOps.buildUnrealGameForAndroid(params.BUILD_CONFIG)
                }
            }
        }

        stage('Build Cook Run and Package Titan Android') {
            steps {
                script {
                    def androidOps = new AndroidOperations(this, env)
                    androidOps.installSdk()
                    if (params.BUILD_TYPE?.toLowerCase() == 'nightly' || params.INITIAL_SETUP) {
                        androidOps.cleanBuildCookRunAndroidASTC(params.BUILD_TARGET, params.BUILD_CONFIG)
                    } else {
                        androidOps.iterativeBuildCookRunAndroidASTC(params.BUILD_TARGET, params.BUILD_CONFIG)
                    }
                }
            }
        }
        stage('Prepare Debug Symbols') {
            steps {
                script {
                    if(params.BUILD_CONFIG == "Test"  || params.BUILD_CONFIG == "Shipping"){
                        def sentryOps = new SentryOperations(this, env)
                        sentryOps.prepareSentrySymbols(env.PLATFORM, params.BUILD_CONFIG, params.BUILD_TARGET, env.STREAM_NAME, env.P4_CHANGELIST, params.BUILD_TYPE, "android")
                    }
                }
            }
        }

        stage('Store Env Variables') {
            steps {
                script {
                    def jenkinsOps = new JenkinsOperations(this, env)
                    def baseBuildDir = "${ENGINE_DIR}\\\\LocalBuilds"
                    def platform = "Android"
                    def sub_folder = "CL-${env.P4_CHANGELIST}-${env.STREAM_NAME}-${env.BUILD_TARGET}-${env.BUILD_CONFIG}-${platform}"
                    if (params.BUILD_TYPE == "Nightly" ){
                        sub_folder+="-Nightly"
                    }
                    def apk_name = "\\\\${env.BUILD_TARGET}-${platform}-${env.BUILD_CONFIG}-arm64.apk"
                    /* Adapting to unreal apk naming
                     refer here:
                     https://github.com/EpicGames/UnrealEngine/blob/2d53fcab0066b1f16dd956b227720841cad0f6f7/Engine/Source/Programs/AutomationTool/Scripts/CopyBuildToStagingDirectory.Automation.cs#L5576
                     */
                    if (params.BUILD_CONFIG == "Development"){
                        apk_name = "\\\\${env.BUILD_TARGET}-arm64.apk"
                    }
                    def envVars = [
                        'STREAM_NAME'                 : env.STREAM_NAME,
                        'ENGINE_DIR'                  : env.ENGINE_DIR,
                        'P4_CHANGELIST'               : env.P4_CHANGELIST,
                        'BUILD_TARGET'                : env.BUILD_TARGET,
                        'BUILD_CONFIG'                : env.BUILD_CONFIG,
                        'BUILD_TYPE'                  : env.BUILD_TYPE,
                        'RELEASE_NOTES'               : env.RELEASE_NOTES,
                        'TOP_LEVEL_DIR'               : "${baseBuildDir}",
                        'SUB_PATH'                    : "${platform}_${env.BUILD_TARGET}" + "\\\\${sub_folder}" + "${apk_name}",
                        'SUB_BUILD_FOLDER_PATH'       : "${platform}_${env.BUILD_TARGET}" + "\\\\${sub_folder}"
                    ]
                    jenkinsOps.storeEnvVarsToFile(envVars, "C:\\\\Jenkins\\\\env_vars.txt")
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
                        commiterSlackId,
                        env.JOB_NAME,
                        env.BUILD_NUMBER,
                        env.P4_CHANGELIST,
                        capturedBuildUrl,
                        isNightly,
                        isInitialSetup
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
                        isNightly,
                        isInitialSetup
                    )
                )
            }
        }
        success {
            script {
                def notifier = new SlackNotifier(this, env)
                notifier.notifyStatus(
                    "success",
                    notifier.craftMessageForStatus(
                        "success",
                        commiterSlackId,
                        env.JOB_NAME,
                        env.BUILD_NUMBER,
                        env.P4_CHANGELIST,
                        capturedBuildUrl,
                        isNightly,
                        isInitialSetup
                    )
                )
            }
        }
    }
}
            ''')
        }
    }
}