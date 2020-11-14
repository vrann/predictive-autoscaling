package io.adobe

import com.cloudera.sparkts.EasyPlot
import com.cloudera.sparkts.models.{ARIMA, ARIMAModel}
import org.apache.spark._
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.{DStream, ReceiverInputDStream}

import scala.concurrent.duration._

object Streaming {

//  var pairs: DStream.empty[(String, Int)]

//  1. https://medium.com/making-sense-of-data/time-series-next-value-prediction-using-regression-over-a-rolling-window-228f0acae363
//  2. https://prometheus.io/blog/

  /**
   * 1. Create dataframe with the timeseries for the last 100 intervals
   * 2. Split on traingin and testing
   * 3. Fit ARIMA model on learning dataset
   * 4. Predict testing and calculate errors:
   * - MAE ( Mean absolute error), Root Mean Square Error (RMSE)
   * - MAPE ( Mean Absolute Percentage Error)
   * - RMSEP ( Root Mean Square Percentage Error)
   * - Almost correct Predictions Error rate (AC_errorRate)
   * 5. Repeat for Linear regression
   * 6. Repeat for fb prophet
   */
  def main(args: Array[String]): Unit = {
//    val dataFile = getClass.getClassLoader.getResourceAsStream("R_ARIMA_DataSet1.csv")
    val dataFile = getClass.getClassLoader.getResourceAsStream("Traffic.csv")
    val rawData = scala.io.Source.fromInputStream(dataFile).getLines().toArray.map(_.toDouble)
    val training = rawData.take((rawData.length * 0.8).toInt)
    val testing = rawData.drop((rawData.length * 0.8).toInt)
//    val trainingData = new DenseVector(training)

    val data = new DenseVector(training)

    //𝑌𝑡=𝑟𝑌(𝑡−1)+𝑒𝑡+𝑎𝑒(𝑡−1)  where 𝑎 is the moving average parameter.
    val model: ARIMAModel = ARIMA.fitModel(1, 0, 1, data)
//    val model = ARIMA.fitModel(0, 1, 1, trainingData)

//    val Array(c, ar, ma) = model.coefficients

//    EasyPlot.ezplot(Seq(testinggData))
    val time: Long = System.nanoTime()

//    val testinggData1 = new DenseVector(testing.take((testing.length * 0.5).toInt))
//    val testinggData2 = new DenseVector(testing.drop((testing.length * 0.5).toInt))
//    val forecast1 = model.forecast(testinggData1, 0)
//    val forecast2 = model.forecast(testinggData2, 0)
//    val forecast5Steps: Array[Double] =
//      testing.foldLeft(Array.empty[Double])(
//        (b, a) => b ++ model.forecast(new DenseVector(Array[Double](a)), 10).toArray)
    //val forecast = predictWindow(model, testing, 1)
    val forecast = model.forecast(data, 50)
    println("Time: " + (System.nanoTime() - time).nanos.toMillis)
//    EasyPlot.ezplot(Seq(new DenseVector(rawData ++ Array.fill(30)(170.0)), forecast))
    testing.foldLeft(training)((b, a) => {
      predictNext(model, b, a, 50)
    })
//    EasyPlot.ezplot(forecast)
//    EasyPlot.ezplot(Seq(testinggData1, forecast1))
//    EasyPlot.ezplot(Seq(testinggData2, forecast2))
//    EasyPlot.ezplot(Seq(new DenseVector(testinggData.values ++ Array.fill(10)(170.0)), forecast))

//    EasyPlot.ezplot(forecast)
//    println(data.size)
//    println(model.forecast(data, 0).size)
//    EasyPlot.acfPlot(rawData, 100)
//    println(ar)
//    println(ma)
  }

  def predictNext(model: ARIMAModel, window: Array[Double], next: Double, predict: Int): Array[Double] = {
    val dataset = window :+ next
    val time: Long = System.nanoTime()
    val forecast = model.forecast(new DenseVector(dataset), predict)
    println("Time: " + (System.nanoTime() - time).nanos.toMicros)
    EasyPlot.ezplot(Seq(new DenseVector(dataset ++ Array.fill(predict)(next)), forecast))
    dataset
  }

  def predictWindow(model: ARIMAModel, window: Array[Double], predictionBase: Int): Array[Double] = {
    if (window.length >= predictionBase) {
      model.forecast(new DenseVector(window.take(predictionBase)), 100).toArray ++ predictWindow(
        model,
        window.drop(predictionBase),
        predictionBase)
    } else if (window.length > 0) {
      model.forecast(new DenseVector(window), 10).toArray
    } else {
      window
    }
  }

  def mainSpark(args: Array[String]): Unit = {
    val conf = new SparkConf().setMaster("local[2]").setAppName("NetworkWordCount")
    val ssc = new StreamingContext(conf, Seconds(1))
    ssc.checkpoint("/tmp/wordcount")
    val lines: ReceiverInputDStream[String] = ssc.socketTextStream("localhost", 9999)

//    var words = List()
    val tenantCounts: DStream[String] = lines.flatMap(_.split(";"))
    // Count each word in each batch
    val pairs: DStream[(String, Int)] = tenantCounts.map(tenant => {
      val tenantCount: Array[String] = tenant.split(":")
      (tenantCount(0), tenantCount(1).toInt)
    })
//    val wordCounts = pairs.reduceByKey(_ + _)
//    val runningCounts = pairs.updateStateByKey[Int](updateFunction _)

    //lag('s_date, 1).over(Window.partitionBy(Column("key1"), 'key2).orderBy('s_date))

    val windowedWordCounts = pairs.reduceByKeyAndWindow((a: Int, b: Int) => (a + b), Seconds(30), Seconds(10))
//    pairs.window(Seconds(30), Seconds(10)).ARIMA.fitModel().forecast()

    // Print the first ten elements of each RDD generated in this DStream to the console
//    wordCounts.print()
//    runningCounts.print()
    windowedWordCounts.print()
    ssc.start() // Start the computation
    ssc.awaitTermination()
  }

  def updateFunction(newValues: Seq[Int], runningCount: Option[Int]): Option[Int] = {
    val newCount: Option[Int] = newValues.foldLeft(runningCount) {
      case (Some(m: Int), i) => Some(m + i)
      case (None, i)         => Some(i)
    }

    // add the new values with the previous running count to get the new count
    newCount
//    Some(newCount)
  }
}
