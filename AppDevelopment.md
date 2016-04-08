# Building an Application

iobeam makes it easy to write and deploy [Apache Spark](http://spark.apache.org/) apps
for data analysis.

Before going forward, you should first follow the [setup and installation](README.md#setup) instructions for generating an iobeam Spark app. Once you've done that, you can begin writing your application.

There are three simple example applications provided (`src/main/scala/com/mycompany/apps/examples/`):

* **AddOneApp** simply adds 1 to each value of a stream, creating a
    new output stream of these modifies values. Generates an alert if
    the result in above some threshold.

* **TemperatureConversionApp** converts a stream of Celsius readings
    to a stream of Fahrenheit temperatures. Generates an alert if the
    temperature is too high.

* **DeviceOpsApp** provides the stream processing basis for rich [device
    ops](DeviceOps.md) functionality, including alerting.

This README discusses the first two.

## Writing your App

A skeleton file for performing streaming data analysis is found in ```MyApp.scala```. The main function is the `main` method, which you can modify.  The repository also includes corresponding unit tests (which to activate, you'll need to modify the `pom.xml` file):

```
src/main/scala/com/mycompany/apps/MyApp.scala
src/test/scala/com/mycompany/apps/AppsTest.scala
```

We detail the two examples below, but you can also find a [full reference API for iobeam's Spark interface](http://assets.iobeam.com/libs/spark/scala/index.html#com.iobeam.spark.streams.package).

## Example Applications

### **AddOneApp**

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

### **TemperatureConversionApp**

```
/**
  * Converts Celsius to Fahrenheit, writes new stream
  * and generates trigger if temperature too high.
  */
@SparkRun("convertCelsius")
object TemperatureConversionApp extends SparkApp {

    /**
      * Simple example of a processing function. Converts the temperature from Celsius
      * to Fahrenheit and writes the value to a new series.
      */
    def convertCelsiusToFahrenheit(timeRecord: TimeRecord): TimeRecord = {
        val temp_C = timeRecord.requireDouble("temp_C")
        val temp_F = (9.0 / 5 * temp_C) + 32

        // Create output series, make sure it uses a new series name
        new TimeRecord(timeRecord.time, Map("temp_F" -> temp_F))
    }

    /**
      * Simple trigger function. Returning empty Seq means no triggers. If more
      * than one field cause triggers, the Seq can contain multiple triggers.
      *
      * @param deviceAndRecord record to check
      * @return Seq of trigger events
      */
    def checkHighTemp(deviceAndRecord: (String, TimeRecord)):
    Seq[TriggerEvent] = {
        val (deviceId, timeRecord) = deviceAndRecord
        val temp_F = timeRecord.requireDouble("temp_F")
        val myThreshold = 68.0 // 68 Fahrenheit = 20 Celsius

        if (temp_F > myThreshold) {
            // Add trigger event "highTemp"
            return Seq(new TriggerEvent("highTemp",
                new TimeRecord(timeRecord.time, Map("temp_F" -> temp_F, "deviceId" -> deviceId))))
        }

        // Not a trigger in this record
        Seq()
    }

    override def main(appContext: AppContext):
    OutputStreams = {
        // Get raw input data
        val stream = appContext.getInputStream

        // Transform data
        val outStream = stream.mapValues(convertCelsiusToFahrenheit)

        // Generate triggers
        val triggers = outStream.flatMap(checkHighTemp)

        // Output streams
        OutputStreams(outStream, triggers)
    }
}

```

## Testing your App
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
iobeam app create -name addOne -path target/myappId-1.0-SNAPSHOT.jar
```

To run the Device Ops app
```
iobeam app create -name convertCelsius -path target/myappId-1.0-SNAPSHOT.jar

```
(Note: Both apps will be contained in the same jar file.)

# Support
Questions? Please reach out to us at [support@iobeam.com](mailto:support@iobeam.com).
