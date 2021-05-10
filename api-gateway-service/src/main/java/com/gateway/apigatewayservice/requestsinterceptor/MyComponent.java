package com.gateway.apigatewayservice.requestsinterceptor;

import com.google.common.util.concurrent.AtomicDouble;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class MyComponent {

    private AtomicDouble ref;

    public MyComponent(MeterRegistry registry) {
        ref = registry.gauge("requests_per_second", new AtomicDouble(0.0f));
    }

    public void setRequestsPerSecond(double requestsPerSecond) {
        ref.set(requestsPerSecond);
    }
}
