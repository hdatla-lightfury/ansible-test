// Only run this when you change jenkins casc file.
// Do not run this if you just changed jobs
// Refer to seed job setup in jenkins-casc.yaml file 
// for setting up new jobs.
pipelineJob('Reload Jenkins Casc') {
    parameters {
        stringParam('GIT_BRANCH', 'main', 'Git branch to load Jenkinsfile from')
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
            scriptPath('pipelines/reload_jenkins_casc.groovy')
        }
    }
}
