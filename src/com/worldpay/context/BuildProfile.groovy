package com.worldpay.context

class BuildProfile {
    private Object profileName
    private GkopCluster cluster

    BuildProfile(String profileName, GkopCluster cluster) {
        this.profileName = profileName
        this.cluster = cluster
    }

    Object getProfileName() {
        return profileName
    }

    GkopCluster getCluster() {
        return cluster
    }
}
