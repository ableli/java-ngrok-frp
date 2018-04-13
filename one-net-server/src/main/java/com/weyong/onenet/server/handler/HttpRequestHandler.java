package com.weyong.onenet.server.handler;

import com.weyong.onenet.dto.DataPackage;
import com.weyong.onenet.server.Initializer.HttpChannelInitializer;
import com.weyong.onenet.server.context.OneNetServerHttpContext;
import com.weyong.onenet.server.session.OneNetHttpSession;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by haoli on 12/26/2017.
 */
@Slf4j
public class HttpRequestHandler extends SimpleChannelInboundHandler<Object> {
    private OneNetHttpSession httpSession;

    public HttpRequestHandler(OneNetHttpSession httpSession) {
        this.httpSession = httpSession;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        String hostName = "";
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            HttpHeaders headers = request.headers();
            hostName = headers.get("HOST");
            if (StringUtils.isNotEmpty(hostName)) {
                httpSession.setContextConfig(OneNetServerHttpContext.instance.getContextConfig(hostName));
                if (httpSession.getContextConfig() != null && httpSession.getOneNetChannel() == null) {
                    Channel clientChannel = OneNetServerHttpContext.instance.getOneNetConnectionManager().getAvailableChannel(hostName);
                    if (clientChannel != null) {
                        httpSession.setOneNetChannel(clientChannel);
                        int kBps = httpSession.getContextConfig().getKBps()*1024;
                        ChannelTrafficShapingHandler trafficShapingHandler =  ((ChannelTrafficShapingHandler) ctx.pipeline().get(HttpChannelInitializer.trafficHandler));
                        trafficShapingHandler.setWriteLimit(kBps);
                        trafficShapingHandler.setReadLimit(kBps);
                    }
                }
            }
        }
        if (httpSession.getOneNetChannel() != null) {
            while (httpSession.getQueue().size() > 0) {
                DataPackage dataPackage = httpSession.getQueue().poll();
                dataPackage.setContextName(httpSession.getContextName());
                dataPackage.setAes(httpSession.getContextConfig().isAes());
                dataPackage.setZip(httpSession.getContextConfig().isZip());
                httpSession.getOneNetChannel().writeAndFlush(dataPackage);
            }
        } else {
            log.info(String.format("Can't find client session for http context %s.",
                    hostName));
            ctx.close();
        }
    }
}
