# micrometer-jmx-example
Sample metrics WAR leveraging MicroMeter JMX Registry

## Features
* Produces Tomcat/Servlet-Container Deployable `mmjmx.war`.
* Uses `io.micrometer.jmx.JmxMeterRegistry` to register `Timer`s, `Counter`s and `Gauges`.
* Provides `io.elankath.mmjmx.MetricServlet` at context path `/` supporting following HTTP operations:
  * `POST /timers/<timerName>` with body with parseable `java.time.Duration` body to record a duration for the given named timer. Registration of Timer will be done if not already done.
  * `POST /counters/<counterName>` with body as integer value to increment the counter for the given named counter. Registration of Counter will be done if not already done.
  * `POST /gauges/<gaugeName>` with body as integer value to set the gauge with given name to the body value. Registration of Gauge will be done if not already done.
  * `GET /` dumps all values of all meters in plain text.
* Use `curl` to post requests to invoke the endpoints.
* A `cf.yaml` file to deploy the WAR as CF application. Exposes JMX remote port on 5000, which can be port-forwarded.
* Use jconsole to check MBean registry. 

  
