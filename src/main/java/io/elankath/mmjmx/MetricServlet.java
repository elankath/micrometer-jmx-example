package io.elankath.mmjmx;

import io.micrometer.core.instrument.*;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@WebServlet("/*")
public class MetricServlet extends HttpServlet {

    String TAG_SYSTEM_ID = "system.id";
    private JmxMeterRegistry registry;
    private Logger log = LoggerFactory.getLogger(MetricServlet.class);

    public void init() {
        try {
            log.warn("(init) Entered init");
            this.registry = new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM);
//        registry.config().commonTags(MeterConstants.TAG_SYSTEM_ID, svcEnv.getSystemId());
            final String systemId = System.getenv("IT_SYSTEM_ID");
            if (systemId != null) {
                registry.config().commonTags(TAG_SYSTEM_ID, systemId);
            }
            log.warn("(init) Created registry: {}", registry);
        } catch (Throwable t) {
            log.error("Error in MetricServlet initialization", t);
            t.printStackTrace(System.out);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        final PrintWriter out = resp.getWriter();
        final String path = req.getPathInfo();
        try {
            final List<Meter> meters = registry.getMeters();
            if (meters.isEmpty()) {
                out.println("*** No meters defined");
                return;
            }
            if (path.startsWith("/counters")) {
                out.println("*** BEGIN COUNTERS ***");
                registry.getMeters()
                        .stream()
                        .filter(m -> Counter.class.isAssignableFrom(m.getClass()))
                        .map(Counter.class::cast)
                        .forEach(c -> out.printf("CounterId: %s, Value: %s\n", c.getId(), c.count()));
                out.println("*** END COUNTERS ***");
            } else if (path.startsWith("/gauges")) {
                out.println("*** BEGIN GAUGES ***");
                registry.getMeters()
                        .stream()
                        .filter(m -> Gauge.class.isAssignableFrom(m.getClass()))
                        .map(Gauge.class::cast)
                        .forEach(g -> out.printf("GaugeId: %s, Value: %s\n", g.getId(), g.value()));
                out.println("*** END GAUGES ***");
            } else if (path.startsWith("/timers")) {
                out.println("*** BEGIN TIMERS ***");
                registry.getMeters()
                        .stream()
                        .filter(m -> Timer.class.isAssignableFrom(m.getClass()))
                        .map(Timer.class::cast)
                        .forEach(t -> out.printf("TimerId: %s, TotalTime: %s, MaxDuration: %s\n", t.getId(), t.totalTime(TimeUnit.SECONDS), t.max(TimeUnit.SECONDS)));
                out.println("*** END TIMERS ***");
            }
        } catch (Throwable t) {
            t.printStackTrace(out);
            t.printStackTrace(System.out);
        } finally {
            out.flush();
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        final PrintWriter out = resp.getWriter();
        final String path = req.getPathInfo();
        final String val = req.getParameter("value");
        try {
            if (path.startsWith("/counters")) {
                final String counterName = path.substring(path.lastIndexOf('/') + 1);
                final int incVal = val == null || val.trim().isEmpty() ? 1 : Integer.parseInt(val);
                out.printf("Incrementing counter with name: %s and value: %d\n", counterName, incVal);
                final Counter c = findMeter(registry.getMeters(), counterName, Counter.class)
                        .orElseGet(() -> registry.counter(counterName));
                c.increment(incVal);
                out.println("Updated counter: " + c);
            }
        } catch (Throwable t) {
            t.printStackTrace(out);
            t.printStackTrace(System.out);
        } finally {
            out.flush();
        }
    }


    public static <T extends Meter> Optional<T> findMeter(final List<Meter> meters, final String name, final Class<T> meterClz) {
        return meters.stream()
                .filter(m -> Objects.equals(m.getId().getName(), name))
                .findAny()
                .map(meterClz::cast);
    }
}
