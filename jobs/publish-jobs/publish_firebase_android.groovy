folder('publish-jobs')
pipelineJob('/publish-jobs/Publish-Firebase-Android') {
    description('Publishes Android Build to Firebase')
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
            scriptPath('pipelines/publish-pipelines/publish_firebase_android.groovy')
        }
    }
}