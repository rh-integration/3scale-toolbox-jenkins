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

  OpenAPI2 openapi = new OpenAPI2(conf.openapi)
  openapi.parseOpenAPISpecificationFile()
  ThreescaleEnvironment environment = new ThreescaleEnvironment(conf.environment)
  ToolboxConfiguration toolbox = new ToolboxConfiguration(conf.toolbox)
  ThreescaleService service = new ThreescaleService([ "openapi": openapi, "environment": environment, "toolbox": toolbox ] + conf.service)

  return service
}

def readOpenAPISpecificationFile(fileName) {
    if (fileName.toLowerCase().endsWith(".json")) {
        return readJSON(file: fileName)
    } else if (fileName.toLowerCase().endsWith(".yaml") || fileName.toLowerCase().endsWith(".yml")) {
        return readYaml(file: fileName)
    } else {
        throw new Exception("Can't decide between JSON and YAML on ${fileName}")
    }
}
