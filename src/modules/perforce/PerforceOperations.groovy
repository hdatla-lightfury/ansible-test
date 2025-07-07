package modules.perforce
import org.slf4j.LoggerFactory

class PerforceOperations {

    // Required env variables and paramaters
    String P4_USER
    String P4_PASS
    String P4_PORT
    String BUILD_MACHINE_NAME
    String CONTAINER_NAME
    String ROOT_DIR
    def steps
    String P4_STREAM
    String STREAM_NAME
    String P4_CHANGELIST
    String P4_WORKSPACE_DIR
    String P4_BASE_CLIENT
    String P4_CLIENT
    def logger = LoggerFactory.getLogger(PerforceOperations.class)
    static final Map<String, String> streamToBaseClientMapping = [
        '//titan-game/development': 'build-lightfury_titan_base_development',
        '//titan-game/mainline': 'build-lightfury_titan_base_mainline',
        '//titan-game/dev-engine': 'build-lightfury_titan_base_dev-engine',
    ]

    // Constructor to initialize the env variables
    PerforceOperations(def steps, def env) {
        this.steps = steps
        this.P4_USER = env.P4_USER
        this.P4_PASS = env.P4_PASS
        this.P4_PORT = env.P4_PORT
        this.BUILD_MACHINE_NAME = env.BUILD_MACHINE_NAME
        this.CONTAINER_NAME = env.CONTAINER_NAME
        this.ROOT_DIR = env.ROOT_DIR
        this.P4_STREAM = steps.params.P4_STREAM
        this.P4_CHANGELIST = steps.params.P4_CHANGELIST
        this.STREAM_NAME = P4_STREAM.tokenize('/').last()
        this.P4_WORKSPACE_DIR =  env.P4_WORKSPACE_DIR
        this.P4_BASE_CLIENT = streamToBaseClientMapping[P4_STREAM]
        this.P4_CLIENT = "${P4_BASE_CLIENT}_${BUILD_MACHINE_NAME}_${CONTAINER_NAME}"
    }

    String getEmailIdFromChangeList(String changeListId){

        def userId = steps.bat(script: """
                                @echo off
                                for /f "tokens=2" %%a in ('p4 -p %P4_PORT% -u %P4_USER% -P %P4_PASS% change -o ${changeListId} ^| findstr /B "User:"') do @echo %%a
                               """, returnStdout: true).trim()
        if(userId.isEmpty()){
            println "UserId is empty, return empty email"
            return ""
        }

        def emailId = steps.bat(script: """
                                @echo off
                                for /f "tokens=2" %%a in ('p4 -p %P4_PORT% -u %P4_USER% -P %P4_PASS% user -o ${userId} ^| findstr /B "Email:"') do @echo %%a
                               """, returnStdout: true).trim()
        if(emailId.isEmpty()){
            println "emailId is empty, return empty email"
            return ""
        }

        // Fallback: return empty user id.
        return emailId
    }

    String getCommitMessage(String changeListId) {

        def commitMessageOutput = steps.bat(script: """
                                            @echo off
                                            p4 -p %P4_PORT% -u %P4_USER% -P %P4_PASS% describe -s ${changeListId}
                                            """, returnStdout: true)

        // Split output into blocks using blank lines as delimiters
        def blocks = commitMessageOutput.split(/\r?\n\r?\n/)

        // Assuming block 1 contains the commit message, return it.
        if (blocks.size() >= 1) {
            return blocks[1].trim()
        }

        // Fallback: return empty release notes
        return ""

    }

     String getChangeList(){

        def changeListOutput = steps.bat(script: """
                                            @echo off
                                            p4 -p %P4_PORT% -u %P4_USER% -P %P4_PASS% changes -s submitted -m1 ${P4_STREAM}/...
                                            """, returnStdout: true)

        def matcher = changeListOutput =~ /Change\s+(\d+)/
        if (matcher.find()) {
            def number = matcher.group(1)
            return number
        } else {
            println "Number not found."
        }

        return "NotFound"

    }

