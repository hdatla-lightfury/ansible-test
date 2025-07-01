package modules.slack

class SlackNotifier {

    def script
    String allSlackChannel
    String failureSlackChannel
    static final Map<String,String> COLOR_MAP = [
        failure: 'danger',   // red
        success: 'good',     // green
        aborted: 'warning'   // yellow
    ]
    // Constructor to initialize the environment variable
    SlackNotifier(def script, def env) {
        this.script = script
        allSlackChannel = env.SLACK_CHANNEL_ALL
        failureSlackChannel = env.SLACK_CHANNEL_FAILURE

    }

    // Method for the sending slack notification
    void sendNotification(
        String channel, String color, String message
    ){
        script.slackSend(
            channel: channel,
            color: color,
            message: message
        )
    }

    void notifyStatus(
        String status, String message
    ) {
        String color = COLOR_MAP[status]

        if (!COLOR_MAP.containsKey(status)) {
            throw new IllegalArgumentException("Unknown status: ${status}")
        }
        // Always send to "all"
        sendNotification(allSlackChannel, color, message)

        if (status in ["failure" , "aborted"]) {
            sendNotification(failureSlackChannel, color, message)
        }
    }

    String getTagsForNotificationMessage(
        String status, String commiterSlackId
    ){
        switch (status) {
            case "failure":
                return "<@${commiterSlackId}> <!channel>"
            case "aborted":
                return "<!channel>"
            case "success":
                return "<@${commiterSlackId}>"
            default:
                throw new IllegalArgumentException("Unknown status: ${status}")
        }
    }

    String getMessageForStatus(
        String status, String jobName, String buildNumber, 
        String changeList, String capturedBuildUrl
    ){
        switch (status) {
            case "failure":
                return "Build failed in ${jobName} - ${buildNumber} for CL - ${changeList}. See details: ${capturedBuildUrl}"
            case "aborted":
                return "Build was aborted in ${jobName} - ${buildNumber}. See details: ${capturedBuildUrl}"
            case "success":
                return "Build was successful for ${jobName} - ${buildNumber} for CL - ${changeList}"
            default:
                throw new IllegalArgumentException("Unknown status: ${status}")
        }
    }

    String getPrependPrefixMsg(
        boolean nightly, boolean initialSetup
    ){
        String msg = ""
        msg += nightly ? "[Nightly]" : ""
        msg += initialSetup ? "[Initial-Setup]" : ""
        return msg
    }

    String craftMessageForStatus(
        String status, String commiterSlackId, String jobName,
        String buildNumber, String changeList, String capturedBuildUrl,
        boolean nightly, boolean initialSetup
    ){
        String message = ""
        String tags = getTagsForNotificationMessage(status, commiterSlackId)
        String prependPrefixMsg = getPrependPrefixMsg(nightly, initialSetup)
        String messageForStatus = getMessageForStatus(status, jobName, buildNumber, changeList, capturedBuildUrl)
        message += "${tags} ${prependPrefixMsg} ${messageForStatus}"
        return message
    }

}
