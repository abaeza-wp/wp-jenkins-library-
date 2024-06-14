package com.worldpay.pipeline

class BuildContext {

    private static String tenant
    private static String componentName
    private static List<String> functionalEnvironments
    private static Boolean useFunctionalEnvironments

    private static String imageTag

    private static BuildProfile currentBuildProfile

    /**
     * For supported regions see: https://github.worldpay.com/pages/Engineering/portal/engineering/developer-platforms/kubernetes/guides/clusters/
     * @param environment the current environment
     * @param awsRegion the target aws region
     */
    static BuildProfile getClusterInformationForAwsRegion(String environment, String awsRegion) {
        switch ("${environment}-${awsRegion}") {
            case "dev-eu-west-1":
                currentBuildProfile = new BuildProfile(
                "dev-euwest1",
                new GkopCluster("dev", "eu-west-1", "7z55k", "api.eu-west-1-7z55k.dev.msp.worldpay.io:6443", "default-route-openshift-image-registry.apps.eu-west-1-7z55k.dev.msp.worldpay.io"))
                break
            case "dev-us-east-1":
                currentBuildProfile = new BuildProfile(
                "dev-useast1",
                new GkopCluster("dev", "us-east-1", "2m2bt", "api.us-east-1-2m2bt.dev.msp.worldpay.io:6443", "default-route-openshift-image-registry.apps.us-east-1-2m2bt.dev.msp.worldpay.io"))
                break
            case "staging-eu-west-1":
                currentBuildProfile = new BuildProfile(
                "staging-euwest1",
                new GkopCluster("stage", "eu-west-1", "hf2js", "api.eu-west-1-hf2js.stage.msp.worldpay.io:443", "default-route-openshift-image-registry.apps.eu-west-1-hf2js.stage.msp.worldpay.io"))
                break
            case "staging-us-east-1":
                currentBuildProfile = new BuildProfile(
                "staging-useast1",
                new GkopCluster("stage", "us-east-1", "aq0mb", "api.us-east-1-aq0mb.stage.msp.worldpay.io:443", "default-route-openshift-image-registry.apps.us-east-1-aq0mb.stage.msp.worldpay.io"))
                break
            case "prod-eu-west-1":
                currentBuildProfile = new BuildProfile(
                "prod-euwest1",
                new GkopCluster("prod", "eu-west-1", "i8tjd", "api.eu-west-1-i8tjd.prod.msp.worldpay.io:443", "default-route-openshift-image-registry.apps.eu-west-1-i8tjd.prod.msp.worldpay.io"))
                break
            case "prod-us-east-1":
                currentBuildProfile = new BuildProfile(
                "prod-useast1",
                new GkopCluster("prod", "us-east-1", "ob4yk", "api.us-east-1-ob4yk.prod.msp.worldpay.io:443", "default-route-openshift-image-registry.apps.us-east-1-ob4yk.prod.msp.worldpay.io:443"))
                break
            default:
                error "Unsupported GKOP aws region: ${awsRegion} for Environment ${environment}, If you think this is a mistake please consider raising an issue."
        }
        return currentBuildProfile
    }


    static BuildProfile getCurrentBuildProfile() {
        return currentBuildProfile
    }

    static Boolean shouldUseFunctionalEnvironments() {
        return useFunctionalEnvironments
    }

    static List<String> getFunctionalEnvironments() {
        return functionalEnvironments
    }

    static String getTenant() {
        return tenant
    }

    static String getComponentName() {
        return componentName
    }

    static String getFullName() {
        return "${tenant}-${componentName}"
    }


    static void initialize(String tenant, String componentName, List<String> functionalEnvironments) {
        this.tenant = tenant
        this.componentName = componentName

        if (functionalEnvironments == null) {
            useFunctionalEnvironments = false
            this.functionalEnvironments = []
        } else {
            useFunctionalEnvironments = true
            this.functionalEnvironments = functionalEnvironments
        }
    }

    static void setImageTag(String imageTag) {
        this.imageTag = imageTag
    }

    static String getImageTag() {
        return imageTag
    }
}