    String syncWorkspaceWithRevision(Boolean forceSync = false) {
    // TODO: we can tweak the perforce settings to download code faster, however i didnt notice any noticeable difference
    // JIRA: https://lightfurygames.atlassian.net/browse/TITAN-2825
    // As our code base increases, its better to revisit and explore
    // def syncCommand = """
    // p4 set net.tcpsize=524288
    // thread can be a maximum of 10
    // p4 -p %P4_PORT% -u %P4_USER% -P %P4_PASS% -c %P4_CLIENT% sync --parallel "threads=10,batch=8,batchsize=524288,min=9,minsize=589824" -q ${stream}/...
    // """

        def syncCommand = """
        p4 -p %P4_PORT% -u %P4_USER% -P %P4_PASS% -c ${P4_CLIENT} sync //${P4_CLIENT}/...
        """

        // Add revision if provided
        if (P4_CHANGELIST.isEmpty()) {
           String changeList = getChangeList()

           if(changeList.equals("NotFound")){
                steps.error("Empty Change list, Exiting Jenkins pipeline")
                return
           }

           P4_CHANGELIST = changeList
        }
        syncCommand = syncCommand.replace("//${P4_CLIENT}/...", "//${P4_CLIENT}/...@${P4_CHANGELIST}")

        // Add the `-f` flag if `forceSync` is true
        if (forceSync) {
            syncCommand = syncCommand.replace("sync", "sync -f")
        }

        steps.bat """
        @echo off
        ${syncCommand}
        """
        return P4_CHANGELIST
    }

    void syncWorkspaceToLatest(Boolean forceSync = false) {
        syncWorkspaceWithRevision(forceSync)
    }

    void trust() {
        steps.bat """p4 -p ${P4_PORT} trust -y"""
    }

    void login() {
        steps.bat """
        @echo off
        echo %P4_PASS%> password.txt
        p4 -p %P4_PORT% -u %P4_USER% login -a < password.txt
        del password.txt
        """
    }

    void setupWorkspace() {
        logger.info("P4_WORKSPACE_DIR=${P4_WORKSPACE_DIR},\
                    P4_USER=${P4_USER},\
                    P4_PORT=${P4_PORT},\
                    P4_PASS=${P4_PASS},\
                    P4_BASE_CLIENT=${P4_BASE_CLIENT},\
                    P4_CLIENT=${P4_CLIENT}\
                    ")
        // Set up the workspace directory if it doesn't exist
        steps.bat """
        if not exist ${P4_WORKSPACE_DIR} mkdir ${P4_WORKSPACE_DIR}
        """
        // Retrieve the existing Perforce client spec
        steps.bat """
        p4 -p ${P4_PORT} -u ${P4_USER} -P ${P4_PASS} client -o ${P4_BASE_CLIENT} > base_client_spec.txt
        """

        // Modify the client spec and save it to a new client spec file
        def output = steps.powershell(returnStdout: true, script: """
        # Read the content of the base_client_spec.txt file
            \$content = Get-Content "base_client_spec.txt"

            # Replace the Client, Owner, and Root fields with class variables
            \$content = \$content -replace 'Client:.*', "Client: ${P4_CLIENT}"
            \$content = \$content -replace 'Owner:.*', "Owner: ${P4_USER}"
            \$content = \$content -replace 'Root:.*', "Root: ${P4_WORKSPACE_DIR}"

            # Filter out lines that start with "Host:"
            \$content = \$content | Where-Object { \$_.Trim() -notmatch '^Host:' }

            # Write the modified content to a new file
            \$content | Set-Content "new_client_spec.txt"
        """)

        // Create the new Perforce client spec
        steps.bat """
        p4 -p ${P4_PORT} -u ${P4_USER} -P ${P4_PASS} client -i < new_client_spec.txt
        """
    }

    void syncWorkspace(Boolean forceSync = false) {
        if (P4_CHANGELIST == null || P4_CHANGELIST == "") {
            syncWorkspaceToLatest(forceSync)
        } else {
            syncWorkspaceWithRevision(forceSync)
        }
    }
}
