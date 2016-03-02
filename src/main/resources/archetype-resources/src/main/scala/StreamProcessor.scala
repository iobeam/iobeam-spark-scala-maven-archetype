package ${package}

import com.iobeam.spark.streams.{IobeamInterface, SparkApp}
import com.iobeam.spark.streams.model.{TimeSeriesStreamPartitioned, OutputStreams, TimeRecord, TriggerEvent, TriggerStream}
import com.iobeam.spark.streams.annotation.SparkRun

/**
  * Application to submit to iobeam
  */
class StreamProcessor() extends SparkApp("${appName}") {

    /**
      * Simple example of processing function. Adds 1 to the field "value" and writes it to
      * the value-new series.
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
      * @param timeRecord record to check
      * @return Seq of trigger events
      */
    def checkTrigger(deviceAndRecord: (String, TimeRecord)): Seq[TriggerEvent] = {
        val (deviceId, timeRecord) = deviceAndRecord
        val myThreshold = 5.0
        val value = timeRecord.requireDouble("value")

        if (value > myThreshold) {
            return Seq(new TriggerEvent("myEventName",
                new TimeRecord(timeRecord.time, Map("triggeredValue" -> value, "deviceId" -> deviceId))))
        }

        // Not a trigger in this record
        Seq()
    }

    /**
      * Sets up stream processing for project.
      *
      * @param iobeamInterface interface to iobeam backend
      * @return Set of outputStreams
      */
    override def processStream(iobeamInterface: IobeamInterface):
    OutputStreams = {
        val stream = iobeamInterface.getInputStreamBySource
        val outStream = stream.mapValues(add1)
        val triggerStream = stream.flatMap(checkTrigger)

        new OutputStreams(new TimeSeriesStreamPartitioned(outStream), TriggerStream(triggerStream))
    }
}
