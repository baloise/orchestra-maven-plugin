A maven plugin to manage [soffico Orchestra](https://orchestra.soffico.de/).
The plugin is developed as open source project and not ( yet ) affiliated with soffico.

The design goals are

- keep your POM clean: no dependencies
- keep your workspace clean : flexibilty in layout with good defaults
 
## Quick start

Prerequisite

- Maven is working. That's all. No pom.xml required.
- add jcenter repo to your maven settings

```
<pluginRepository>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
    <id>bintray</id>
    <name>bintray</name>
    <url>http://jcenter.bintray.com</url>
</pluginRepository>
```

... to be completed

## Make your life easier with plugin groups

Add the following to your *~/.m2/settings.xml*

```
<pluginGroups>
    <pluginGroup>com.baloise.maven</pluginGroup>
</pluginGroups>
```

now you can use something like

`> mvn orchestra:deploy -Dscenario=path\to\scenario -Dserver=https://orchestra.mycorp.com:9443`

[(tell me more)](http://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html#Configuring_Maven_to_Search_for_Plugins)

## Goals

currently the following use cases are supported

- [package](scenario-package-mojo.html) a scenario source folder into a PSC file
- [(re)deploy](scenario-deploy-mojo.html) a PSC file and associated landscapes to a server


## I don't want to use maven

You can just reuse the soap client and helper methods in a small library without dependencies on maven, i.e

https://dl.bintray.com/baopso/mvn/com/baloise/maven/orchestra-maven-plugin/0.4.1/orchestra-maven-plugin-0.4.1-lib.jar


[![Build Status](https://travis-ci.org/baloise/jenkins-maven-plugin.svg)](https://travis-ci.org/baloise/orchestra-maven-plugin)
