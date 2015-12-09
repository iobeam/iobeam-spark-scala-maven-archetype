# Maven Archetype for iobeam spark app

This archetype is used to bootstrap a spark app project

## Prerequisites

* Maven
* [ Maven settings file](https://bitbucket.org/440-labs/maven-settings) (or alternatively clone this repo and do ```mvn install```)

## Creating a Project

Simply issue the following command

    mv2n archetype:generate -DarchetypeArtifactId=spark-app-maven-archetype -DarchetypeGroupId=com.iobeam

then follow the instructions for giving the new project a groupId (basically package name) and artifactId (project name).

