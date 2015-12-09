package org.apache.spark

import com.iobeam.spark.streams.StreamProcessor
import com.iobeam.spark.streams.config.DeviceConfig
import com.iobeam.spark.streams.model.DataSet
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.ClockWrapper
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfter, FlatSpec, GivenWhenThen, Matchers}
import spark.streams.testutils.SparkStreamingSpec

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Example test code for an iobeam spark streaming app
  */
class TestDataSet(time: Long, val value: Int) extends DataSet(time, Map("value" -> value)) {
    def getValue: Int = value
}

class StreamProcessorTest extends FlatSpec with SparkStreamingSpec with Matchers with
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

    // default timeout for eventually trait
    implicit override val patienceConfig =
        PatienceConfig(timeout = scaled(Span(1500, Millis)))

    "An output DStream" should "have the values increased by one" in {

        Given("streaming context is initialized")
        val batchQueue = mutable.Queue[RDD[DataSet]]()
        val results = ListBuffer.empty[List[(Long, Int)]]

        // Create the QueueInputDStream and use it do some processing
        val inputStream = ssc.queueStream(batchQueue)

        // The deviceId is not use in this example and neither is device config
        val deviceDatasetConf = inputStream.map(a => ("TestDevice", (a, new DeviceConfig(""))))

        // Setup the processing app, the projectId is not relevant
        val app = new StreamProcessor(1L)

        // Get the output from the app
        val outputStreams = app.processStream(deviceDatasetConf)
        val firstOutput = outputStreams.getTimeSeries.head

        // Catch the resulting RDDs and convert it to tuples for easy comparisons
        firstOutput.dstream.foreachRDD {
            rdd => results.append(
                rdd.map(a => (a._2._1.time, a._2._1.getData("value").asInstanceOf[Int]))
                    .collect().toList)
        }

        val clock = new ClockWrapper(ssc)

        ssc.start()

        for ((batch, i) <- batches.zipWithIndex) {
            clock.advance(1000)
            batchQueue += ssc.sparkContext.makeRDD(batch)
            eventually {
                results.length should equal(i + 1)
            }

            results.last should equal(correctOutput(i))
        }

        println(results)
        ssc.stop()
    }
}

