pipelineJob('Install Plugins') {
    definition {
        cps {
            script('''
                pipeline {
                    agent {
                        label 'built-in'
                    }
                    stages {
                        stage('Checkout from Perforce') {
                            steps {
                                p4sync(
                                    charset: 'none',
                                    credential: 'perforce-credentials-build-lightfury',

                                    populate: autoClean(
                                                delete: true,
                                                modtime: false,
                                                force: true,
                                                parallel: [enable: false, minbytes: '1024', minfiles: '1', threads: '4'],
                                                pin: '',
                                                quiet: true,
                                                replace: true,
                                                tidy: false
                                               ),

                                    workspace: manualSpec(
                                                    charset: 'none',
                                                    name: 'build-lightfury_lfg_titan_ci-jenkins-master',
                                                    spec: clientSpec(
                                                        allwrite: false,
                                                        clobber: false,
                                                        compress: false,
                                                        line: 'LOCAL',
                                                        locked: false,
                                                        modtime: false,
                                                        rmdir: false,
                                                        streamName: '',
                                                        view: """//titan-ci/mainline/src/docker/docker_stages/jenkins_master/linux/plugins-list.txt //build-lightfury_lfg_titan_ci-jenkins-master/..."""
                                                 )
                                    )
                                )
                            }
                        }
                        stage('Copy Plugin file') {
                            steps {
                                script {
                                    echo "Copying Plugin File"
                                    sh """
                                        sudo cp -r "/var/jenkins_home/workspace/Install Plugins/plugins-list.txt" "."
                                    """
                                }
                            }
                        }

                        stage('Install Plugins') {
                            steps {
                                script {
                                    // Its impossible to uninstall plugins and its configurations
                                    // So uninstalling plugin has to be done manually.
                                    // Read the plugin list file from the workspace.
                                    def pluginListFile = 'plugin-list.txt'
                                    echo "Reading plugin list from ${pluginListFile}"
                                    def pluginList = readFile(pluginListFile)
                                    def plugins = pluginList.readLines().findAll { line ->
                                        line.trim() && !line.trim().startsWith('#')
                                    }
                                    echo "Found plugins: ${plugins}"

                                    // Get references to Jenkins' instance, plugin manager, and update center.
                                    def instance = Jenkins.instance
                                    def pm = instance.pluginManager
                                    def uc = instance.updateCenter

                                    // Loop over each plugin entry and install if not already present.
                                    for(int i = 0 ; i < plugins.size() ; i++){
                                        // Expected entry format: pluginId or pluginId:version
                                        def parts = plugins[i].split(':')
                                        def pluginId = parts[0].trim()
                                        def version = parts.size() > 1 ? parts[1].trim() : null

                                        if (pm.getPlugin(pluginId)) {
                                            echo "Plugin ${pluginId} is already installed."
                                        } else {
                                            echo "Installing plugin ${pluginId}${version ? ' version ' + version : ''}..."
                                            def pluginDeployment = uc.getPlugin(pluginId)
                                            if (pluginDeployment != null) {
                                                // Deploy the plugin; the 'true' argument tells Jenkins to restart if needed.
                                                pluginDeployment.deploy()
                                                echo "Installation for plugin ${pluginId} initiated."
                                            } else {
                                                echo "Plugin ${pluginId} not found in the update center!"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
           ''')
        }
    }
 }
