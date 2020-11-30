### Predictions

ARIMA model https://en.wikipedia.org/wiki/Autoregressive_integrated_moving_average allows to train a model against the 
time series set and use either Seasonality or Moving Averages to predict next values.
In this experiment we use the moving averages based on the observed value and update it with every new value observed.

We use a fork of Cloudera Spark TS https://github.com/sryza/spark-timeseries updated to the latest version of Spark for
the open implemenattion of ARIMA.

This gives а real-time model which re-trains itself based on incoming values. 
It Can be connected to be fed with the data from Prometheus or from the application itself directly.

On average the update of the model with the new value and forecasting next 50 values takes 800 micros (locally on MacBook)

#### Running

Run io.adobe.ts.Prediction.main class. 
It will generate 25 images, one for every value from the testing set with 50 next predictions

### Prometheus Sidecar

Prometheus has released fast and memory efficient Stream API https://prometheus.io/blog/ 
which allows to read the data dirtectly from TSDB in the format how it's stored there with no overhead on parsing/encoding etc. 
Also, it reads only the data which consumer is ready to accept (back-pressured), so there is no memory wasted for the data which will never be used.

This project connects Client code which allows to use this API from scala. The client uses Snappy-encoded Proto messages
to make a request. The response looks like a stream of bytes in TSDB format each wrapped in Proto message. Client library
encodes the request, and provides streaming API to read the responses

#### Running

1. Download/Run Prometheus locally https://github.com/prometheus/prometheus
   - Use the following configuration to scrap the data from Kamon (Akka monitoring plugin) on localhost:9095 
        ```yaml
        global:
          scrape_interval:     1s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
          evaluation_interval: 1s # Evaluate rules every 15 seconds. The default is every 1 minute.
        scrape_configs:
          - job_name: 'prometheus'
            honor_labels: true
            metrics_path: '/'
            static_configs:
            - targets: ['localhost:9095']
        ```
   - Run Akka application with the Kamon enabled (i.e. https://git.corp.adobe.com/dx-devex-acceleration/experimental-multitenantrouting) to 
feed some data
2. Run io.adobe.prometheus.App.main to start actor system

#### Api

Request:

```scala
val prometheusReadRequest = Remote.ReadRequest
          .newBuilder()
          .addQueries(
            Remote.Query
              .newBuilder()
              .setStartTimestampMs(0)
              .setEndTimestampMs(System.currentTimeMillis)
              .addMatchers(
                LabelMatcher
                  .newBuilder()
                  .setName("__name__")
                  .setType(LabelMatcher.Type.EQ)
                  .setValue("akka_system_active_actors_count")) //Index to read from
              .setHints(
                (prometheus.Types.ReadHints newBuilder ())
                  .setStartMs(1604980010543L)
                  .setEndMs(System.currentTimeMillis)
                  .setStepMs(0)
                  .setRangeMs(300000)
              ))
          .addAcceptedResponseTypes(Remote.ReadRequest.ResponseType.STREAMED_XOR_CHUNKS) //forces Streaming API
          .build()
```


Client:

```scala
new PrometheusStreamReader(address)
          .stream(prometheusReadRequest)
          .onComplete(t =>
          t.foreach(s =>
            s.runWith(Sink.foreach(a => {
              println(a)
            }))))
```
