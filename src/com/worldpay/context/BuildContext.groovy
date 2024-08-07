package com.worldpay.context

import com.cloudbees.groovy.cps.NonCPS

class BuildContext {

    private static String tenant
    private static String componentName
    private static List<String> functionalEnvironments
    private static Boolean useFunctionalEnvironments

    private static String imageTag

    private static BuildProfile currentBuildProfile
    private static Map<String, BuildProfile> gkopSupportedRegions = getGkopSupportedRegions()

    /**
     * For supported regions see: https://github.worldpay.com/pages/Engineering/portal/engineering/developer-platforms/kubernetes/guides/clusters/
     * @param environment the current environment
     * @param awsRegion the target aws region
     */
    static BuildProfile getBuildProfileForAwsRegion(String environment, String awsRegion) {
        def lookupKey = "${environment}-${awsRegion}"
        def cluster = gkopSupportedRegions[lookupKey]

        if (cluster == null) {
            error "Unsupported GKOP aws region: ${awsRegion} for Environment ${environment}, Used ${lookupKey} as lookup key, If you think this is a mistake please consider raising an issue."
        }
        currentBuildProfile = cluster
        return cluster
    }

    static String mapAwsRegionFromProfile(String profileName) {
        if (profileName.contains("euwest1")) {
            return "eu-west-1"
        } else if (profileName.contains("useast1")) {
            return "us-east-1"
        }
        error "Could not get AWS region for profile name ${profileName}, If you think this is a mistake please consider raising an issue."
    }


    static BuildProfile getCurrentBuildProfile() {
        return currentBuildProfile
    }

    static Boolean getUseFunctionalEnvironments() {
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

    @NonCPS
    static Map<String, BuildProfile> getGkopSupportedRegions() {
        return [
            "dev-eu-west-1"    : new BuildProfile("dev-euwest1", new GkopCluster("dev", "eu-west-1", "7z55k", "api.eu-west-1-7z55k.dev.msp.worldpay.io:6443", "default-route-openshift-image-registry.apps.eu-west-1-7z55k.dev.msp.worldpay.io")),
            "dev-us-east-1"    : new BuildProfile("dev-useast1", new GkopCluster("dev", "us-east-1", "2m2bt", "api.us-east-1-2m2bt.dev.msp.worldpay.io:6443", "default-route-openshift-image-registry.apps.us-east-1-2m2bt.dev.msp.worldpay.io")),
            "stage-eu-west-1": new BuildProfile("stage-euwest1", new GkopCluster("stage", "eu-west-1", "hf2js", "api.eu-west-1-hf2js.stage.msp.worldpay.io:443", "default-route-openshift-image-registry.apps.eu-west-1-hf2js.stage.msp.worldpay.io")),
            "stage-us-east-1": new BuildProfile("stage-useast1", new GkopCluster("stage", "us-east-1", "aq0mb", "api.us-east-1-aq0mb.stage.msp.worldpay.io:443", "default-route-openshift-image-registry.apps.us-east-1-aq0mb.stage.msp.worldpay.io")),
            "prod-eu-west-1"   : new BuildProfile("prod-euwest1", new GkopCluster("prod", "eu-west-1", "i8tjd", "api.eu-west-1-i8tjd.prod.msp.worldpay.io:443", "default-route-openshift-image-registry.apps.eu-west-1-i8tjd.prod.msp.worldpay.io")),
            "prod-us-east-1"   : new BuildProfile("prod-useast1", new GkopCluster("prod", "us-east-1", "ob4yk", "api.us-east-1-ob4yk.prod.msp.worldpay.io:443", "default-route-openshift-image-registry.apps.us-east-1-ob4yk.prod.msp.worldpay.io:443")),
        ]
    }
}
