package com.worldpay.utils

class TokenHelper {
    static String devTokenName(namespace, awsRegion) {
        return tokenNameOf("dev", namespace, awsRegion)
    }
    static String tokenNameOf(environment, appName, awsRegion, String functionalEnvironment) {
        return tokenNameOf(environment, "${appName}-${functionalEnvironment}", awsRegion)
    }

    static String tokenNameOf(environment, appName, awsRegion) {
        def awsRegionString = awsRegion.replace('-', '')
        return "svc_token-${appName}-${environment}-${awsRegionString}"
    }
}
