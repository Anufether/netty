/*
 * Copyright 2013 The Netty Project
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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.ssl.SslContext;

/**
 * HTTP服务器的Pipeline初始化器
 * 
 * 关键概念：ChannelPipeline（处理器链）
 * - Pipeline是一个双向链表，包含多个ChannelHandler
 * - 入站事件（Inbound）：从头到尾传播，如：接收数据、连接建立
 * - 出站事件（Outbound）：从尾到头传播，如：发送数据、关闭连接
 * - Handler的顺序非常重要！
 */
public class HttpHelloWorldServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public HttpHelloWorldServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    /**
     * 初始化Channel的Pipeline（每个新连接都会调用此方法）
     * 
     * Pipeline中的Handler执行顺序：
     * 入站（读取数据）：从上到下
     * 出站（发送数据）：从下到上
     */
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        
        // 1. SSL/TLS处理器（可选，仅在启用HTTPS时添加）
        // 作用：加密/解密HTTPS流量
        // 位置：必须放在最前面，因为需要先解密才能解析HTTP
        if (sslCtx != null) {
            p.addLast(sslCtx.newHandler(ch.alloc()));
        }
        
        // 2. HTTP编解码器（核心！）
        // 作用：
        //   - 入站：将ByteBuf解码为HttpRequest、HttpContent等HTTP对象
        //   - 出站：将HttpResponse编码为ByteBuf
        // 这是HttpRequestDecoder + HttpResponseEncoder的组合
        p.addLast(new HttpServerCodec());
        
        // 3. HTTP内容压缩器
        // 作用：自动压缩HTTP响应（支持gzip、deflate）
        // 如果客户端发送"Accept-Encoding: gzip"头，响应会被自动压缩
        p.addLast(new HttpContentCompressor((CompressionOptions[]) null));
        
        // 4. HTTP 100-Continue支持
        // 作用：处理HTTP协议的"Expect: 100-continue"头
        // 用于大文件上传前的预检，客户端先询问服务器是否接受上传
        p.addLast(new HttpServerExpectContinueHandler());
        
        // 5. 业务处理器（最后一个入站Handler）
        // 作用：处理HTTP请求，生成HTTP响应
        // 这里实现具体的业务逻辑：返回"Hello World"
        HttpHelloWorldServerHandler httpHelloWorldServerHandler = new HttpHelloWorldServerHandler();
        p.addLast(httpHelloWorldServerHandler);
    }
}
