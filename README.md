# Writing your first iobeam spark app
The first step to develop a custom iobeam spark app is to set up up a Scala project with the right spark and iobeam dependencies. To automate this, iobeam provides a [Maven archetype](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html) that gives you framework code and dependencies such as iobeam libraries and unit test framework. 
### Prerequisites
* A recent [Maven installation](https://maven.apache.org/download.cgi#Installation)
* Scala 2.10
* JDK 1.8 

### Creating a Project

To set up the iobeam spark app project, you simply issue the following command

```
    mvn archetype:generate -DarchetypeArtifactId=spark-app-maven-archetype -DarchetypeGroupId=com.iobeam
```
Maven will ask about information that is unique to the project and you should at least set groupId and artifactId.

```
Define value for property 'groupId': : com.mycompany
Define value for property 'artifactId': : myappId
Define value for property 'version':  1.0-SNAPSHOT: : [Enter for default] 
Define value for property 'package':  com.mycompany: : [Enter for default] 
Define value for property 'appName': : MyAppName
Confirm properties configuration:
groupId: com.mycompany
artifactId: myappId
version: 1.0-SNAPSHOT
package: com.mycompany
 Y: : [Enter]
```

When the properties are set, maven will create a project directory in the current directory. In this case, a directory named 

```
./myappId/
```

To test that the project works, try to build the example app.

```
cd myappId/
mvn package

```

If the build is successful, the end of the maven output will look similar to 
```
...
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 25.664 s
[INFO] Finished at: 2015-12-22T14:00:52+01:00
[INFO] Final Memory: 42M/1211M
[INFO] ------------------------------------------------------------------------

```
The ```mvn package``` command will build a JAR file that can be uploaded to iobeam. In this example, the JAR file is 

```
target/myappId-1.0-SNAPSHOT.jar
```

## Writing your app
The first files to modify are the main stream processing class and the corresponding unit tests.
```
src/main/scala/com/mycompany/spark/streams/StreamProcessor.scala
src/test/scala/com/mycompany/spark/streams/StreamProcessorTest.scala
```

The analysis of the streaming data is defined in  ```StreamProcessor.scala```. The example code provided simply adds 1 to all values coming through the stream processing. For more useful examples, see the [iobeam examples repo](LINKTOEXAMPLES).

```
class StreamProcessor() extends SparkApp("MyAppName") {

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

```

### Test code
To help development, the Scala test framework is included in the project together with Spark and Spark streaming specs. Modify  ```src/test/scala/com/mycompany/spark/streams/StreamProcessorTest.scala```  to match your new analysis code. The TestDataSet is used for easy initialisation of test data and can be extended to match the data format of the iobeam project. To use the test frame work with new analysis code the ```batches``` list needs to be initialised with new input batches and the ```correctOutput```  list needs to contain the expected output.

```
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
...
```