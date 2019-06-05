#!groovy

package fr.itix

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

  List<ApplicationPlan> plans = []
  conf.applicationPlans.each{
    if (it.systemName == null || it.name == null) {
      throw new Exception("Missing property in application plan: name or systemName")
    }

    ApplicationPlan plan = new ApplicationPlan(it)
    plans.add(plan)
  }

  OpenAPI2 openapi = new OpenAPI2(conf.openapi)
  openapi.parseOpenAPISpecificationFile()
  ThreescaleEnvironment environment = new ThreescaleEnvironment(conf.environment)
  ToolboxConfiguration toolbox = new ToolboxConfiguration(conf.toolbox + ["JOB_BASE_NAME": JOB_BASE_NAME, "BUILD_NUMBER": BUILD_NUMBER])
  ThreescaleService service = new ThreescaleService([ "openapi": openapi, "environment": environment, "toolbox": toolbox, applicationPlans: plans ] + conf.service)

  return service
}

Map getDefaultApplicationCredentials(ThreescaleService service, String applicationName) {
  String targetSystemName = service.environment.targetSystemName
  String destination = service.toolbox.destination
  String secretName = service.toolbox.secretName
  String secret = ""
  if (secretName != null) {
    openshift.withCluster() {
      openshift.withProject(service.toolbox.openshiftProject) {
        def secretObject = openshift.selector('secret', secretName).object()
        secret = secretObject.data[".3scalerc.yaml"]
      }
    }
  }

  writeFile file: "app_id", text: applicationName + targetSystemName + destination + secret
  writeFile file: "app_secret", text: "secret" + applicationName + targetSystemName + destination + secret
  def app_id = sha1 file: "app_id"
  def app_secret = sha1 file: "app_secret"

  // TODO: return user_key OR client_id/client_secret depending on the security scheme of the OpenAPI
  return [
    userKey: app_id,
    clientId: app_id,
    clientSecret: app_secret
  ]
}