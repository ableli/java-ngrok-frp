package com.weyong.onenet.client.manager;

import com.weyong.onenet.client.OneNetClient;
import com.weyong.onenet.client.config.OnenetClientServerConfig;
import com.weyong.onenet.client.initializer.OneNetChannelInitializer;
import com.weyong.onenet.client.session.ServerSession;
import com.weyong.onenet.dto.HeartbeatPackage;
import com.weyong.onenet.dto.InitialRequestPackage;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by haoli on 2018/4/5.
 */
@Slf4j
@Data
@Service
public class OneNetServerSessionManager {
    private ConcurrentHashMap<String, ServerSession> oneNetServerSessions = new ConcurrentHashMap<>();
    private String clientName;
    private Integer reconnectAfterNSeconds;

    @Scheduled(fixedDelay = 2000)
    private void heartbeat() {
        oneNetServerSessions.values().stream().forEach(serverSession -> {
            if (serverSession.getLastHeartbeatTime() == null) {
                createServerSession(serverSession);
                heatrbeatOneChannel(serverSession);
                return;
            }
            if (Duration.between(serverSession.getLastHeartbeatTime(), Instant.now()).toMillis()>reconnectAfterNSeconds*1000) {
                log.info(String.format("Server Session %s:%d inactive.Try to renew.",
                        serverSession.getOnenetClientServerConfig().getHostName(), serverSession.getOnenetClientServerConfig().getOneNetPort()));
                createServerSession(serverSession);
            }
            heatrbeatOneChannel(serverSession);
        });
    }

    private void heatrbeatOneChannel(ServerSession serverSession) {
        try {
            serverSession.getServerChannel().writeAndFlush(HeartbeatPackage.instance());
            log.debug(String.format("Heartbeat server session %s:%d",
                    serverSession.getOnenetClientServerConfig().getHostName(), serverSession.getOnenetClientServerConfig().getOneNetPort()));
        } catch (Exception ex) {
            log.error(String.format("Heartbeat server session %s:%d meet ex , and  it is :%s",
                    serverSession.getOnenetClientServerConfig().getHostName(), serverSession.getOnenetClientServerConfig().getOneNetPort(),
                    ex.getMessage()));
        }
    }

    private void createServerSession(ServerSession serverSession) {
        try {
            log.info(String.format("Start to connect server session %s:%d",
                    serverSession.getOnenetClientServerConfig().getHostName(), serverSession.getOnenetClientServerConfig().getOneNetPort()));
            OnenetClientServerConfig onenetClientServerConfig = serverSession.getOnenetClientServerConfig();
            Channel socketChannel = OneNetClient.createChannel(onenetClientServerConfig.getHostName(), onenetClientServerConfig.getOneNetPort(), new OneNetChannelInitializer(serverSession));
            InitialRequestPackage dt = new InitialRequestPackage();
            dt.setClientName(clientName);
            dt.setContextNames(serverSession.getOnenetClientServerConfig().getContexts().stream()
                    .map((oneNetClientContextConfig -> oneNetClientContextConfig.getContextName())).collect(Collectors.toList()));
            socketChannel.writeAndFlush(dt);
            serverSession.setServerChannel(socketChannel);
            log.info(String.format("Server session %s:%d established",
                    serverSession.getOnenetClientServerConfig().getHostName(), serverSession.getOnenetClientServerConfig().getOneNetPort()));
        } catch (Exception ex) {
            log.error(String.format("Connection server session %s:%d meet ex , and  it is :%s",
                    serverSession.getOnenetClientServerConfig().getHostName(), serverSession.getOnenetClientServerConfig().getOneNetPort(),
                    ex.getMessage()));
        }
    }
}
