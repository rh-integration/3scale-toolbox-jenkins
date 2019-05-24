#!groovy

package fr.itix

class OpenAPI2 {
    String filename
    def content
    String version
    String majorVersion

    OpenAPI2(Map conf) {
        assert conf.filename != null
        this.filename = conf.filename
    }

    static def readOpenAPISpecificationFile(fileName) {
        if (fileName.toLowerCase().endsWith(".json")) {
            return readJSON(file: fileName)
        } else if (fileName.toLowerCase().endsWith(".yaml") || fileName.toLowerCase().endsWith(".yml")) {
            return readYaml(file: fileName)
        } else {
            throw new Exception("Can't decide between JSON and YAML on ${fileName}")
        }
    }

    void parseOpenAPISpecificationFile() {
        this.content = readOpenAPISpecificationFile(this.filename)
        assert content.swagger == "2.0"
        this.version = content.info.version
        assert this.version != null
        this.majorVersion = version.tokenize(".")[0]
    }
}