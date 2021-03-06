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
package net.sf.harp07.telnet.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import javax.swing.JOptionPane;
import org.apache.commons.validator.routines.InetAddressValidator;

/**
 * Simplistic telnet client.
 */
public final class TelnetClient {

    private static InetAddressValidator ipv = InetAddressValidator.getInstance();
    static final boolean SSL = System.getProperty("ssl") != null;
    static String HOST;//System.getProperty("host", "10.73.250.11");
    static final int PORT = 23;//Integer.parseInt(System.getProperty("port", SSL? "8992" : "8023"));

    public static void main(String[] args) throws Exception {
        HOST = JOptionPane.showInputDialog(null, "Enter IP-address: ", "IP-address", JOptionPane.OK_CANCEL_OPTION);
        /*int cancelint=JOptionPane.CANCEL_OPTION;
        int okint=JOptionPane.OK_OPTION;
        if (cancelint==JOptionPane.CANCEL_OPTION) return;*/
        try {
            if (!ipv.isValid(HOST)) {
                JOptionPane.showMessageDialog(null, "Wrong IP-address !", "wrong IP", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NullPointerException ne) {
                JOptionPane.showMessageDialog(null, "Empty IP-address !", "wrong IP", JOptionPane.ERROR_MESSAGE);
                return;            
        }
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new TelnetClientInitializer(sslCtx));

            // Start the connection attempt.
            Channel ch = b.connect(HOST, PORT).sync().channel();

            // Read commands from the stdin.
            ChannelFuture lastWriteFuture = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            //Scanner sc=new Scanner(System.in);
            for (;;) {
                String line = in.readLine();
                //String line = sc.next();
                if (line == null) {
                    break;
                }

                // Sends the received line to the server.
                lastWriteFuture = ch.writeAndFlush(line + "\r\n");

                // If user typed the 'bye' command, wait until the server closes
                // the connection.
                if ("bye".equals(line.toLowerCase())) {
                    ch.closeFuture().sync();
                    break;
                }
            }

            // Wait until all messages are flushed before closing the channel.
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        //} catch (IOException ioe) {
        //    group.shutdownGracefully();
        } finally {
            group.shutdownGracefully();
        }
    }
}
