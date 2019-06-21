#!groovy

package com.redhat

/*
 * Library classes cannot directly call steps such as sh or git. 
 * They can however implement methods, outside of the scope of an enclosing class,
 * which in turn invoke Pipeline steps.
 */
def readOpenAPISpecificationFile(fileName) {
    if (fileName.toLowerCase().endsWith(".json")) {
        return readJSON(file: fileName)
    } else if (fileName.toLowerCase().endsWith(".yaml") || fileName.toLowerCase().endsWith(".yml")) {
        return readYaml(file: fileName)
    } else {
        throw new Exception("Can't decide between JSON and YAML on ${fileName}")
    }
}

def readFile(String filename) {
    return readFile(file: filename)
}

def removeFile(String filename) {
    sh "rm -f -- '$filename'"
}

String basename(path) {
    def filename = path.drop(path.lastIndexOf("/") != -1 ? path.lastIndexOf("/") + 1 : 0)
    filename = filename.replaceAll("[^-._a-zA-Z0-9]", "_")
    return filename
}

def writeOpenAPISpecificationFile(String fileName, def content) {
    if (fileName.toLowerCase().endsWith(".json")) {
        return writeJSON(file: fileName, json: content)
    } else if (fileName.toLowerCase().endsWith(".yaml") || fileName.toLowerCase().endsWith(".yml")) {
        return writeYaml(file: fileName, data: content)
    } else {
        throw new Exception("Can't decide between JSON and YAML on ${fileName}")
    }
}

def readJSON(String json) {
    return readJSON(text: json)
}
