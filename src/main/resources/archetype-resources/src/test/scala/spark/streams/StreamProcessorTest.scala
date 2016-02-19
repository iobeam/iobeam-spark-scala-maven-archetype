package org.apache.spark

import com.iobeam.spark.streams.{AppRunnerInterface, StreamProcessor}
import com.iobeam.spark.streams.model.TimeRecord
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.ClockWrapper
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}
import spark.streams.testutils.SparkStreamingSpec

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
/**
  * Example test code for an iobeam spark streaming app
  */
class TestTimeRecord(override val time: Long,
                     val value: Double) extends TimeRecord(time, Map("value" -> value)) {
    def getValue: Double = value
}

class StreamProcessorTest extends FlatSpec with Matchers with SparkStreamingSpec with GivenWhenThen with Eventually {

    val batches = List(
        List(
            new TestTimeRecord(1, 1)
        ),
        List(
            new TestTimeRecord(5, 1),
            new TestTimeRecord(10, 7)
        ),
        List(
            new TestTimeRecord(11, 7),
            new TestTimeRecord(12, 7)
        )
    )

    val correctOutput = List(
        List(
            (1, 2)
        ),
        List(
            (5, 2),
            (10, 8)
        ),
        List(
            (11, 8),
            (12, 8)
        )
    )

    // default timeout for eventually trait
    implicit override val patienceConfig =
        PatienceConfig(timeout = scaled(Span(1500, Millis)))

    "An output DStream" should "have the values increased by one" in {

        Given("streaming context is initialized")
        val batchQueue = mutable.Queue[RDD[TimeRecord]]()
        val results = ListBuffer.empty[List[(Long, Double)]]

        // Create the QueueInputDStream and use it do some processing
        val inputStream = ssc.queueStream(batchQueue)

        // The deviceId is not use in this example
        val deviceTimeRecord = inputStream.map(a => ("TestDevice", a))

        // Get an iobeam interface for local use
        val interface = new AppRunnerInterface(deviceTimeRecord)

        // Setup the processing app
        val app = new StreamProcessor()

        // Get the output from the app
        val outputStreams = app.processStream(interface)
        val firstOutput = outputStreams.getTimeSeries.head

        // Catch the resulting RDDs and convert it to tuples for easy comparisons
        firstOutput.getDStream.foreachRDD {
            rdd => results.append(
                rdd.map(a => (a._2.time, a._2.requireDouble("value")))
                    .collect().toList)
        }

        val clock = new ClockWrapper(ssc)

        ssc.start()

        for ((batch, i) <- batches.zipWithIndex) {
            batchQueue += ssc.sparkContext.makeRDD(batch)
            clock.advance(1000)
            eventually {
                results.length should equal(i + 1)
            }

            results.last should equal(correctOutput(i))
        }

        println(results)
        ssc.stop()
    }
}

