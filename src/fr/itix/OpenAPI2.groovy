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

    void parseOpenAPISpecificationFile() {
        this.content = readOpenAPISpecificationFile(this.filename)
        assert content.swagger == "2.0"
        this.version = content.info.version
        assert this.version != null
        this.majorVersion = version.tokenize(".")[0]
    }
}