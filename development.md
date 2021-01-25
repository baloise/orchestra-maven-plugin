# Update orchestra interface

To refresh WSDL from local orchestra installation:
   - mvn -Pfetch-wsdl -Dorchestra.soapclient.orchestraurl=<URL TO YOUR ORCHESTRA INSTANCE>/

# Release the plugin maven central

you need update the [pom version](https://github.com/baloise/orchestra-maven-plugin/blob/master/pom.xml#L11) and push to any branch to trigger the [release pipeline](https://github.com/baloise/orchestra-maven-plugin/actions?query=workflow%3ACR).


Any [-SNAPSHOT](https://maven.apache.org/guides/getting-started/#What_is_a_SNAPSHOT_version) will be deployed to the [sonatype oss snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/com/baloise/).


Any release will be published to [maven central](https://repo1.maven.org/maven2/com/baloise/).


⚠ Please note that you can only push a release once, as it can not be overwritten. Don't forget to increment the POM version after release see [example commit 2cda17](https://github.com/baloise/orchestra-maven-plugin/commit/2cda17d2fd23d963733b46b019a04430e526467e). You may find the [maven-release-plugin](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html) helpful. 
