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
boolean isInitialSetup = false

import modules.perforce.PerforceOperations
import modules.unreal.UnrealOperations
import modules.ugs.UGSOperations
import modules.jenkins.JenkinsOperations
import modules.slack.SlackNotifier

pipeline {
   parameters {
        // Required Parameters
        string(name: 'P4_CHANGELIST', defaultValue: '', description: 'perforce CL to build')
        string(name: 'P4_STREAM', defaultValue: '', description: 'perforce Stream to build')
        string(name: 'NODE_NAME', defaultValue: '', description: 'Node name to run on')
        string(name: 'BUILD_CONFIG', defaultValue: '', description: 'Node name to run on')
        string(name: 'BUILD_TARGET', defaultValue: '', description: 'Target to run for')
        booleanParam(
            name: 'INITIAL_SETUP',
            defaultValue: false,
            description: 'Set this flag if you are setting a workspace in a particular node for the first time'
        )
    }
    agent { label "${params.NODE_NAME}" }
    stages{

        stage('Skip first build to refresh UI and Check for required params') {
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
                    env.USE_OLD_VERSION_NAME_METHOD = "False".toBoolean()
                    env.P4PORT = env.P4_PORT
                    env.P4USER = env.P4_USER
                    env.P4PASS = env.P4_PASS
                    capturedBuildUrl = env.BUILD_URL
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
                    env.P4_CHANGELIST = perforceOps.syncWorkspace(params.INITIAL_SETUP)
                    env.COMMITER_USER_EMAIL = perforceOps.getEmailIdFromChangeList(env.P4_CHANGELIST)
                    commiterSlackId = env.COMMITER_USER_EMAIL.isEmpty() ? "" : slackUserIdFromEmail(env.COMMITER_USER_EMAIL)
                    if(params.INITIAL_SETUP){
                        commiterSlackId = "devops"
                    }
                }
            }
        }

        stage('Unreal Setup') {
            steps {
                script {
                    def unrealOps = new UnrealOperations(this, env)
                    if(params.INITIAL_SETUP){
                        unrealOps.setup()
                        unrealOps.generateProjectFiles()
                    }
                }
            }
        }

		stage('Run Build and Package for UGS') {
			steps {
				script {
					def unrealOps = new UnrealOperations(this, env)
					def streamWithBinaries = "${params.P4_STREAM}-binaries"
												
					unrealOps.runBuildGraph(
						"Engine/Build/Graph/Examples/BuildEditorAndTools.xml",
						"Submit To Perforce for UGS",
						[
							"set:EditorTarget": "TitanEditor",
							"set:ArchiveStream": streamWithBinaries,
							"BuildMachine"    : "",
							"p4"              : "",
							"submit"          : ""
						]
					)
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
                        false,
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
                        false,
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
                        false,
                        isInitialSetup
                    )
                )
            }
        }
    }
}
