package io.adobe.ts

import com.cloudera.sparkts.EasyPlot
import com.cloudera.sparkts.models.{ARIMA, ARIMAModel}
import org.apache.spark.mllib.linalg.DenseVector

import scala.concurrent.duration._

object Prediction {

  /**
   * 1. Create dataframe with the timeseries for the last 100 intervals
   * 2. Split on training and testing
   * 3. Fit ARIMA model on learning dataset
   * 4. Predict testing and calculate errors:
   * - MAE ( Mean absolute error), Root Mean Square Error (RMSE)
   * - MAPE ( Mean Absolute Percentage Error)
   * - RMSEP ( Root Mean Square Percentage Error)
   * - Almost correct Predictions Error rate (AC_errorRate)
   *
   * References:
   * 1. https://medium.com/making-sense-of-data/time-series-next-value-prediction-using-regression-over-a-rolling-window-228f0acae363
   * 2. https://prometheus.io/blog/
   */
  def main(args: Array[String]): Unit = {
    val dataFile = getClass.getClassLoader.getResourceAsStream("Traffic.csv")
    val rawData = scala.io.Source.fromInputStream(dataFile).getLines().toArray.map(_.toDouble)
    //split data 80/20 into training/testing. We will train model on the training set, then will try to predict testing data in real time one by one
    val training = rawData.take((rawData.length * 0.8).toInt)
    val testing = rawData.drop((rawData.length * 0.8).toInt)

    val data = new DenseVector(training)

    //𝑌𝑡=𝑟𝑌(𝑡−1)+𝑒𝑡+𝑎𝑒(𝑡−1)  where 𝑎 is the moving average parameter.
    /* Given a time series, fit a non-seasonal ARIMA model of order (p, d, q), where p represents
     * the autoregression terms, d represents the order of differencing, and q moving average error
     * terms
     */
    val model: ARIMAModel = ARIMA.fitModel(1, 0, 1, data)

    //for each data point in testing set predict next 50 values. N-th prediction will take into account all N-1 previous values from the training set
    testing.foldLeft(training)((b, a) => {
      predictNext(model, b, a, 50)
    })
    //@TODO test predictions instead of plotting
  }

  /**
   * Predict next predictNum values based on the trained model.
   *
   * @param model trained model
   * @param window the data points model was trained on
   * @param next current value we predict upon
   * @param predictNum number of values to predict
   * @return the window+next data set. Predictions are plotted but not returned
   */
  def predictNext(model: ARIMAModel, window: Array[Double], next: Double, predictNum: Int): Array[Double] = {
    val dataset = window :+ next
    val time: Long = System.nanoTime()
    val forecast = model.forecast(new DenseVector(dataset), predictNum)
    println("Time: " + (System.nanoTime() - time).nanos.toMicros)
    //plot the predicted data alongside with actual values
    EasyPlot.ezplot(Seq(new DenseVector(dataset ++ Array.fill(predictNum)(next)), forecast))
    dataset
  }
}
