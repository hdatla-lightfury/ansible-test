pipelineJob('BuildPublish-WindowsEditor-UGS') {
    triggers {
        p4Trigger {
          // allows perforce or any system to trigger a build with jenkins token and a jenkins Crumb
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
            scriptPath('pipelines/build_publish_windows_editor_ugs.groovy')
        }
    }
}
