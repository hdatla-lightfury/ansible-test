pipelineJob('BuildPublish-Android-Firebase') {
    triggers {
        // Runs midnight 1:30 AM on sunday-friday
        cron('H 19 * * 0-5')
        p4Trigger {
          // allows perforce or any system to trigger a build with jenkins token and a jenkins Crumb
        }
    }
    definition {
        cps {
            script('''
                @Library("dsl_scripts") _
                import vars.BuildConstantsAndroid
                import modules.jenkins.JenkinsOperations
                def BUILD_CONFIG_VALUES = BuildConstantsAndroid.BUILD_CONFIG_VALUES
                def BUILD_CONFIG_VALUES_NIGHTLY = BuildConstantsAndroid.BUILD_CONFIG_VALUES_NIGHTLY
                def BUILD_TARGET_VALUES = BuildConstantsAndroid.BUILD_TARGET_VALUES
                def BUILD_TARGET_VALUES_NIGHTLY = BuildConstantsAndroid.BUILD_TARGET_VALUES_NIGHTLY
                def STREAMS = BuildConstantsAndroid.STREAMS
                def STREAMS_NIGHTLY = BuildConstantsAndroid.STREAMS_NIGHTLY
                def jenkinsOps = new JenkinsOperations(this, env)
                def nodes = jenkinsOps.getNodesForPipeline(env.JOB_NAME)
                properties([
                    parameters([
                        booleanParam(
                            name: 'NIGHTLY',
                            defaultValue: false,
                            description: 'Select this flag for Nightly builds'
                        ),
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
                            referencedParameters: 'NIGHTLY',
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
                                    if (NIGHTLY.equals("on")) {
                                        return ${STREAMS_NIGHTLY.inspect()}
                                    } else {
                                        return ${STREAMS.inspect()}
                                    }
                                    """
                                ]
                            ]
                        ],
                        [$class: 'CascadeChoiceParameter',
                            choiceType: 'PT_SINGLE_SELECT',
                            name: 'BUILD_TARGET',
                            referencedParameters: 'NIGHTLY',
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
                                    if (NIGHTLY.equals("on")) {
                                        return ${BUILD_TARGET_VALUES_NIGHTLY.inspect()}
                                    } else {
                                        return ${BUILD_TARGET_VALUES.inspect()}
                                    }
                                    """
                                ]
                            ]
                        ],
                        [$class: 'CascadeChoiceParameter',
                            choiceType: 'PT_SINGLE_SELECT',
                            name: 'BUILD_CONFIG',
                            referencedParameters: 'NIGHTLY',
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
                                    if (NIGHTLY.equals("on")) {
                                        return ${BUILD_CONFIG_VALUES_NIGHTLY.inspect()}
                                    } else {
                                        return ${BUILD_CONFIG_VALUES.inspect()}
                                    }
                                    """
                                ]
                            ]
                        ],
                        booleanParam(
                            name: 'INITIAL_SETUP',
                            defaultValue: false,
                            description: 'Set this flag if you are setting a workspace in a particular node for the first time'
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
                        stage('Skip first build and Check for required params') {
                            steps {
                                script {
                                    jenkinsOps.checkAndSkipBuild(currentBuild,params)
                                    // Validate that required parameters are provided.
                                    jenkinsOps.validateBuildParameters(params, ['P4_STREAM', 'NODE'])
                                    boolean isTimerTriggered = !currentBuild.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause').isEmpty()
                                    def isNightly = params.NIGHTLY.toBoolean() || isTimerTriggered
                                    def filteredParams = jenkinsOps.filterParamList(params, ['NIGHTLY'])                             
                                    def paramList = filteredParams.collect { key, value ->
                                        "${key}=${value}"
                                    }
                                    paramList << "NIGHTLY=${isNightly}"
                                    def paramsString = paramList.join('\\n')                                   
                                    buildDescription paramsString
                                }
                            }
                        }

                        stage("Build and Publish For Android") {
                            steps {
                                script {
                                    // Helper function to handle selection logic and timer override
                                    // Determine if NIGHTLY is active via parameter and check for timer trigger
                                    def isNightly = params.NIGHTLY.toBoolean()
                                    boolean isTimerTriggered = !currentBuild.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause').isEmpty()

                                    // selection for Build Configs (with defaults applied via .drop(1))
                                    def runBuildConfigSelection = params.BUILD_CONFIG.trim()
                                    def selectedBuildConfigs = jenkinsOps.selectConfig(
                                        runBuildConfigSelection,
                                        BUILD_CONFIG_VALUES,
                                        BUILD_CONFIG_VALUES_NIGHTLY,
                                        isNightly,
                                        isTimerTriggered,
                                        "BuildConfig"
                                    )
                                    echo "Selected Build Configs: ${selectedBuildConfigs}"

                                    //selection for Build Targets
                                    def runBuildTargetSelection = params.BUILD_TARGET.trim()
                                    def selectedBuildTargets = jenkinsOps.selectConfig(
                                        runBuildTargetSelection, BUILD_TARGET_VALUES, BUILD_TARGET_VALUES_NIGHTLY, isNightly, isTimerTriggered, "BuildTarget")
                                    echo "Selected Build Targets: ${selectedBuildTargets}"

                                    // selection for Streams
                                    def runSelectedStream = params.P4_STREAM.trim()
                                    def selectedStreams = jenkinsOps.selectConfig(runSelectedStream, STREAMS, STREAMS_NIGHTLY, isNightly, isTimerTriggered, "Stream")
                                    echo "Selected Streams: ${selectedStreams}"
                                    def buildType = isNightly || isTimerTriggered ? "Nightly" : ""
                                    // Build a parallel map for each combination of stream, build config, and build target.
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
                                                                         buildType,
                                                                         "android"
                                                                      )
                                                    }
                                                    echo "Final node label: ${node_label}"

                                                    jenkinsOps.findLockAndRunOnFreeNode(node_label) {
                                                        echo "Triggering Build Android for configuration: ${buildConfig} and target: ${buildTarget}"
                                                        build job: '/build-jobs/Build-Android', parameters: [
                                                            string(name: 'P4_CHANGELIST', value: params.P4_CHANGELIST),
                                                            string(name: 'P4_STREAM', value: stream),
                                                            string(name: 'NODE_NAME', value: env.NODE_NAME),
                                                            string(name: 'BUILD_CONFIG', value: "${buildConfig}"),
                                                            string(name: 'BUILD_TYPE', value: "${buildType}"),
                                                            string(name: 'BUILD_TARGET', value: "${buildTarget}"),
                                                            booleanParam(name: 'INITIAL_SETUP', value: params.INITIAL_SETUP)
                                                        ], wait: true

                                                        build job: '/publish-jobs/Publish-Firebase-Android', parameters: [
                                                            string(name: 'P4_STREAM', value: stream),
                                                            string(name: 'NODE_NAME', value: env.NODE_NAME)
                                                        ], wait: true
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
            ''')
        }
    }
}
