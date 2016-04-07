# Iobeam Device Ops spark app

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

To write a DeviceOps app using iobeam:

* First, create a iobeam app [using these instructions](README.md).
* Second, copy the *main* function from the DeviceOps example from `examples/DeviceOpsApp.scala` to [your application](README.md#writing-your-app).
* Third, modify and extend your app's functionality using the below instructions.

### Filters

Filters work on individual series and outputs a new value on each sample. They can be attached to the
output from other filters but will then operate on the data from the previous batch.

### Triggers

Triggers is evaluated on each sample and outputs a trigger name or None. Triggers can be 
attached to derived series.

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
  .addDeviceTrigger(new DeviceTimeoutTrigger(Minutes(5), "device offline"))
  .build
```
