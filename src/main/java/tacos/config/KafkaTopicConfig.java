package tacos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(prefix = "tacos.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public NewTopic ordersEventsTopic(OrderEventsProperties properties) {
        return TopicBuilder.name(properties.getOrdersEventsTopic())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
