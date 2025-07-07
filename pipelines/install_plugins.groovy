pipeline {
    agent {
        label 'built-in'
    }
    parameters {
        stringParam('GIT_BRANCH', 'main', 'Git branch to load DSL scripts from')
    }
    environment {
        GIT_REPO = 'https://github.com/LightFuryGames/lfg-titan-jenkins.git'
        PLUGIN_FILE_PATH = '${env.WORKSPACE}/images/docker/jenkins/master/linux-runtime/plugin-list.txt'
        GIT_CREDENTIAL_ID = 'github-creds'
    }
    stages {
        stage('Checkout Repo from GitHub') {
            steps {
                withCredentials([usernamePassword(credentialsId: env.GIT_CREDENTIAL_ID, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    git url: "https://${GIT_USER}:${GIT_TOKEN}@github.com/LightFuryGames/lfg-titan-jenkins.git", branch: "${params.GIT_BRANCH}"
                }
            }
        }

        stage('Copy Plugin file') {
            steps {
                script {
                    echo "Copying Plugin File"
                    sh """
                        cp "${env.PLUGIN_FILE_PATH}" "./plugin-list.txt"
                    """
                }
            }
        }

        stage('Install Plugins') {
            steps {
                script {
                    def pluginListFile = 'plugin-list.txt'
                    echo "Reading plugin list from ${pluginListFile}"
                    def pluginList = readFile(pluginListFile)
                    def plugins = pluginList.readLines().findAll { line ->
                        line.trim() && !line.trim().startsWith('#')
                    }
                    echo "Found plugins: ${plugins}"

                    def instance = Jenkins.instance
                    def pm = instance.pluginManager
                    def uc = instance.updateCenter

                    for(int i = 0 ; i < plugins.size() ; i++){
                        def parts = plugins[i].split(':')
                        def pluginId = parts[0].trim()
                        def version = parts.size() > 1 ? parts[1].trim() : null

                        if (pm.getPlugin(pluginId)) {
                            echo "Plugin ${pluginId} is already installed."
                        } else {
                            echo "Installing plugin ${pluginId}${version ? ' version ' + version : ''}..."
                            def pluginDeployment = uc.getPlugin(pluginId)
                            if (pluginDeployment != null) {
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
