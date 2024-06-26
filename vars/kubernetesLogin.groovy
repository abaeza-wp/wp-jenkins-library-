import com.worldpay.context.BuildContext

/*
 Used to login to a Kubernetes (GKOP) cluster, and provide a temporary short-lived Kubernetes token (for usage with
 CLI tools).
 This will also login the current build agent to the target Kubernetes cluster, so that the OpenShift client
 (oc command) can be used.
 */

def call(String clusterUsername) {
    call(clusterUsername, null, false)
}

def call(String jenkinsCredentialId, String namespace) {
    call(null, jenkinsCredentialId, namespace, false)
}

def call(String clusterUsername, String jenkinsCredentialId, String namespace) {
    def clusterApi = BuildContext.currentBuildProfile.cluster.api
    call(clusterUsername, clusterApi, jenkinsCredentialId, namespace, false)
}

def call(String clusterUsername, String jenkinsCredentialId, String namespace, Boolean ignoreTls) {
    def clusterApi = BuildContext.currentBuildProfile.cluster.api
    call(clusterUsername, clusterApi, jenkinsCredentialId, namespace, ignoreTls)
}

def call(String clusterUsername, String clusterApi, String jenkinsCredentialId, String namespace, Boolean ignoreTls) {
    withCredentials([
    string(credentialsId: jenkinsCredentialId, variable: "JENKINS_TOKEN")
    ]) {
        echo "Logging into cluster..."

        def params = ""
        if (ignoreTls) {
            params += "--insecure-skip-tls-verify"
        }


        if (clusterUsername != null) {
            sh "oc login ${clusterApi} ${params} --username=${clusterUsername} --password=${JENKINS_TOKEN}"
        } else {
            sh "oc login ${clusterApi} ${params} --token=${JENKINS_TOKEN}"
        }

        if (namespace) {
            // Set namespace for service (fail-safe) - allowed to fail as may not exist yet
            sh "oc project ${namespace} || true"
        }
        kubernetesToken = sh(script: "oc whoami -t", returnStdout: true).trim()
        return kubernetesToken
    }
}
