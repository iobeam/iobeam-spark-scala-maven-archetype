# Introduction
iobeam supports [Apache Spark](http://spark.apache.org/) apps
for data analysis. Writing and deploying iobeam Spark apps allows users to
process their data in a variety of ways, from simple rule-based models to more
complex prediction, anomaly detection, classification, and other ML-driven
models. The output of an iobeam Spark app can be a new stream of derived data,
or an event to trigger an action using our Trigger service.

This repo describes how to write an iobeam Spark app. [The full reference API
can be found
here](http://assets.iobeam.com/libs/spark/scala/#com.iobeam.spark.streams.package).

# Writing your first iobeam Spark app
The first step to develop a custom iobeam
Spark app is to set up up a Scala app with the right Spark and iobeam
dependencies. To automate this, iobeam provides a [Maven
archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html)
that gives you framework code and dependencies such as iobeam libraries and a
unit test framework.

## Setup

### Prerequisites
* A recent [Maven installation](https://maven.apache.org/download.cgi#Installation)
* Scala 2.10
* JDK 8 

### Creating an App

To set up the iobeam Spark app, you run the following command from your command-line:

```
mvn archetype:generate \
-DarchetypeArtifactId=iobeam-spark-scala-maven-archetype \
-DarchetypeGroupId=com.iobeam \
-DarchetypeCatalog=https://oss.sonatype.org/content/repositories/releases/archetype-catalog.xml
```
Maven will ask for information unique to your app. At the minimum, you should set `groupId`,
`artifactId`, and `appName`.

```
Define value for property 'groupId': : com.mycompany
Define value for property 'artifactId': : myappId
Define value for property 'version':  1.0-SNAPSHOT: : [Enter for default] 
Define value for property 'package':  com.mycompany: : [Enter for default] 
Define value for property 'appName': : MyAppName
Confirm properties configuration:
groupId: com.mycompany
artifactId: myappId
version: 1.0-SNAPSHOT
package: com.mycompany
 Y: : [Enter]
```

When those properties are set, maven will create an app directory in the current directory. In this case, a directory named `./myappId/`. 

To test that the app works, try to build the example app.

```
cd myappId/
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
target/myappId-1.0-SNAPSHOT.jar
```

## Writing your app
The first files to modify are the main analysis class and the corresponding unit tests.
```
src/main/scala/com/mycompany/Spark/streams/StreamProcessor.scala
src/test/scala/com/mycompany/Spark/streams/StreamProcessorTest.scala
```

### Analysis code
The analysis of the streaming data is defined in ```StreamProcessor.scala```. The main
function is the `processStream` method, which you will need to override. 

The example code provided simply adds 1 to all values coming through the stream processing,
creating a new output stream. (For more complex examples, see the [iobeam examples repo](https://github.com/iobeam/iobeam-spark-scala-examples).)

```
class StreamProcessor() extends SparkApp("MyAppName") {

    def add1(timeRecord: TimeRecord): TimeRecord = {
        val newValue = timeRecord.requireDouble("value") + 1
        val outputData = new TimeRecord(timeRecord.time, Map("value" -> newValue))

        outputData
    }

    override def processStream(iobeamInterface: IobeamInterface):
    OutputStreams = {
        val stream = iobeamInterface.getInputStreamBySource
        val outStream = stream.mapValues(add1)

        new OutputStreams(new TimeSeriesStreamPartitioned(outStream))
    }
}

```

### Test code
To help development, the Scala test framework is included in the
app together with Spark and Spark streaming specs.

Modify
```src/test/scala/com/mycompany/Spark/streams/StreamProcessorTest.scala``` to
match your new analysis code. The TestDataSet is used for easy initialization
of test data and can be extended to match the data format of the iobeam app.

To use the test framework with new analysis code, the ```batches``` list needs
to be initialised with new input batches and the ```correctOutput```list needs
to contain the expected output.

```
class StreamProcessorTest extends FlatSpec with SparkStreamingSpec with Matchers with
BeforeAndAfter with GivenWhenThen with Eventually {

    val batches = List(
        List(
            new TestDataSet(1, 1)
        ),
        List(
            new TestDataSet(5, 1),
            new TestDataSet(10, 7)
        ),
        List(
            new TestDataSet(11, 7),
            new TestDataSet(12, 7)
        )
    )

    val correctOutput = List(
        List(
            (1,2)
        ),
        List(
            (5,2),
            (10,8)
        ),
        List(
            (11,8),
            (12,8)
        )
    )
...
```

# Support
Questions? Please reach out to us at [support@iobeam.com](mailto:support@iobeam.com).
