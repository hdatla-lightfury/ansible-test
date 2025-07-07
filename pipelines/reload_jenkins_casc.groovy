pipeline {
    agent {
        label 'built-in'
    }
    parameters {
        stringParam('GIT_BRANCH', 'main', 'Git branch to load DSL scripts from')
    }
    environment {
        GIT_REPO = 'https://github.com/LightFuryGames/lfg-titan-jenkins.git'
        JCASC_FILE_PATH = '${env.WORKSPACE}/images/docker/jenkins/master/linux-runtime/jenkins-casc.yaml'
        JCASC_TARGET_DIR = '/var/jenkins_home/'
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

        stage('Sync JCasC Config') {
            steps {
                sh """
                    echo "Copying JCasC file..."
                    sudo cp "${env.JCASC_FILE_PATH}" "${env.JCASC_TARGET_DIR}/jenkins-casc.yaml"
                """
            }
        }

        stage('Restart Jenkins') {
            steps {
                script {
                    echo "Triggering safe Jenkins restart to apply JCasC config..."
                    Jenkins.instance.safeRestart()
                }
            }
        }
    }
}
