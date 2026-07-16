package tacos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tacos.kafka")
public class OrderEventsProperties {

    private boolean enabled = true;
    private String ordersEventsTopic = "orders.events";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOrdersEventsTopic() {
        return ordersEventsTopic;
    }

    public void setOrdersEventsTopic(String ordersEventsTopic) {
        this.ordersEventsTopic = ordersEventsTopic;
    }
}
