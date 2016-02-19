package com.iobeam.spark.streams

import com.iobeam.spark.streams.model.{TimeSeriesStreamPartitioned, OutputStreams, TimeRecord}

/**
  * Application to submit to iobeam
  *
  */

class StreamProcessor() extends SparkApp("${appName}") {

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
