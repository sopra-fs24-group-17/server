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
                .setAllowedOrigins("http://localhost:3000/", "https://sopra-fs24-group-17-client.oa.r.appspot.com/")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());

        registry.addEndpoint("/wss")
                .setAllowedOrigins("http://localhost:3000/", "https://sopra-fs24-group-17-client.oa.r.appspot.com/")
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/game", "/login", "/logout", "/friendshiprequest")
                .setTaskScheduler(heartBeatScheduler())
                .setHeartbeatValue(new long[] {60000, 120000});
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean serverContainer = new ServletServerContainerFactoryBean();
        serverContainer.setMaxSessionIdleTimeout(-1L);
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