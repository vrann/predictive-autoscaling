#Essential Import statements
import numpy as np
import pandas as pd
from fbprophet import Prophet
import warnings
import sys
import os
import gc
import time


changepoint_prior_scale = 0.005
seasonality_prior_scale = 0.05
changepoint_range = 0.5

from pyspark.sql.functions import collect_list, struct
from pyspark.sql.types import FloatType, StructField, StructType, StringType, TimestampType ,DoubleType

from pyspark.sql import SparkSession

spark = SparkSession.builder \
        .master("local") \
        .appName("Word Count") \
        .config("spark.some.config.option", "some-value") \
        .getOrCreate()

def retrieve_data():
    """Load sample data from gcp(can be from hadoop) as a pyspark.sql.DataFrame."""
    df = spark.read.options(header='true', inferSchema='true', sep=',', treatEmptyValuesAsNulls='true', nullValue = "NULL" ).csv("file:///Users/etulika/Projects/LinearRegression/prophet_input.csv")
    # Drop any null values incase they exist
    df = df.dropna()

    # Rename timestamp to ds and total to y for fbprophet
    df = df.select(
        df['timestamp'].alias('ds'),
        df['app'],
        df['value'].cast(FloatType()).alias('y'),
        df['metric']
    )

    return df

def forecasting_logic(partition_list):
    import numpy as np
    import pandas as pd
    from fbprophet import Prophet
    import warnings
    import sys
    import os
    import gc
    import time
    #Convert the list to pandas dataframe for any further processing before passing to your library
    pdf = pd.DataFrame(partition_list,columns=['ds','app','y','metric'])
    pdf['ds'] = pd.to_datetime(pdf['ds'], format = '%Y-%m-%d')
    prh = Prophet(seasonality_prior_scale=seasonality_prior_scale,
                changepoint_prior_scale=changepoint_prior_scale,
                changepoint_range=changepoint_range,
                interval_width=0.95, weekly_seasonality=True, daily_seasonality=True)
    model=prh.fit(pdf)
    future = model.make_future_dataframe(periods=50)
    predicted_pdf = model.predict(future)
    predicted_pdf['ds'] = predicted_pdf['ds'].astype('str')
    predicted_pdf1 = predicted_pdf[['ds', 'yhat', 'yhat_lower', 'yhat_upper']]
    return predicted_pdf1.values.tolist()


from pyspark.sql.types import FloatType, StructField, StructType, StringType, TimestampType ,DoubleType
df = retrieve_data()
df = df.repartition('app', 'metric')
m_rdd = df.rdd.mapPartitions(forecasting_logic,preservesPartitioning=True)
schema = StructType([StructField('ds', StringType(), True), \
                    StructField('yhat', DoubleType(), True), \
                    StructField('yhat_lower', DoubleType(), True), \
                    StructField('yhat_upper', DoubleType(), True)])

# Providing a schema will avoid unnecessary sample partition evaluation by spark to infer schema
# and will save time
df_prediction = spark.createDataFrame(m_rdd, schema)
df_prediction.show()


#Sample Input data
"""
timestamp,metric,app,value
2019-01-01 00:00:00,m1,a,61.87483488182826
2019-01-01 00:05:00,m1,a,4.774629678532727
2019-01-01 00:10:00,m1,a,56.723598483827686
2019-01-01 00:15:00,m1,a,73.41004199189977
2019-01-01 00:20:00,m1,a,25.89179312049582
2019-01-01 00:25:00,m1,a,75.94699428222006
2019-01-01 00:30:00,m1,a,15.20946296181217
2019-01-01 00:35:00,m1,a,82.9956834656641
2019-01-01 00:40:00,m1,a,4.720798758063505
"""



#                ############# Output ###################
"""
+-------------------+------------------+-------------------+------------------+
|                 ds|              yhat|         yhat_lower|        yhat_upper|
+-------------------+------------------+-------------------+------------------+
|2019-01-01 00:00:00|51.354028798471155|-2.4206375878734123|105.38500966832127|
|2019-01-01 00:05:00| 51.33999971200106| -4.341115236530652|106.30849211749636|
|2019-01-01 00:10:00| 51.32161565031851| -1.368532095461058|105.77883407617068|
|2019-01-01 00:15:00|  51.2990168614464|-1.9202271113414415|111.35647478099933|
|2019-01-01 00:20:00| 51.27237136205229| -7.602349745735067|105.55498711789016|
|2019-01-01 00:25:00|   51.241873649662| -4.135534610817248|106.13077103160703|
|2019-01-01 00:30:00|51.207743227776234| -6.384170162946994|107.98202467941587|
|2019-01-01 00:35:00| 51.17022295615627| -3.859753032174601|109.71308846632631|
|2019-01-01 00:40:00|51.129577237843215|-10.862201256991623|108.94310149998367|
|2019-01-01 00:45:00|51.086090057492235|-3.1266736530769115| 107.7096935820277|
|2019-01-01 00:50:00| 51.04006288556377|  -8.33943965422948|105.48242209146146|
|2019-01-01 00:55:00| 50.99181246415709| -4.984629149002711|105.32229568807001|
|2019-01-01 01:00:00|50.941668491590434| -4.662277553616718|108.51431282431047|
|2019-01-01 01:05:00| 50.88997122255918| -2.947079869664252|107.31026503284492|
"""