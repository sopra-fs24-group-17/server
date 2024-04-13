package ch.uzh.ifi.hase.soprafs24.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addEndpoint("/wss")
                .setAllowedOrigins("*")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/game", "/login", "/logout", "/friendshiprequest")
                .setHeartbeatValue(new long[] {1000, 1000})
                .setTaskScheduler(heartBeatScheduler())
                .setHeartbeatValue(new long[] {60000, 120000});
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean serverContainer = new ServletServerContainerFactoryBean();
        serverContainer.setMaxSessionIdleTimeout(-1L); // Avoids closure of websocket due to inactivity -> To be adjusted later
        return serverContainer;
    }

    // Used to verify persistent open connection of websocket through constant ping from server to client
    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler hbScheduler = new ThreadPoolTaskScheduler();
        hbScheduler.setPoolSize(1);
        return hbScheduler;
    }
}