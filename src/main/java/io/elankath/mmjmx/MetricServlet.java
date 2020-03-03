package io.elankath.mmjmx;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.*;

public class MetricServlet extends HttpServlet {
    String TAG_SYSTEM_ID = "system.id";
    private JmxMeterRegistry registry;

    public void init() {
        this.registry = new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM);
//        registry.config().commonTags(MeterConstants.TAG_SYSTEM_ID, svcEnv.getSystemId());
        final String systemId = System.getenv("IT_SYSTEM_ID");
        if (systemId != null) {
            registry.config().commonTags(TAG_SYSTEM_ID, systemId);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        final PrintWriter out = resp.getWriter();
        final String path = req.getPathInfo();
        try {
            final String val = req.getParameter("value");
            if (path == null || path.equalsIgnoreCase("")) {
                out.println("-- Received Request on host: " + req.getLocalName() + ", socket address:" + req.getLocalAddr() + ":"
                        + req.getLocalPort() + " at time: " + new Date(System.currentTimeMillis()) + " from client: " + req.getRemoteAddr());
                out.println("***** HTTP(S) PROXY INFO *****");
                final String httpProxyHost = System.getProperty("https.proxyHost");
                final String httpPort = System.getProperty("https.proxyPort");
                out.println("https.proxyHost = " + httpProxyHost);
                out.println("https.proxyPort= " + httpPort);
                final InetAddress[] allIps = InetAddress.getAllByName(httpProxyHost);
                out.println("InetAddress(es) of https.proxyHost = " + Arrays.asList(allIps));
                out.println("***** SYSTEM PROPERTIES *****");
                Properties properties = System.getProperties();
                properties.list(out);
                out.println("***** ENVIRONMENT VARIABLES **** ");
                System.getenv().entrySet().forEach(out::println);
            } else if (path.startsWith("/counters")) {
                final String counterName = path.substring(path.lastIndexOf('/') + 1);
                final int incVal = val == null || val.trim().isEmpty() ?  1: Integer.parseInt(val);
                out.printf("Incrementing counter with name: %s and value: %d\n", counterName, incVal);
                final Counter c = findMeter(registry.getMeters(), counterName, Counter.class)
                        .orElseGet( () -> registry.counter(counterName));
                c.increment(incVal);
                out.println("Updated counter: " + c);
            }
        } catch (Throwable t) {
            t.printStackTrace(out);
        } finally {
            out.flush();
        }
    }

    public static <T extends Meter> Optional<T> findMeter(final List<Meter> meters, final String name, final Class<T> meterClz) {
        return meters.stream()
                .filter(m -> Objects.equals(m.getId().getName(),name))
                .findAny()
                .map(meterClz::cast);
    }
}
