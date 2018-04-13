package com.weyong.onenet.client.handler;

import com.weyong.onenet.client.session.ClientSession;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * Created by hao.li on 2017/4/13.
 */
public class LocalChannelInitializer extends ChannelInitializer<SocketChannel> {
    public static final String LOCAL_RESPONSE_HANDLER = "LocalResponseHandler";
    public static final String CHANNEL_TRAFFIC_HANDLER = "ChannelTrafficHandler";
    private ClientSession clientSession;

    public LocalChannelInitializer(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        int bytesPreSecond = 0;
        if (clientSession != null) {
            bytesPreSecond = clientSession.getOneNetClientContext().getKBps() * 1024;
        }
        p.addLast(CHANNEL_TRAFFIC_HANDLER, new ChannelTrafficShapingHandler(bytesPreSecond,
                bytesPreSecond))
                .addLast(LOCAL_RESPONSE_HANDLER, new LocalInboudHandler(clientSession))
                .addLast(new ByteArrayEncoder());
    }
}
