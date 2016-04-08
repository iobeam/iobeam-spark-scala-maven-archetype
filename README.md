# Introduction
iobeam supports [Apache Spark](http://spark.apache.org/) apps
for data analysis. Writing and deploying iobeam Spark apps allows users to
process their data in a variety of ways, from simple rule-based models to more
complex prediction, anomaly detection, classification, and other ML-driven
models. The output of an iobeam Spark app can be a new stream of derived data,
or an event to trigger an action using our Trigger service.

This repo describes how to write an iobeam Spark app.  Some developers
might be using iobeam for their entire data pipeline, while others'
first introduction to iobeam will be for "device ops" and telemetry.


# Writing your first iobeam Spark app

The first step to develop a custom iobeam
Spark app is to set up up a Scala app with the right Spark and iobeam
dependencies. To automate this, iobeam provides a [Maven
archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html)
that gives you framework code and dependencies such as iobeam libraries and a
unit test framework.

## Setup

### Prerequisites
* A recent [Maven installation](https://maven.apache.org/download.cgi#Installation) (3.3+)
* Scala 2.10
* JDK 8

### Creating an App

To set up the iobeam Spark app, you run the following command from your command-line:

```
mvn archetype:generate \
-DarchetypeArtifactId=iobeam-spark-scala-maven-archetype \
-DarchetypeGroupId=com.iobeam \
-DarchetypeCatalog=https://assets.iobeam.com/libs/archetype-catalog.xml
```
Maven will ask for information unique to your app. At the minimum, you should set `groupId`,
`artifactId`.

```
Define value for property 'groupId': : com.mycompany
Define value for property 'artifactId': myApp
Define value for property 'version':  1.0-SNAPSHOT: : [Enter for default]
Define value for property 'package':  com.mycompany: : [Enter for default]
Confirm properties configuration:
groupId: com.mycompany
artifactId: myApp
version: 1.0-SNAPSHOT
package: com.mycompany
 Y: : [Enter]
```

When those properties are set, maven will create an app directory in the current directory. In this case, a directory named `./myApp/`.

### Building an App

To test that the app works, try to build the example app.

```
cd myApp/
mvn package

```

If the build is successful, the end of the maven output will look similar to
```
...
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 25.664 s
[INFO] Finished at: 2015-12-22T14:00:52+01:00
[INFO] Final Memory: 42M/1211M
[INFO] ------------------------------------------------------------------------

```
The ```mvn package``` command will build a JAR file that can be uploaded to iobeam. In this example, the JAR file is

```
target/myApp-1.0-SNAPSHOT.jar
```

## Next steps

Now, it's time to modify and/or deploy your app on iobeam.

* **Device Ops**.  If you are interested in iobeam's native support for device ops, [follow these device ops instructions](DeviceOps.md)
* **App Development**.  For more general instructions on writing apps on iobeam, [follow these app development instructions](AppDevelopment.md)


# Support
Questions? Please reach out to us at [support@iobeam.com](mailto:support@iobeam.com).
