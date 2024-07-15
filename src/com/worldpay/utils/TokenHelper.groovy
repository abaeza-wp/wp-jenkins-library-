package com.worldpay.utils

class TokenHelper {
    static String devTokenName(namespace, awsRegion) {
        return tokenNameOf("dev", namespace, awsRegion)
    }
    static String tokenNameOf(environment, namespace, awsRegion, String functionalEnvironment) {
        return tokenNameOf(environment, "${namespace}-${functionalEnvironment}", awsRegion)
    }

    static String tokenNameOf(environment, namespace, awsRegion) {
        def awsRegionString = awsRegion.replace('-', '')
        return "svc_token-${namespace}-${environment}-${awsRegionString}"
    }
}
