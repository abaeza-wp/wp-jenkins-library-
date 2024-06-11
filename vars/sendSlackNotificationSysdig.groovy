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


