package ${package}.streams

import com.iobeam.spark.streams.config.{DeviceConfig, SeriesConfig}
import com.iobeam.spark.streams.model.{TimeSeriesStreamPartitioned, DataSet, OutputStreams}
import org.apache.spark.streaming.dstream.DStream

/**
  * Application to submit to iobeam
  *
  */

class StreamProcessor() extends SparkApp(${appName}) {

    def add1(dataAndConf: (DataSet, DeviceConfig)): DataSet = {
        val (dataSet, conf) = dataAndConf
        val newValue = dataSet.requireDouble("value") + 1
        val outputData = new DataSet(dataSet.time, Map("value" -> newValue))

        outputData
    }

    override def processStream(stream: DStream[(String, (DataSet, DeviceConfig))]):
    OutputStreams = {

        val outStream = stream.mapValues(add1)

        new OutputStreams(new TimeSeriesStreamPartitioned("out_stream", outStream))
    }
}
