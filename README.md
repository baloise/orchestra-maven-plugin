# orchstra-maven-plugin

A maven plugin to manage soffico orchestra.

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

## Configuration options

Of course you have all the options as where to set the properties
[(tell me more)](http://docs.codehaus.org/display/MAVENUSER/MavenPropertiesGuide)

... to be completed

[![Build Status](https://travis-ci.org/baloise/jenkins-maven-plugin.svg)](https://travis-ci.org/baloise/orchestra-maven-plugin)


## I don't want to use maven

You can just reuse the soap client and helper methods in a small library without dependencies on maven, i.e

https://dl.bintray.com/baopso/mvn/com/baloise/maven/orchestra-maven-plugin/0.1.6/orchestra-maven-plugin-0.1.6-lib.jar

... to be completed

