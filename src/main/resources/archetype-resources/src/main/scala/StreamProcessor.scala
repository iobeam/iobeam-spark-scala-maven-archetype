package ${package}

import com.iobeam.spark.streams.{IobeamInterface, SparkApp}
import com.iobeam.spark.streams.model.{TimeSeriesStreamPartitioned, OutputStreams, TimeRecord}
import com.iobeam.spark.streams.annotation.SparkRun

/**
  * Application to submit to iobeam
  */


@SparkRun("${appName}")
class StreamProcessor() extends SparkApp("${appName}") {

    /**
      * Simple example of processing function. Adds 1 to the field "value" in
      * each timerecord.
      *
      * @param timeRecord
      * @return
      */
    def add1(timeRecord: TimeRecord): TimeRecord = {
        val newValue = timeRecord.requireDouble("value") + 1
        new TimeRecord(timeRecord.time, Map("value" -> newValue))
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

        new OutputStreams(new TimeSeriesStreamPartitioned(outStream))
    }
}
