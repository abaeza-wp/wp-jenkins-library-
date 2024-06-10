/*
    Used to login to a Kubernetes (GKOP) cluster, and provide a temporary short-lived Kubernetes token (for usage with
    CLI tools).

    This will also login the current build agent to the target Kubernetes cluster, so that the OpenShift client
    (oc command) can be used.
*/

def call(String type, String tenant, String component, String profile, Closure body) {
    script
    {
        withCredentials([
        string(credentialsId: "${env.SVC_TOKEN}", variable: "JENKINS_TOKEN")
        ])
        {
            echo "Logging into cluster..."

            def profileConfig = readYaml(file: "deployment/profiles/${profile}.yml")

            def params = ""
            def ignoreTls = profileConfig.deploy.ignore_tls
            if (ignoreTls) {
                params += "--insecure-skip-tls-verify"
            }

            if (profileConfig.deploy.cluster_username) {
                sh "oc login ${profileConfig.deploy.cluster} ${params} --username=${profileConfig.deploy.cluster_username} --password=${JENKINS_TOKEN}"
            } else {
                sh "oc login ${profileConfig.deploy.cluster} ${params} --token=${JENKINS_TOKEN}"
            }

            // Set namespace for service (fail-safe) - allowed to fail as may not exist yet
            sh "oc project ${profileConfig.deploy.namespace} || true"

            // Fetch token for image builds
            def kubernetesToken = sh(script: "oc whoami -t", returnStdout: true).trim()
            return kubernetesToken
        }
    }
    body.call()
}


return this

