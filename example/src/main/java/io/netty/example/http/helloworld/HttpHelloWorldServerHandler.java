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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * HTTP业务处理器：处理HTTP请求并返回响应
 * 
 * 继承SimpleChannelInboundHandler的好处：
 * - 自动类型转换：泛型<HttpObject>会自动过滤消息类型
 * - 自动资源释放：channelRead0执行完后会自动释放msg，避免内存泄漏
 * - 相比ChannelInboundHandlerAdapter更安全、更简洁
 */
public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    // HTTP响应内容："Hello World"（字节数组形式）
    private static final byte[] CONTENT = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };

    /**
     * 当读取操作完成时调用
     * 作用：刷新所有待发送的数据到网络
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * 处理HTTP请求（核心方法）
     * 
     * @param ctx 上下文对象，用于与Pipeline交互（读写数据、关闭连接等）
     * @param msg HTTP对象，可能是HttpRequest、HttpContent等
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        // 1. 类型检查：确保接收到的是HTTP请求对象
        // 注意：一个完整的HTTP请求可能分为多个对象（HttpRequest + HttpContent）
        // 这里只处理HttpRequest部分
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            // 2. 检查客户端是否要求保持连接（HTTP Keep-Alive）
            // Keep-Alive允许在一个TCP连接上发送多个HTTP请求/响应，提高性能
            boolean keepAlive = HttpUtil.isKeepAlive(req);
            
            // 3. 构建HTTP响应对象
            FullHttpResponse response = new DefaultFullHttpResponse(
                req.protocolVersion(),              // HTTP版本（1.0、1.1、2.0）
                OK,                                 // HTTP状态码：200 OK
                Unpooled.wrappedBuffer(CONTENT)     // 响应体内容："Hello World"
            );
            
            // 4. 设置响应头
            response.headers()
                .set(CONTENT_TYPE, TEXT_PLAIN)      // Content-Type: text/plain（纯文本）
                .setInt(CONTENT_LENGTH, response.content().readableBytes());  // Content-Length: 11

            // 5. 处理Keep-Alive机制
            if (keepAlive) {
                // HTTP/1.1默认开启Keep-Alive，HTTP/1.0默认关闭
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    // 如果协议版本默认不支持Keep-Alive，需要显式设置
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                // 6. 如果不保持连接，告诉客户端要关闭连接
                response.headers().set(CONNECTION, CLOSE);
            }

            // 7. 发送HTTP响应
            // 注意：这里只是write，还没有真正发送，需要flush才会发送
            ChannelFuture f = ctx.write(response);

            // 8. 如果不保持连接，发送完成后关闭Channel
            if (!keepAlive) {
                // 添加监听器：当写操作完成后，自动关闭连接
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * 异常处理：当处理过程中发生异常时调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 打印异常堆栈
        cause.printStackTrace();
        // 关闭连接
        ctx.close();
    }
}
