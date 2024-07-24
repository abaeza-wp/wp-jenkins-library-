/*
 Used to send a slack notification
 */

def call(Object message) {
    def payload = '{' +
            "\"channel\":\"${message.channel}\"," +
            "\"username\":\"${message.username}\"," +
            "\"icon_emoji\":\"${message.emoji}\"," +
            '\"attachments\":[' +
            '{' +
            "\"color\":\"${message.color}\"," +
            '\"blocks\":[' +
            '{' +
            '\"type\":\"header\",' +
            '\"text\":{' +
            '\"type\":\"plain_text\",' +
            "\"text\":\"${message.header}\"" +
            '}' +
            '},' +
            '{' +
            '\"type\":\"context\",' +
            '\"elements\":[' +
            '{' +
            '\"type\":\"mrkdwn\",' +
            "\"text\":\"${message.text}\"" +
            '},' +
            '{' +
            '\"type\":\"mrkdwn\",' +
            "\"text\":\"${message.location}\"" +
            '}' +
            ']' +
            '}' +
            ']' +
            '}' +
            ']' +
            '}'

    withCredentials([
        string(credentialsId: "${env.SLACK_WEBHOOK_URL}", variable: 'SLACK_URL')
    ]) {
        echo 'Sending notification to slack channel'

        def response = sh(script: "curl -X POST --data-urlencode \"payload=\"'${payload}' '${env.SLACK_URL}'", returnStdout: true)

        if (response.contains('ok')) {
            echo 'Slack notification sent'
        } else {
            echo 'Unable to send slack notification'
            echo "Response: ${response}"
        }
    }
}
