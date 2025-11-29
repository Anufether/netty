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
package io.netty.example.echo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.StandardCharsets;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 打印ByteBuf的元数据信息（你之前看到的）
        System.out.println("ByteBuf元数据: " + msg.toString());
        
        // 将msg转换为ByteBuf，读取实际内容
        ByteBuf buf = (ByteBuf) msg;
        
        // 方法1：使用toString(Charset)直接转换为字符串（推荐）
        String content = buf.toString(StandardCharsets.UTF_8);
        System.out.println("接收到的消息: " + content);
        
        // 方法2：也可以手动读取字节（了解即可）
        // byte[] bytes = new byte[buf.readableBytes()];
        // buf.getBytes(buf.readerIndex(), bytes);
        // String content = new String(bytes, StandardCharsets.UTF_8);
        
        // 回显数据给客户端
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
