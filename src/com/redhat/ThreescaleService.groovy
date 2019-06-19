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


        // Compute the target system_name
        this.environment.targetSystemName = (this.environment.environmentName != null ? "${this.environment.environmentName}_" : "") + this.environment.baseSystemName + "_${this.openapi.majorVersion}"

        def baseName = basename(this.openapi.filename)
        def globalOptions = toolbox.getGlobalToolboxOptions()
        def commandLine = "3scale import openapi ${globalOptions} -t ${this.environment.targetSystemName} -d ${this.toolbox.destination} /artifacts/${baseName}"
        if (this.environment.stagingPublicBaseURL != null) {
            commandLine += " --staging-public-base-url=${this.environment.stagingPublicBaseURL}"
        }
        if (this.environment.productionPublicBaseURL != null) {
            commandLine += " --production-public-base-url=${this.environment.productionPublicBaseURL}"
        }
        if (this.environment.privateBaseUrl != null) {
            commandLine += " --override-private-base-url=${this.environment.privateBaseUrl}"
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
            def commandLine = "3scale application-plan apply ${globalOptions} ${this.toolbox.destination} ${this.environment.targetSystemName} ${it.systemName} --approval-required=${it.approvalRequired} --cost-per-month=${it.costPerMonth} --end-user-required=${it.endUserRequired} --name=${it.name} --publish=${it.published} --setup-fee=${it.setupFee} --trial-period-days=${it.trialPeriodDays}"
            if (it.defaultPlan) {
                commandLine += " --default"
            }

            toolbox.runToolbox(commandLine: commandLine,
                    jobName: "apply-application-plan-${it.systemName}")
        }
    }

    void applyApplication(Map applicationMap) {

        def globalOptions = toolbox.getGlobalToolboxOptions()
        this.applications.each {
            def commandLine = "3scale application apply --account=${it.account}  --plan=${it.plan}  --service=${this.environment.targetSystemName} --name=${it.name}  --description=${it.description}  ${globalOptions} ${this.toolbox.destination}   ${it.name} "

            toolbox.runToolbox(commandLine: commandLine,
                    jobName: "apply-application-${it.name}")
        }
    }

    Map readProxy(String environment) {

        def globalOptions = toolbox.getGlobalToolboxOptions()
        def commandLine = "3scale proxy-config show ${globalOptions} ${this.toolbox.destination} ${this.environment.targetSystemName} ${environment}"
        def proxyDefinition =toolbox.runToolbox(commandLine: commandLine,
                jobName: "apply-proxy-config-show")

        return new Util().readJSON(proxyDefinition.stdout).content.proxy as Map
    }

    void promoteToProduction() {


        def globalOptions = toolbox.getGlobalToolboxOptions()
        def commandLine = "3scale proxy-config promote ${globalOptions} ${this.toolbox.destination} ${this.environment.targetSystemName}"
        toolbox.runToolbox(commandLine: commandLine,
                jobName: "apply-proxy-config-promote")
    }

    static String basename(path) {
        def filename = path.drop(path.lastIndexOf("/") != -1 ? path.lastIndexOf("/") + 1 : 0)
        filename = filename.replaceAll("[^-._a-zA-Z0-9]", "_")
        return filename
    }

}

