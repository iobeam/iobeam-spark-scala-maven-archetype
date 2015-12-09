package com.iobeam.spark.streams

import com.iobeam.spark.streams.config.{DeviceConfig, SeriesConfig}
import com.iobeam.spark.streams.model.{TimeSeriesStream, DataSet, OutputStreams}
import org.apache.spark.streaming.dstream.DStream

/**
  * Application to submit to iobeam
  *
  */

class StreamProcessor(projectId: Long) extends SparkApp("MyAppName", projectId) {

    def add1(dataAndConf: (DataSet, DeviceConfig)): (DataSet, DeviceConfig) = {

        val (dataSet, conf) = dataAndConf
        val newValue = dataSet.getData("value").asInstanceOf[Int] + 1
        val outputData = new DataSet(dataSet.time, Map("value" -> newValue))

        (outputData, conf)
    }

    override def processStream(stream: DStream[(String, (DataSet, DeviceConfig))]):
    OutputStreams = {

        val outStream = stream.mapValues(add1)

        new OutputStreams(Seq(new TimeSeriesStream("out_stream", outStream)), Seq())
    }

    override def getConfigs: Seq[SeriesConfig] = {
        Seq()
    }

    override def usedTimeSeriesNames: Seq[DataSet.DataKey] = Seq("value")
}
