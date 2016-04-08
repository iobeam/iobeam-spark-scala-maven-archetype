package ${package}.examples

import com.iobeam.spark.streams.annotation.SparkRun
import com.iobeam.spark.streams.filters.Ewma
import com.iobeam.spark.streams.model.OutputStreams
import com.iobeam.spark.streams.triggers.{DeviceTimeoutTrigger, ThresholdTimeoutTrigger, ThresholdTrigger}
import com.iobeam.spark.streams.{AppContext, DeviceOps, DeviceOpsConfigBuilder, SparkApp}
import org.apache.spark.streaming.{Milliseconds, Minutes, Seconds}

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
            .addSeriesTrigger("cpu_smoothed", new ThresholdTrigger(90.0, "cpu_over_90", 70,
            "cpu_below_70"))
            // Add a threshold timeout trigger
            .addSeriesTrigger("cpu_smoothed",
            new ThresholdTimeoutTrigger(85.0, 70.0, Seconds(5), "cpu_over_85_for_5_s",
                "cpu_restored"))
            // Add check of when devices go offline
            .addDeviceTrigger(new DeviceTimeoutTrigger(Minutes(5), "device_offline"))
            .build

        val (outStream, triggers) = DeviceOps.getDeviceOpsOutput(stream, config)

        // Output streams
        OutputStreams(outStream, triggers)
    }
}
