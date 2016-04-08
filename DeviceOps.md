# Device Ops Application

iobeam provides library support for common operations associated with
DeviceOps, including stateful series processing with filtering and
triggers.

The input to a DeviceOps application is a stream of measurements
(e.g., cpu, battery levels, memory consumption, etc.), while the
output will be a derived stream (the result of processing and
filtering the incoming measurement stream) and a stream of [events to
be triggered](https://docs.iobeam.com/triggering-on-data).

This library provides a *DeviceOpsConfigBuilder* that allows one to
perform simple stream processing in just a few lines of configuration,
including common stateful transformations (e.g., computing
exponentially-weighted moving averages, derivations, etc.), filters
(e.g., simple thresholds, time-based stateful conditions, etc.)  and
event generation.

By now, you should have first followed the [setup and installation](README.md#setup) instructions for generating an iobeam Spark app. Once you've done that, you can modify and/or deploy the DeviceOps app.

## Understanding the Framework

The core functionality of a device ops application can be realized using iobeam's ```DeviceOpsConfigBuilder```, which allows one to chain a series of transformations and filters to generate out derived data streams and trigger events.

The below example first takes an input stream that includes a time series of raw `cpu` readings, then smoothes the series using an exponentially-weighted moving average.  It generates various trigger events named:

* *cpu_over_90* when the smoothed series goes above 90%
* *cpu_below_70* when a smoothed series that *had* been above 90% then drops below 70%
* *cpu_over_85_for_5_s* when the smoothed series remains over 85% for more than 5 seconds
* *cpu_restored* when the series that had been above 85% for 5 seconds drops below 70%
* *device_offline* when no data is received from the device in a 5 minute period

```
/**
  * Builds a Device Ops app using iobeam library.
  */
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
            .addSeriesTrigger("cpu_smoothed",
                new ThresholdTrigger(90.0, "cpu_over_90",
                                     70.0, "cpu_below_70"))

            // Add a threshold timeout trigger
            .addSeriesTrigger("cpu_smoothed",
                new ThresholdTimeoutTrigger(85.0, 70.0, Seconds(5),
                  "cpu_over_85_for_5_s", "cpu_restored"))

            // Add check of when devices go offline
            .addDeviceTrigger(new DeviceTimeoutTrigger(Minutes(5), "device_offline"))
            .build

        val (outStream, triggers) = DeviceOps.getDeviceOpsOutput(stream, config)

        // Output streams
        OutputStreams(outStream, triggers)
    }
}
```

## Building, Deploying, and Testing your App

To build the device ops app, just run `mvn package` from your top-level directory, as detailed [in the README](README.md#building-an-app).  This will create a JAR file that can be uploaded to iobeam.

To upload and run the device ops app, using our [CLI](https://github.com/iobeam/iobeam).

```
iobeam app create -name deviceOps -path target/myApp-1.0-SNAPSHOT.jar

New app created.
App ID: 878
App Name: deviceOps
```

### Registering triggers

Now that your app is running on iobeam, you can register triggers so you can get notified when some event occurs.  See our [documentation](https://docs.iobeam.com/triggering-on-data) for more detailed info, but to send an email to yourself when the device's CPU is above a certain threshold, use the following command:

```
iobeam trigger create email -name "cpu_over_90" \
  -payload "The CPU of device {{ .deviceId }} is above 90%" \
  -subject "High CPU" -to <your email address>
```

### Sending test data

Then, to test that it works, send some test data:

```
iobeam device create -id DeviceOps-Test-1
for v in 85 95 99 99 50 ; do
   iobeam import -deviceId DeviceOps-Test-1 -series cpu -value $v ;
done
```

In fact, you can even query the system to determine when some condition occurs:

```
iobeam query -deviceId DeviceOps-Test-1 -series cpu_over_90

{
  "result": [
    {
      "data": [
        {
          "time": 1460137113326,
          "value": 1.0
        }
      ],
      "device_id": "DeviceOps-Test-1",
      "name": "cpu_over_90",
      "project_id": 49
    }
  ],
  "timefmt": "msec"
}
```

Because the stock example generates an event whenever the device is offline
for more than 5 minutes (which can also be tied to a trigger), you should
stop your app when you are done testing it:

```
iobeam app stop -id 878

Requested status: STOPPED. Waiting for current status to change.
Checking app status...RUNNING
Checking app status...STOPPED
Success!
```

If you don't remember your app id, just run:

```
iobeam app list

App ID  : 878
App Name: deviceOps
Created : 2016-04-08T14:37:15Z
Requested Status: RUNNING
Current Status  : RUNNING

BUNDLE INFO
URI     : file://telemetryApp-1.0-SNAPSHOT.jar
Type    : JAR
Checksum: 2889f37315fcfbd8ba5e780e2d9097fa48e64162a1f0c924f499516548598976 (SHA256)
```

## Modifying the App

Now that we've deployed the stock device ops app, you can now extend the
program according to your needs.  Either edit the example in-place, or
copy the *main* function to `MyApp.scala`.

To redeploy your app, just run the update command via the CLI.  Include the `-name` parameter if you modified `MyApp.scala`.

```
iobeam app update -id 878 -name myApp -path target/myApp-1.0-SNAPSHOT.jar
```

There are two main ways to process data streams using the device ops library:

* **Filters**: Filters work on individual series and outputs a new value on
each sample. They can be attached to the output from other filters but will
then operate on the data from the previous batch.

* **Triggers**: Triggers is evaluated on each sample and outputs a trigger name
or None. Triggers can be attached to derived series.

## Examples

### Low battery level

A common use of the trigger system is to generate events when a certain metric passes a threshold.
For example when the battery level of a device is below a defined level. To setup a trigger for when
the series named "battery" is below 10 %, you setup a ThresholdTrigger:

```scala
new DeviceOpsConfigBuilder()
  .addTrigger("battery", ThresholdTrigger(10.0, "Battery below 10%", 15.0))
  .build
```
where `10.0` is the threshold level (or "low-watermark") and `15.0` is the trigger release level (or "high watermark").
This hysteresis means that the trigger will not create multiple events if the battery readings oscillate around the
trigger level.

### Monitor when CPU is above threshold and when it goes below another threshold
0
When monitoring a metric where you are interested both when it enters and leaves a problematic area,
such as high CPU, you configure the threshold trigger:
```scala
new DeviceOpsConfigBuilder()
  .addTrigger("cpu", ThresholdTrigger(90.0, "CPU above 90%", 70.0, "CPU below 70%"))
  .build
```

This will create a trigger event when the CPU readings go above `90%` and another event when the series
go below `70%`.

### High CPU load for extended period

To set a threshold trigger that detects when a series (e.g. `cpu`) is above a threshold longer than a time limit:
```scala
new DeviceOpsConfigBuilder()
  .addTrigger("cpu", ThresholdTimeoutTrigger(70.0, 50.0, Seconds(30), "Above threshold >30s"))
  .build
```

This will create a trigger when CPU readings remove above `70%` without dipping below `50%` for over 30secs.

### Detect quickly changing series

Detecting quick changes in series can be done by connecting a threshold trigger to a derivative filter.
```scala
new DeviceOpsConfigBuilder()
  .addFilter("cpu", "cpu_derived", Filter.DeriveFilter)
  .addTrigger("cpu_derived", ThresholdTrigger(1.0, "CPU increase high", 0.0, "CPU leveled out"))
  .build
```

As noise on a series can make the derivative very jumpy, a smoothing filter can be applied before
```scala
new DeviceOpsConfigBuilder()
  .addFilter("noisy_series", "smoothed_series", new Ewma(0.1)))
  .addFilter("smoothed_series", "series_derived", new DeriveFilter)
  .addTrigger("series_derived", new ThresholdTrigger(1.0, "Series increase high", 0.0, "Series leveled out"))
  .build
```

For irregularly sampled series, `EwmaIrregular` can be used instead.  

### Device offline

Detecting when a device has gone offline, i.e., last sent data to iobeam longer than a specific timeout threshold.
```scala
new DeviceOpsConfigBuilder()
  .addDeviceTrigger(new DeviceTimeoutTrigger(Minutes(5), "device_offline"))
  .build
```

# Support
Questions? Please reach out to us at [support@iobeam.com](mailto:support@iobeam.com).
