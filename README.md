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

This same repo and maven archetype can be used for either, and more
information about these two scenarios can be found at the following:

* [Full reference API for iobeam's Spark interface](http://assets.iobeam.com/libs/spark/scala/index.html#com.iobeam.spark.streams.package)
* [iobeam's native support for DeviceOps](DeviceOps.md)

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
Define value for property 'artifactId': : myappId
Define value for property 'version':  1.0-SNAPSHOT: : [Enter for default] 
Define value for property 'package':  com.mycompany: : [Enter for default] 
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
The first file to modify is the main analysis class, and if you choose to use it,
the class containing the corresponding unit tests. Modify the pom.xml to activate tests.
```
src/main/scala/com/mycompany/apps/MyApp.scala
src/test/scala/com/mycompany/apps/AppsTest.scala
```

There are three simple example applications under an examples
subdirectory (`src/main/scala/com/mycompany/apps/examples/`):

* **AddOneApp** simply adds 1 to each value of a stream, creating a
    new output stream of these modifies values. Generates an alert if
    the result in above some threshold.

* **TemperatureConversionApp** converts a stream of Celsius readings
    to a stream of Fahrenheit temperatures. Generates an alert if the
    temperature is too high.

* **DeviceOpsApp** provides the stream processing basis for rich [device
    ops](DeviceOps.md) functionality, including alerting.

### Analysis code
The analysis of the streaming data is defined in ```MyApp.scala```. The main
function is the `main` method, which you will need to override. 

For example, the example **AddOneApp* is defined as the following:

```
@SparkRun("AddOne")
object AddOne extends SparkApp {
    /**
      * Simple example of processing function. Adds 1 to the field "value" and
      * writes it to the value-new series.
      */
    def add1(timeRecord: TimeRecord): TimeRecord = {

        val newValue = timeRecord.requireDouble("value") + 1

        // Create output series, make sure it uses a new series name
        new TimeRecord(timeRecord.time, Map("value-new" -> newValue))
    }

    /**
      * Simple trigger function. Returning empty Seq means no triggers. If more
      * than one field cause triggers, the Seq can contain multiple triggers.
      *
      * @param deviceAndRecord record to check
      * @return Seq of trigger events
      */
    def checkTrigger(deviceAndRecord: (String, TimeRecord)): Seq[TriggerEvent] = {
        val (deviceId, timeRecord) = deviceAndRecord
        val myThreshold = 5.0
        val value = timeRecord.requireDouble("value")

        if (value > myThreshold) {
            return Seq(new TriggerEvent("myEventName",
                new TimeRecord(timeRecord.time,
                               Map("triggeredValue" -> value, "deviceId" -> deviceId))))
        }

        // Not a trigger in this record
        Seq()
    }

    /**
      * Sets up stream processing for project.
      *
      * @param appContext interface to iobeam backend
      * @return Set of outputStreams
      */
    override def main(appContext: AppContext):
    OutputStreams = {
        val stream = appContext.getInputStream
        val outStream = stream.mapValues(add1)
        val triggerStream = stream.flatMap(checkTrigger)

        OutputStreams(outStream, triggerStream)
    }
}

```

The **DeviceOpsApp** makes heavy use of [iobeam's native support for DeviceOps](DeviceOps.md), including series filtering and triggers.

```
@SparkRun("deviceOps")
object DeviceOpsApp extends SparkApp {

    override def main(appContext: AppContext):
    OutputStreams = {
        // Get raw input data
        val stream = appContext.getInputStream

        // Build device ops config
        val config = new DeviceOpsConfigBuilder()
            // smooth the input series cpu with a EWMA filter with alpha 0.2
            .addSeriesFilter("cpu", "cpu_smoothed", new Ewma(0.2))
            // Add a threshold trigger to the series
            .addSeriesTrigger("cpu_smoothed", new ThresholdTrigger(90.0, "cpu_over_90", 70,
            "cpu_below_70"))
            // Add a threshold timeout trigger
            .addSeriesTrigger("cpu_smoothed",
            new ThresholdTimeoutTrigger(85.0, 70.0, Milliseconds(5000), "cpu_over_85_for_5_s",
                "cpu_restored"))
            // Add check of when devices go offline
            .addDeviceTrigger(new DeviceTimeoutTrigger(Minutes(5), "device offline"))
            .build

        val (outStream, triggers) = DeviceOps.getDeviceOpsOutput(stream, config)

        // Output streams
        OutputStreams(outStream, triggers)
    }
}
```

### Test code
To help development, the Scala test framework is included in the
app together with Spark and Spark streaming specs.

Modify
```src/test/scala/com/mycompany/Spark/streams/AppsTest.scala``` to
match your new analysis code. The TestDataSet is used for easy initialization
of test data and can be extended to match the data format of the iobeam app.

To use the test framework with new analysis code, the ```batches``` list needs
to be initialised with new input batches and the ```correctOutput```list needs
to contain the expected output.

```
class AddOneTest extends FlatSpec with SparkStreamingSpec with Matchers with
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

## Run code on iobeam

To run the addOne example app
```
iobeam app create -name addOne -path target/myjarfile.jar 
```

To run the Device Ops app
```
iobeam app create -name deviceOps -path target/myjarfile.jar 

```
(Note: Both apps will be contained in the same jar file.)
# Support
Questions? Please reach out to us at [support@iobeam.com](mailto:support@iobeam.com).
