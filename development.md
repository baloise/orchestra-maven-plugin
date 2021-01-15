#Update orchestra interface

To refresh WSDL from local orchestra installation:
   - mvn -Pfetch-wsdl -Dorchestra.soapclient.orchestraurl=<URL TO YOUR ORCHESTRA INSTANCE>/

#Release the plugin to bintray

you need update the [pom version](https://github.com/baloise/orchestra-maven-plugin/blob/master/pom.xml#L11) and push a [tag starting with v](https://github.com/baloise/orchestra-maven-plugin/releases) to trigger the [release pipeline](https://github.com/baloise/orchestra-maven-plugin/actions?query=workflow%3ARelease).
