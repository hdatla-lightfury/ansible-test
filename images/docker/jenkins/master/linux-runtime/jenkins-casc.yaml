## TODO : Find a way to split this in to multiple yaml files and merge them together
## Merging casc files as reported by multiple developers is a huge pain and doesn't support deep merging, so for now we have to live with this long file
## Refer -> https://github.com/jenkinsci/configuration-as-code-plugin/issues/1303

x-baseView: &baseView
  columns:
    - "status"
    - "weather"
    - "jobName"
    - "lastSuccess"
    - "lastFailure"
    - "lastDuration"
    - "buildButton"
    - "favoriteColumn"
  includeStdJobList: true
  topPortlets:
    - latestBuilds:
        name: "Latest builds"
        numBuilds: 10

x-regExJobFilter: &baseRegExJobFilter
    includeExcludeTypeString: "includeMatched"
    matchDisplayName: false
    matchFullDisplayName: false
    matchFullName: false
    matchName: true
    valueTypeString: "NAME"

jenkins:
  systemMessage: "Jenkins configured automatically by Jenkins Configuration as Code plugin"
  numExecutors: 6 #Should ideally be the no of nodes available.
  myViewsTabBar: "standard"
  primaryView:
      listView:
        name: "all"

  views:
    - dashboard:
        <<: *baseView
        jobFilters:
        - regExJobFilter:
            <<: *baseRegExJobFilter
            regex: ".*Android.*"
        name: "1. Android"
    - dashboard:
        <<: *baseView
        jobFilters:
          - regExJobFilter:
              <<: *baseRegExJobFilter
              regex: ".*-Windows(?:-|$).*"
        name: "2. Windows"
    - dashboard:
        <<: *baseView
        jobFilters:
          - regExJobFilter:
              <<: *baseRegExJobFilter
              regex: ".*WindowsEditor.*"
        name: "3. UGS"
    - all:
        name: "all"

  viewsTabBar: "standard"

  securityRealm:
    googleOAuth2:
      clientId: "${GOOGLE_CLIENT_ID}"
      clientSecret: "${GOOGLE_CLIENT_SECRET}"

  authorizationStrategy:
    roleBased:
      roles:
        # Define roles at the global level
        # Full administrative access for backend and devops team
        global:
          - name: administration
            permissions:
              - "Overall/Administer"
            entries:
              - user: "jenkins-admin@lightfurygames.com"
          - name: "readonly" ##every user who is not given administrator access should be given this role by authsphere by default
            permissions:
                - "Overall/Read"
            entries:
                - group: "authenticated"
        # Ability to run parent pipelines for QA
        items:
          - name: titan-build-publish-jobs-run-access
            pattern: "BuildPublish.*"
            permissions: &build_run_permissions
              - "Job/Read"
              - "Job/Build"
              - "Job/Cancel"
              - "Job/Discover"
              - "Run/Replay"
              - "Run/Update"
              - "View/Read"
              - "Overall/Read"
          # Ability to run parent jobs and view logs of child jobs
          - name: titan-all-jobs-run-access
            pattern: "^(?!(?:Reload Jenkins Casc$|Job_DSL_Seed$|Install Plugins)$).*$"
            permissions: *build_run_permissions
          # Ability to view status,logs of all jobs
          - name: titan-all-jobs-view-access
            pattern: "^(?!(?:Reload Jenkins Casc$|Job_DSL_Seed$|Install Plugins)$).*$"
            permissions: &view_permissions
              - "Job/Read"
              - "View/Read"
              - "Overall/Read"
          # Ability to view status,logs of parent pipelines
          - name: titan-build-publish-jobs-view-access
            pattern: "BuildPublish.*"
            permissions: *view_permissions

security:
  globalJobDslSecurityConfiguration:
    useScriptSecurity: false

credentials:
  system:
    domainCredentials:
      - credentials:
          - string:
              scope: GLOBAL
              id: slack-token
              secret: '${SLACK_TOKEN}'
              description: Slack token
          - usernamePassword:
              scope: GLOBAL
              id: github-creds
              username: '${GITHUB_USERNAME}'
              password: '${GITHUB_TOKEN}'
              description: GitHub access using PAT
              
unclassified:
  buildDiscarders:
    configuredBuildDiscarders:
      - "jobBuildDiscarder"
      - simpleBuildDiscarder:
          discarder:
            logRotator:
              daysToKeepStr: "45"
              numToKeepStr: "300"
  location:
    url: "${JENKINS_URL}" # Replace with your Jenkins server's base URL
    adminAddress: "jenkins-admin@lightfurygames.com"  # Set the administrator's email address
  mailer:
    authentication:
      password: "${JENKINS_ADMIN_GOOGLE_APP_PASSWORD}"
      username: "jenkins-admin@lightfurygames.com"
    charset: "UTF-8"
    smtpHost: "smtp.gmail.com"
    smtpPort: "465"
    useSsl: true
    useTls: false
  slackNotifier:
    teamDomain:  lightfurygames.slack.com
    tokenCredentialId: slack-token
    botUser: true

groovy:
  - script: |
      / Validate tokens are not null
      if (!perforce_token || !authsphere_token || !swarm_agent_token) {
          println "One or more required environment variables are missing. Aborting token creation."
          return
      }

      // Retrieve user
      def user = User.get('jenkins-admin@lightfurygames.com', false, null)
      if (user == null) {
          println "User 'jenkins-admin@lightfurygames.com' not found."
          return
      }

      def tokenStore = user.getProperty(ApiTokenProperty.class)?.tokenStore
      if (tokenStore == null) {
          println "Token store not found for user. Ensure ApiTokenProperty is available."
          return
      }

      // Helper to ensure fixed token
      def ensureToken = { String name, String value ->
          if (!tokenStore.tokenList.any { it.name == name }) {
              tokenStore.addFixedNewToken(name, value)
              println "Created token: ${name}"
          } else {
              println "Token '${name}' already exists. Skipping."
          }
      }

      // Create tokens
      ensureToken("perforce-trigger-token", perforce_token)
      ensureToken("authsphere-token", authsphere_token)
      ensureToken("swarm-agent-token", swarm_agent_token)

      // Save user
      user.save()

jobs:
  - script: >
      job('Job_DSL_Seed') {
        parameters {
          stringParam('GIT_BRANCH', 'main', 'Git branch to load DSL scripts from')
        }
        scm {
          git {
            remote {
              url('https://github.com/LightFuryGames/lfg-titan-jenkins.git')
              credentials('github-creds')
            }
            branch('${GIT_BRANCH}')
          }
        }
        steps {
          dsl {
            external('jobs/**/*.groovy')
            additionalClasspath('src')
            lookupStrategy('JENKINS_ROOT')
            ignoreExisting(false) // forces rebuild of jobs
            removeAction('DELETE') // prunes obsolete jobs not in the github repo
          }
        }
        steps{
          Jenkins.instance.getAllItems(Job.class).each { job ->
            job.save()
          }
        }
      }
