library(
  identifier: "dsl_scripts@${params.SHARED_LIB_BRANCH ?: 'main'}",
  retriever: modernSCM([
    $class: 'GitSCMSource',
    remote: 'https://github.com/LightFuryGames/lfg-titan-jenkins.git',
    credentialsId: 'github-creds'
  ])
)

import vars.BuildConstantsUgs
import modules.jenkins.JenkinsOperations
def jenkinsOps = new JenkinsOperations(this, env)
def nodes = jenkinsOps.getNodesForPipeline(env.JOB_NAME)
def BUILD_CONFIG_VALUES = BuildConstantsUgs.BUILD_CONFIG_VALUES
def STREAMS = BuildConstantsUgs.STREAMS
def BUILD_TARGET_VALUES = BuildConstantsUgs.BUILD_TARGET_VALUES
properties([
    parameters([
        choice(
            name: 'NODE',
            choices: nodes,
            description: 'The node to run on, only select and run if you know what you are doing'
        ),
        string(
            name: 'P4_CHANGELIST',
            defaultValue: '',
            description: 'Changelist associated with the stream'
        ),
        [$class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            name: 'P4_STREAM',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: """ return ${STREAMS.inspect()} """
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                        return ${STREAMS.inspect()}
                    """
                ]
            ]
        ],
        [$class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            name: 'BUILD_CONFIG',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: """ return ${BUILD_CONFIG_VALUES.inspect()} """
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                        return ${BUILD_CONFIG_VALUES.inspect()}
                    """
                ]
            ]
        ],
        [$class: 'CascadeChoiceParameter',
            choiceType: 'PT_SINGLE_SELECT',
            name: 'BUILD_TARGET',
            script: [
                $class: 'GroovyScript',
                fallbackScript: [
                    classpath: [],
                    sandbox: false,
                    script: """ return ${BUILD_TARGET_VALUES.inspect()} """
                ],
                script: [
                    classpath: [],
                    sandbox: false,
                    script: """
                        return ${BUILD_TARGET_VALUES.inspect()}
                    """
                ]
            ]
        ],
        booleanParam(
            name: 'INITIAL_SETUP',
            defaultValue: false,
            description: 'Set this flag if you are setting a workspace on a particular node for the first time'
        ),
        booleanParam(
            name: 'SKIP_BUILD_TO_REFRESH_UI',
            defaultValue: false,
            description: 'Flag to rebuild the UI, do not touch if you dont know what this means'
        )
    ])
])

def p4Stream = params.P4_STREAM?.trim()
def streamName = (p4Stream && !p4Stream.equalsIgnoreCase("ALL")) ? p4Stream.tokenize('/')?.last() : ""
pipeline {
    agent {
        label 'built-in'
    }
    stages {
        stage('Skip first build and Check required params') {
            steps {
                script {
                    jenkinsOps.checkAndSkipBuild(currentBuild,params)
                    // Validate that required parameters are provided.
                    jenkinsOps.validateBuildParameters(params, ['P4_STREAM', 'NODE'])
                    def paramList = params.collect { key, value -> "${key}=${value}" }
                    def paramsString = paramList.join("\\n")
                    buildDescription paramsString
                }
            }
        }
        stage("Build and Publish For UGS") {
            steps {
                script {
                    // Determine if the build is nightly and/or timer-triggered
                    def isNightly = false
                    def isTimerTriggered = false

                    // Use the common function to select Build Configs
                    def selectedBuildConfigs = jenkinsOps.selectConfig(
                        params.BUILD_CONFIG.trim(),
                        BUILD_CONFIG_VALUES,
                        [],
                        isNightly,
                        isTimerTriggered,
                        "BuildConfig"
                    )
                    echo "Selected Build Configs: ${selectedBuildConfigs}"

                    // Use the common function to select Build Targets
                    def selectedBuildTargets = jenkinsOps.selectConfig(
                        params.BUILD_TARGET.trim(),
                        BUILD_TARGET_VALUES,
                        [],
                        isNightly,
                        isTimerTriggered,
                        "BuildTarget"
                    )
                    echo "Selected Build Targets: ${selectedBuildTargets}"

                    // Use the common function to select Streams
                    def selectedStreams = jenkinsOps.selectConfig(
                        params.P4_STREAM.trim(),
                        STREAMS,
                        [],
                        isNightly,
                        isTimerTriggered,
                        "Stream"
                    )
                    echo "Selected Streams: ${selectedStreams}"
                    // Build a parallel map for each combination
                    def branches = [:]
                    selectedStreams.each { stream ->
                        selectedBuildConfigs.each { buildConfig ->
                            selectedBuildTargets.each { buildTarget ->
                                def branchName = "${stream}-${buildConfig}-${buildTarget}"
                                branches[branchName] = {
                                    echo "Running job for Stream: ${stream}, BuildConfig: ${buildConfig}, BuildTarget: ${buildTarget}"
                                    streamName = stream.tokenize('/')?.last()
                                    
                                    def node_label = params.NODE
                                    if (params.NODE == 'NONE') {
                                            node_label = jenkinsOps.getNodeLabel(
                                                        "windows",
                                                            streamName,
                                                            "",
                                                            "ugs"
                                                        )
                                    }
                                    echo "Final node label: ${node_label}"

                                    jenkinsOps.findLockAndRunOnFreeNode(node_label){
                                        // Trigger Build job for UGS Editor (or the appropriate UGS job)
                                        build job: '/build-jobs/Build-WindowsEditor', parameters: [
                                            string(name: 'P4_CHANGELIST', value: params.P4_CHANGELIST),
                                            string(name: 'P4_STREAM', value: stream),
                                            string(name: 'NODE_NAME', value: env.NODE_NAME),
                                            string(name: 'BUILD_CONFIG', value: "${buildConfig}"),
                                            string(name: 'BUILD_TARGET', value: "${buildTarget}"),
                                            booleanParam(name: 'INITIAL_SETUP', value: params.INITIAL_SETUP)
                                        ], wait: true

                                        // TODO: We have to split the build_ugs script to build and publish
                                    }
                                }
                            }
                        }
                    }

                    parallel branches
                }
            }
        }
    }
}
            