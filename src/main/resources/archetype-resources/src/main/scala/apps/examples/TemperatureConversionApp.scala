package ${package}.examples

import com.iobeam.spark.streams.annotation.SparkRun
import com.iobeam.spark.streams.model.{OutputStreams, TimeRecord, TriggerEvent}
import com.iobeam.spark.streams.{AppContext, SparkApp}

/**
  * Converts Celsius to Fahrenheit, writes new stream
  * and generates trigger if temperature too high.
  */
@SparkRun("convertCelsius")
object TemperatureConversionApp extends SparkApp {

    /**
      * Simple example of a processing function. Converts the temperature from Celsius
      * to Fahrenheit and writes the value to a new series.
      */
    def convertCelsiusToFahrenheit(timeRecord: TimeRecord): TimeRecord = {
        val temp_C = timeRecord.requireDouble("temp_C")
        val temp_F = (9.0 / 5 * temp_C) + 32

        // Create output series, make sure it uses a new series name
        new TimeRecord(timeRecord.time, Map("temp_F" -> temp_F))
    }

    /**
      * Simple trigger function. Returning empty Seq means no triggers. If more
      * than one field cause triggers, the Seq can contain multiple triggers.
      *
      * @param deviceAndRecord record to check
      * @return Seq of trigger events
      */
    def checkHighTemp(deviceAndRecord: (String, TimeRecord)):
    Seq[TriggerEvent] = {
        val (deviceId, timeRecord) = deviceAndRecord
        val temp_F = timeRecord.requireDouble("temp_F")
        val myThreshold = 68.0 // 68 Fahrenheit = 20 Celsius

        if (temp_F > myThreshold) {
            // Add trigger event "highTemp"
            return Seq(new TriggerEvent("highTemp",
                new TimeRecord(timeRecord.time, Map("temp_F" -> temp_F, "deviceId" -> deviceId))))
        }

        // Not a trigger in this record
        Seq()
    }

    override def main(appContext: AppContext):
    OutputStreams = {
        // Get raw input data
        val stream = appContext.getInputStream

        // Transform data
        val outStream = stream.mapValues(convertCelsiusToFahrenheit)

        // Generate triggers
        val triggers = outStream.flatMap(checkHighTemp)

        // Output streams
        OutputStreams(outStream, triggers)
    }
}
