library(
  identifier: "dsl_scripts@${params.SHARED_LIB_BRANCH ?: 'main'}",
  retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/LightFuryGames/lfg-titan-jenkins.git',
    credentialsId: 'github-creds'
  ])
)

def capturedBuildUrl = ''
def commiterSlackId = ''
boolean isNightly = false
boolean isInitialSetup = false

import modules.perforce.PerforceOperations
import modules.unreal.UnrealOperations
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

		stage('Set Global Env Variables') {
			steps{
				script{
                    env.STREAM_NAME = params.P4_STREAM.tokenize('/').last()
                    env.P4_WORKSPACE_DIR = "${env.ROOT_DIR}\\\\${env.STREAM_NAME}"
                    env.ENGINE_DIR = "${env.P4_WORKSPACE_DIR}\\\\titan-engine"
                    env.PLATFORM = "Win64"
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

        stage('Build Unreal Editor') {
            steps {
                script {
                    def unrealOps = new UnrealOperations(this, env)
                    unrealOps.buildEditor()
                }
            }
        }

		stage('Build Game For Windows') {
			steps {
				script {
					def buildPlatform = "Win64"
					def unrealOps = new UnrealOperations(this, env)
					unrealOps.buildGame(buildPlatform, params.BUILD_CONFIG)
				}
			}
		}

		stage('Build Cook Run and Package Titan Windows') {
			steps {
				script {
					def buildPlatform = "Win64"
					def baseFlags = "-FastCook -notools -nop4 -utf8output -nocompileeditor -skipbuildeditor -unattended -utf8output -build -cook -stage -pak -prereqs -package -archive -iostore -compressed -nocompile -nocompileuat"
					def additionalFlags = params.BUILD_TYPE?.toLowerCase() == 'nightly' || params.INITIAL_SETUP
						? "-clean ${baseFlags}"
						: "-iterativecooking ${baseFlags}"

					def unrealOps = new UnrealOperations(this, env)
					unrealOps.buildCookRun(
						platform = buildPlatform,
						target = params.BUILD_TARGET,
						configuration = params.BUILD_CONFIG,
						cookFlavor = "ASTC",
						additionalFlags = additionalFlags
					)
				}
			}
		}

        stage('Prepare Debug Symbols') {
            steps {
                script {
                    if(params.BUILD_CONFIG == "Test"  || params.BUILD_CONFIG == "Shipping"){
                        def sentryOps = new SentryOperations(this, env)
                        sentryOps.prepareSentrySymbols(env.PLATFORM, params.BUILD_CONFIG, params.BUILD_TARGET, env.STREAM_NAME, env.P4_CHANGELIST, params.BUILD_TYPE, "windows")
                    }
                }
            }
        }

        stage('Store Env Variables') {
            steps {
                script {
                    def jenkinsOps = new JenkinsOperations(this, env)
                    def baseBuildDir = "${ENGINE_DIR}\\\\LocalBuilds"
                    def platform = "Win64"
                    def sub_folder = "CL-${env.P4_CHANGELIST}-${env.STREAM_NAME}-${env.BUILD_TARGET}-${env.BUILD_CONFIG}-${platform}"
                    if (params.BUILD_TYPE == "Nightly" ){
                        sub_folder+="-Nightly"
                    }
                    def envVars = [
                        'STREAM_NAME'                 : env.STREAM_NAME,
                        'ENGINE_DIR'                  : env.ENGINE_DIR,
                        'P4_CHANGELIST'               : env.P4_CHANGELIST,
                        'BUILD_TARGET'                : env.BUILD_TARGET,
                        'BUILD_CONFIG'                : env.BUILD_CONFIG,
                        'BUILD_TYPE'                  : env.BUILD_TYPE,
                        'TOP_LEVEL_DIR'               : "${baseBuildDir}",
                        'SUB_FOLDER_NAME'             : "${platform}_${env.BUILD_TARGET}",
                        'SUB_BUILD_FOLDER_PATH'           : "${platform}_${env.BUILD_TARGET}" + "\\\\${sub_folder}"
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
