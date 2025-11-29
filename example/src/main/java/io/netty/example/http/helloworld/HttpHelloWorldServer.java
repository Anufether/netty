/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.http.helloworld;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.example.util.ServerUtil;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

/**
 * HTTP服务器示例：接收HTTP请求并返回"Hello World"
 * 这是一个最简单的HTTP服务器实现，展示了Netty处理HTTP协议的基本流程
 */
public final class HttpHelloWorldServer {

    // 是否启用SSL/TLS加密（通过-Dssl参数设置）
    static final boolean SSL = System.getProperty("ssl") != null;
    // 服务器监听端口：默认8080（HTTP）或8443（HTTPS）
    static final int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));

    public static void main(String[] args) throws Exception {
        // 1. 配置SSL上下文（如果启用HTTPS）
        final SslContext sslCtx = ServerUtil.buildSslContext();

        // 2. 创建事件循环组（EventLoopGroup）
        // 这是Netty的核心组件，负责处理所有的I/O操作和事件
        // MultiThreadIoEventLoopGroup：多线程事件循环组，可以利用多核CPU
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            // 3. 创建服务器启动辅助类（ServerBootstrap）
            ServerBootstrap b = new ServerBootstrap();
            
            // 4. 配置服务器参数
            // SO_BACKLOG：TCP连接队列的最大长度，这里设置为1024
            b.option(ChannelOption.SO_BACKLOG, 1024);
            
            // 5. 设置EventLoopGroup和Channel类型
            b.group(group)
             .channel(NioServerSocketChannel.class)           // 使用NIO传输（基于Java NIO）
             .handler(new LoggingHandler(LogLevel.INFO))      // 服务器级别的日志处理器
             .childHandler(new HttpHelloWorldServerInitializer(sslCtx));  // 客户端连接的处理器初始化器

            // 6. 绑定端口并启动服务器
            // sync()：同步等待绑定完成
            Channel ch = b.bind(PORT).sync().channel();

            // 7. 打印提示信息，告诉用户如何访问服务器
            System.err.println("Open your web browser and navigate to " +
                    (SSL? "https" : "http") + "://127.0.0.1:" + PORT + '/');

            // 8. 等待服务器关闭
            // closeFuture()：获取Channel的关闭Future
            // sync()：阻塞等待，直到服务器Channel关闭
            ch.closeFuture().sync();
        } finally {
            // 9. 优雅地关闭EventLoopGroup，释放所有资源
            group.shutdownGracefully();
        }
    }
}
