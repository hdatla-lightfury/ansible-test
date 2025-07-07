pipelineJob('BuildPublish-Android-Firebase') {
    properties {
        pipelineTriggers {
            triggers {
                // Runs midnight 1:30 AM on sunday-friday
                cron{
                    spec('H 19 * * 0-5')
                }
            }
        }
    }
    parameters {
        stringParam('GIT_BRANCH', 'main', 'Git branch to load Jenkinsfile from')
        stringParam('SHARED_LIB_BRANCH', 'main', 'Branch of the shared library to use')
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url('https://github.com/LightFuryGames/lfg-titan-jenkins.git')
                        credentials('github-creds')
                    }
                    branch('$GIT_BRANCH')
                }
            }
            scriptPath('pipelines/build_publish_android_firebase.groovy')
        }
    }
}
