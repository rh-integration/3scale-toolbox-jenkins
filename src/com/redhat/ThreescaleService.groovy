#!groovy

package com.redhat

class ThreescaleService {
    OpenAPI2 openapi
    List<ApplicationPlan> applicationPlans
    List<Application> applications
    ToolboxConfiguration toolbox
    ThreescaleEnvironment environment

    void importOpenAPI() {
        Util util = new Util()

        def baseName = basename(this.openapi.filename)
        def globalOptions = toolbox.getGlobalToolboxOptions()
        def commandLine = [ "3scale", "import", "openapi" ] + globalOptions + [ "-t", this.environment.targetSystemName, "-d", this.toolbox.destination, "/artifacts/${baseName}" ]
        if (this.environment.stagingPublicBaseURL != null) {
            commandLine += "--staging-public-base-url=${this.environment.stagingPublicBaseURL}"
        }
        if (this.environment.productionPublicBaseURL != null) {
            commandLine += "--production-public-base-url=${this.environment.productionPublicBaseURL}"
        }
        if (this.environment.privateBaseUrl != null) {
            commandLine += "--override-private-base-url=${this.environment.privateBaseUrl}"
        }

        if (this.openapi.securityScheme == ThreescaleSecurityScheme.OPEN) {
            commandLine += "--default-credentials-userkey=${this.applications[0].userkey}"
        }

        if (this.openapi.securityScheme == ThreescaleSecurityScheme.OIDC) {
            commandLine += "--oidc-issuer-endpoint=${this.environment.oidcIssuerEndpoint}"
        }

        if (! this.openapi.validateOAS) {
            commandLine += "--skip-openapi-validation"
        }

        toolbox.runToolbox(commandLine: commandLine,
                jobName: "import",
                openAPI: [
                        "filename": baseName,
                        "content" : util.readFile(this.openapi.filename)
                ])
    }

    void applyApplicationPlans() {
        def globalOptions = toolbox.getGlobalToolboxOptions()
        this.applicationPlans.each {
            // the issue THREESCALE-2816 for resolving update error
            def commandLine = [ "3scale", "application-plan", "apply" ] + globalOptions + [ this.toolbox.destination, this.environment.targetSystemName, it.systemName ]
            commandLine += [ "--approval-required=${it.approvalRequired}",
                             "--cost-per-month=${it.costPerMonth}",
                             "--name=${it.name}",
                             "--publish=${it.published}",
                             "--setup-fee=${it.setupFee}",
                             "--trial-period-days=${it.trialPeriodDays}" ]

            if (it.defaultPlan) {
                commandLine += "--default"
            }

            toolbox.runToolbox(commandLine: commandLine,
                    jobName: "apply-application-plan-${it.systemName}")
        }
    }

    void applyApplication() {

        def globalOptions = toolbox.getGlobalToolboxOptions()
        this.applications.each {
            def commandLine = [ "3scale", "application", "apply" ] + globalOptions + this.toolbox.destination
            if (this.openapi.securityScheme == ThreescaleSecurityScheme.APIKEY
             || this.openapi.securityScheme == ThreescaleSecurityScheme.OPEN) {

                commandLine += it.userkey
            } else if (this.openapi.securityScheme == ThreescaleSecurityScheme.OIDC) {
                commandLine += [ it.clientId, "--application-key=${it.clientSecret}" ]
            } else {
                throw new Exception("NOT_IMPLEMENTED")
            }

            commandLine += [ "--name=${it.name}",
                             "--description=${it.description != null ? it.description : "Created by the 3scale_toolbox from a Jenkins pipeline."}",
                             "--plan=${it.plan}",
                             "--service=${this.environment.targetSystemName}",
                             "--account=${it.account}" ]

            // Disabled for now because of https://issues.jboss.org/browse/THREESCALE-2844
            /*
            if (it.active) {
                commandLine += "--resume"
            } else {
                commandLine += "--suspend"
            }
            */

            toolbox.runToolbox(commandLine: commandLine,
                    jobName: "apply-application-${it.name}")
        }
    }

    Map readProxy(String environment) {
        assert environment != null

        def globalOptions = toolbox.getGlobalToolboxOptions()
        def commandLine = [ "3scale", "proxy-config", "show" ] + globalOptions + [ this.toolbox.destination, this.environment.targetSystemName, environment ]
        def proxyDefinition =toolbox.runToolbox(commandLine: commandLine,
                jobName: "apply-proxy-config-show")

        return new Util().readJSON(proxyDefinition.stdout).content.proxy as Map
    }

    void promoteToProduction() {


        def globalOptions = toolbox.getGlobalToolboxOptions()
        def commandLine = [ "3scale", "proxy-config", "promote" ] + globalOptions + [ this.toolbox.destination, this.environment.targetSystemName ]
        toolbox.runToolbox(commandLine: commandLine,
                jobName: "apply-proxy-config-promote")
    }

    static String basename(path) {
        def filename = path.drop(path.lastIndexOf("/") != -1 ? path.lastIndexOf("/") + 1 : 0)
        filename = filename.replaceAll("[^-._a-zA-Z0-9]", "_")
        return filename
    }

}

