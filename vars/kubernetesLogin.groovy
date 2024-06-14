import com.worldpay.pipeline.BuildContext

/*
 Used to login to a Kubernetes (GKOP) cluster, and provide a temporary short-lived Kubernetes token (for usage with
 CLI tools).
 This will also login the current build agent to the target Kubernetes cluster, so that the OpenShift client
 (oc command) can be used.
 */

def call() {
    call(false, null)
}

def call(Boolean ignoreTls, String clusterUsername) {
    withCredentials([
        string(credentialsId: "${env.SVC_TOKEN}", variable: "JENKINS_TOKEN")
    ]) {
        echo "Logging into cluster..."

        def clusterApi = BuildContext.getCurrentBuildProfile().cluster.api

        def params = ""
        if (ignoreTls) {
            params += "--insecure-skip-tls-verify"
        }


        if (BuildContext.getCurrentBuildProfile().getCluster().isDev()) {
            sh "oc login ${clusterApi} ${params} --username=${clusterUsername} --password=${JENKINS_TOKEN}"
        } else {
            sh "oc login ${clusterApi} ${params} --token=${JENKINS_TOKEN}"
        }

        //TODO: Maybe clean up as this will be ensured by helm
        // Set namespace for service (fail-safe) - allowed to fail as may not exist yet
        //        sh "oc project ${namespace} || true"

        kubernetesToken = sh(script: "oc whoami -t", returnStdout: true).trim()
        return kubernetesToken
    }
}
