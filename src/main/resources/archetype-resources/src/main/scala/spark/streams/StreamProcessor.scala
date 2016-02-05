package com.iobeam.spark.streams

import com.iobeam.spark.streams.config.DeviceConfig
import com.iobeam.spark.streams.model.{TimeSeriesStreamPartitioned, OutputStreams, TimeRecord}
import org.apache.spark.streaming.dstream.DStream

/**
  * Application to submit to iobeam
  *
  */

class StreamProcessor() extends SparkApp("${appName}") {

    def add1(dataAndConf: (TimeRecord, DeviceConfig)): TimeRecord = {
        val (timeRecord, _) = dataAndConf
        val newValue = timeRecord.requireDouble("value") + 1
        val outputData = new TimeRecord(timeRecord.time, Map("value" -> newValue))

        outputData
    }

    override def processStream(stream: DStream[(String, (TimeRecord, DeviceConfig))]):
    OutputStreams = {

        val outStream = stream.mapValues(add1)

        new OutputStreams(new TimeSeriesStreamPartitioned("out_stream", outStream))
    }
}
