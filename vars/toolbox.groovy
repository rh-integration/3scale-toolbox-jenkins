#!groovy

package com.redhat

String generateRandomBaseSystemName() {
  def random = new Random()
  return String.format("testcase_%08x%08x", random.nextInt(), random.nextInt())
}

ThreescaleService prepareThreescaleService(Map conf) {
  assert conf.openapi != null
  assert conf.environment != null
  assert conf.toolbox != null
  assert conf.service != null
  assert conf.applicationPlans != null
  assert conf.applications != null

  List<ApplicationPlan> plans = []
  conf.applicationPlans.each{
    if ((it.systemName == null || it.name == null) && !it.artefactFile?.trim()) {
      throw new Exception("Missing property in application plan: name or systemName or artefactFile ")
    }

    ApplicationPlan plan = new ApplicationPlan(it)
    plans.add(plan)
  }


  OpenAPI2 openapi = new OpenAPI2(conf.openapi)
  openapi.parseOpenAPISpecificationFile()
  openapi.updateTitleWithEnvironmentAndVersion(conf.environment.environmentName)

  if (conf.environment.targetSystemName == null) {
    // Compute the target system_name
    conf.environment.targetSystemName = (conf.environment.environmentName != null ? "${conf.environment.environmentName}_" : "") + conf.environment.baseSystemName + "_${openapi.majorVersion}"
  }

  // compute the public base urls from system_name, version number and wildcard domain (for semantic versioning)
  String apiBaseName = conf.environment.targetSystemName.replaceAll(/[^-0-9a-zA-Z]/, "-").toLowerCase()
  if (conf.environment.stagingPublicBaseURL == null && conf.environment.publicStagingWildcardDomain != null) {
    conf.environment.stagingPublicBaseURL = "https://${apiBaseName}.${conf.environment.publicStagingWildcardDomain}"
  }
  if (conf.environment.productionPublicBaseURL == null && conf.environment.publicProductionWildcardDomain != null) {
    conf.environment.productionPublicBaseURL = "https://${apiBaseName}.${conf.environment.publicProductionWildcardDomain}"
  }

  ThreescaleEnvironment environment = new ThreescaleEnvironment(conf.environment)
  ToolboxConfiguration toolbox = new ToolboxConfiguration(conf.toolbox + ["JOB_BASE_NAME": JOB_BASE_NAME, "BUILD_NUMBER": BUILD_NUMBER])

  List<Application> apps = []
  conf.applications.each{
    if (it.account == null || it.name == null) {
      throw new Exception("Missing property in application : name or account")
    }

    Map credentials = [:]
    if ((openapi.securityScheme == ThreescaleSecurityScheme.OPEN
      || openapi.securityScheme == ThreescaleSecurityScheme.APIKEY) && it.userkey == null) {

        credentials = getDefaultApplicationCredentials(environment, toolbox, it.name).findAll { key, value -> key == "userkey" }
    } else if (openapi.securityScheme == ThreescaleSecurityScheme.OIDC && it.clientId == null && it.clientSecret == null) {
        credentials = getDefaultApplicationCredentials(environment, toolbox, it.name).findAll { key, value -> key == "clientId" || key == "clientSecret" }
    }

    Application app = new Application(it + credentials)
    apps.add(app)
  }

  ThreescaleService service = new ThreescaleService([ "openapi": openapi, "environment": environment, "toolbox": toolbox, applicationPlans: plans ,"applications":apps] + conf.service)

  return service
}

Map getDefaultApplicationCredentials(ThreescaleEnvironment environment, ToolboxConfiguration toolbox, String applicationName) {
  String targetSystemName = environment.targetSystemName
  String destination = toolbox.destination
  String secretName = toolbox.secretName
  String secret = ""
  if (secretName != null) {
    openshift.withCluster() {
      openshift.withProject(toolbox.openshiftProject) {
        def secretObject = openshift.selector('secret', secretName).object()
        secret = secretObject.data[".3scalerc.yaml"]
      }
    }
  }

  writeFile file: "app_id", text: applicationName + targetSystemName + destination + secret
  writeFile file: "app_secret", text: "secret" + applicationName + targetSystemName + destination + secret
  def app_id = sha1 file: "app_id"
  def app_secret = sha1 file: "app_secret"

  return [
    userkey: app_id,
    clientId: app_id,
    clientSecret: app_secret
  ]
}