#!groovy

package com.redhat

class Application {
    String account
    String name
    String description
    String plan
    // Disabled for now because of https://issues.jboss.org/browse/THREESCALE-2844
    // boolean active = true
    String userkey
    String clientId
    String clientSecret

}