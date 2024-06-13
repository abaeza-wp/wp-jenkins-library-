import com.worldpay.pipeline.BuildConfigurationContext

/*
 Used to login to a Kubernetes (GKOP) cluster, and provide a temporary short-lived Kubernetes token (for usage with
 CLI tools).
 This will also login the current build agent to the target Kubernetes cluster, so that the OpenShift client
 (oc command) can be used.
 */

def call() {
    withCredentials([
        string(credentialsId: "${env.SVC_TOKEN}", variable: "JENKINS_TOKEN")
    ]) {
        echo "Logging into cluster..."

        def clusterApi = BuildConfigurationContext.getCurrentBuildConfig().cluster.api
        def profileName = BuildConfigurationContext.getCurrentBuildConfig().profileName

        def profile = readYaml(file: "deployment/profiles/${profileName}.yml")

        def params = ""
        def ignoreTls = profile.deploy.ignore_tls
        if (ignoreTls) {
            params += "--insecure-skip-tls-verify"
        }


        if (profile.deploy.cluster_username) {
            sh "oc login ${clusterApi} ${params} --username=${profile.deploy.cluster_username} --password=${JENKINS_TOKEN}"
        } else {
            sh "oc login ${clusterApi} ${params} --token=${JENKINS_TOKEN}"
        }

        // Set namespace for service (fail-safe) - allowed to fail as may not exist yet
        sh "oc project ${profile.deploy.namespace} || true"

        kubernetesToken = sh(script: "oc whoami -t", returnStdout: true).trim()
        return kubernetesToken
    }
}
