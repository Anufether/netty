package io.netty.example.http.helloworld;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.example.util.ServerUtil;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;

/**
 * Netty服务器通用模版
 */
public final class NettyServerTemplate {
    private static final boolean SSL = System.getProperty("ssl") != null;
    private static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "8443" : "8080"));

    public static void main(String[] args) throws Exception {
        final SslContext sslCtx = ServerUtil.buildSslContext();
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    p.addLast(new HttpServerCodec());
                    p.addLast(new HttpContentCompressor((CompressionOptions[]) null));
                    p.addLast(new HttpServerExpectContinueHandler());
                    SimpleChannelInboundHandler<HttpObject> channelInboundHandler = new SimpleChannelInboundHandler<HttpObject>() {

                        //                private static final byte[] CONTENT = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };
                        // 推荐：显式指定UTF-8编码，跨平台/跨JVM无乱码
                        private final byte[] CONTENT = "你好，世界".getBytes(StandardCharsets.UTF_8);

                        /**
                         * 刷新
                         */
                        public void channelReadComplete(ChannelHandlerContext ctx) {
                            ctx.flush();
                        }

                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
                            if (msg instanceof HttpRequest) {
                                HttpRequest req = (HttpRequest) msg;
                                boolean keepAlive = HttpUtil.isKeepAlive(req);

                                FullHttpResponse response = new DefaultFullHttpResponse(
                                        req.protocolVersion(),
                                        HttpResponseStatus.OK,
                                        Unpooled.wrappedBuffer(CONTENT)
                                );
                                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

                                if (keepAlive) {
                                    if (req.protocolVersion() == HttpVersion.HTTP_1_1) {
                                        response.headers().set(HttpHeaderNames.CONNECTION, KEEP_ALIVE);
                                    }
                                } else {
                                    response.headers().set(CONNECTION, CLOSE);
                                }
                                ChannelFuture channelFuture = ctx.write(response);

                                if (!keepAlive) {
                                    channelFuture.addListener(ChannelFutureListener.CLOSE);
                                }
                            }

                        }

                        @Override
                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                            // 打印异常堆栈
                            cause.printStackTrace();
                            // 关闭连接
                            ctx.close();
                        }
                    };
                    p.addLast(channelInboundHandler);
                }
            };

            bootstrap.group(group)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(channelInitializer);

            ChannelFuture future = bootstrap.bind(PORT).sync();
            System.out.println("Server started on port: " + PORT);
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }


}
