A maven plugin to manage [soffico Orchestra](https://orchestra.soffico.de/).
The plugin is developed as open source project and not ( yet ) affiliated with soffico.

The design goals are

- keep your POM clean: no dependencies
- keep your workspace clean : flexibilty in layout with good defaults
 
## Quick start

Prerequisite

- Maven is working. That's all. No pom.xml required. Plugin is distributed via [Maven Central](https://repo1.maven.org/maven2/com/baloise/maven/orchestra-maven-plugin/).

... to be completed

## Make your life easier with plugin groups

Add the following to your *~/.m2/settings.xml*

```
<pluginGroups>
    <pluginGroup>com.baloise.maven</pluginGroup>
</pluginGroups>
```

now you can use something like

`> mvn orchestra:deploy -DpscFile=path\to\scenario -Dserver=https://orchestra.mycorp.com:9443`

[(tell me more)](http://maven.apache.org/guides/introduction/introduction-to-plugin-prefix-mapping.html#Configuring_Maven_to_Search_for_Plugins)

## Goals

currently the following use cases are supported

- [package](scenario-package-mojo.html) a scenario source folder into a PSC file
- [(re)deploy](scenario-deploy-mojo.html) a PSC file and associated landscapes to a server


## I don't want to use maven

You can just reuse the soap client and helper methods in a small library without dependencies on maven, i.e

https://repo1.maven.org/maven2/com/baloise/maven/orchestra-maven-plugin/0.7.3/orchestra-maven-plugin-0.7.3-lib.jar

If you are looking for other files, feel free to have a look at the maven [release](https://repo1.maven.org/maven2/com/baloise/maven/orchestra-maven-plugin/) and [snapshot](https://oss.sonatype.org/content/repositories/snapshots/com/baloise/maven/orchestra-maven-plugin/) repositories and cherry pick 🍒.

![Continuous Release](https://github.com/baloise/orchestra-maven-plugin/workflows/CR/badge.svg)
