folder('build-jobs')
pipelineJob('/build-jobs/Build-Windows') {
    description('Builds Windows')
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
            scriptPath('pipelines/build-pipelines/build_windows.groovy')
        }
    }
}