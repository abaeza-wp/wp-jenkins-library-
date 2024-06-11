/*
    Used to send slack notifications with the outcome of sysdig scan
*/

def call(Boolean outcome, String reportLocation) {
    script
    {
        def message = [
        channel : "$env.SLACK_SYSDIG_CHANNEL",
        username: "Sessions Config Sysdig Scan",
        emoji   : "",
        color   : "",
        header  : "",
        text    : "",
        location: "Report can be retrieved from here:\n${reportLocation}"
        ]

        var scanMessage = "Sysdig Scan for *${env.SERVICE_NAME}* build *No.${env.BUILD_NUMBER}* for *${env.BUILD_APP_VERSION}* : *${env.BUILD_COMMIT_HASH}*"

        if (outcome) {
            message.emoji = ":+1:"
            message.color = "#31AD72"
            message.header = "Success"
            message.text = "${scanMessage} has completed successfully."
        } else {
            message.emoji = ":-1:"
            message.color = "#EA5E1A"
            message.header = "Failed"
            message.text = "${scanMessage} has failed.\n"
        }

        sendSlackNotification(message)
    }
}


/*
    Used to send a slack notification
*/

def sendSlackNotification(Object message) {

    def payload = "{" +
    "\"channel\":\"${message.channel}\"," +
    "\"username\":\"${message.username}\"," +
    "\"icon_emoji\":\"${message.emoji}\"," +
    "\"attachments\":[" +
    "{" +
    "\"color\":\"${message.color}\"," +
    "\"blocks\":[" +
    "{" +
    "\"type\":\"header\"," +
    "\"text\":{" +
    "\"type\":\"plain_text\"," +
    "\"text\":\"${message.header}\"" +
    "}" +
    "}," +
    "{" +
    "\"type\":\"context\"," +
    "\"elements\":[" +
    "{" +
    "\"type\":\"mrkdwn\"," +
    "\"text\":\"${message.text}\"" +
    "}," +
    "{" +
    "\"type\":\"mrkdwn\"," +
    "\"text\":\"${message.location}\"" +
    "}" +
    "]" +
    "}" +
    "]" +
    "}" +
    "]" +
    "}"

    withCredentials([string(credentialsId: "${env.SLACK_WEBHOOK_URL}", variable: "SLACK_URL")]) {
        echo "Sending notification to slack channel"

        def response = sh(script: "curl -X POST --data-urlencode \"payload=\"'${payload}' '${env.SLACK_URL}'", returnStdout: true)

        if (response.contains("ok")) {
            echo "Slack notification sent"
        } else {
            echo "Unable to send slack notification"
            echo "Response: ${response}"
        }
    }
}
