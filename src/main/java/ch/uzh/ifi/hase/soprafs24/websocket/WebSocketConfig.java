package ch.uzh.ifi.hase.soprafs24.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CustomHandshakeHandler customHandshakeHandler;

    @Autowired
    public WebSocketConfig(CustomHandshakeHandler customHandshakeHandler) {
        this.customHandshakeHandler = customHandshakeHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .setHandshakeHandler(customHandshakeHandler)
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addEndpoint("/wss")
                .setAllowedOrigins("*")
                .setHandshakeHandler(customHandshakeHandler)
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/game", "/login", "/logout", "/friendshiprequest");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean serverContainer = new ServletServerContainerFactoryBean();
        serverContainer.setMaxSessionIdleTimeout(-1L); // Avoids closure of websocket due to inactivity -> To be adjusted later
        return serverContainer;
    }

    // Used to verify persistent open configuration of websocket through ping
    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler hbScheduler = new ThreadPoolTaskScheduler();
        hbScheduler.setPoolSize(1);
        return hbScheduler;
    }
}
