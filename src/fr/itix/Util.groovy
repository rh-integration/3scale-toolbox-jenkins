#!groovy

package fr.itix

/*
 * Library classes cannot directly call steps such as sh or git. 
 * They can however implement methods, outside of the scope of an enclosing class,
 * which in turn invoke Pipeline steps.
 */
static def readOpenAPISpecificationFile(fileName) {
    if (fileName.toLowerCase().endsWith(".json")) {
        return readJSON(file: fileName)
    } else if (fileName.toLowerCase().endsWith(".yaml") || fileName.toLowerCase().endsWith(".yml")) {
        return readYaml(file: fileName)
    } else {
        throw new Exception("Can't decide between JSON and YAML on ${fileName}")
    }
}
