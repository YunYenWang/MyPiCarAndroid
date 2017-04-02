package com.cht.iot.mypicar;

import com.cht.iot.car.Control;
import com.cht.iot.util.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rickwang on 2017/4/2.
 */

public class MyPiCarClient {
    static final Logger LOG = LoggerFactory.getLogger(MyPiCarClient.class);

    final DatagramSocket socket;
    final String host;
    final int port;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    public MyPiCarClient(String host, int port) throws IOException {
        socket = new DatagramSocket();

        this.host = host;
        this.port = port;
    }

    public void close() throws IOException {
        socket.close();
    }

    public void control(int west, int east, long duration) throws IOException {
        Control ctrl = new Control();
        ctrl.west = west;
        ctrl.east = east;
        ctrl.duration = duration;

        String json = JsonUtils.toJson(ctrl);
        byte[] bytes = json.getBytes();

        DatagramPacket pkt = new DatagramPacket(bytes, bytes.length);
        pkt.setAddress(InetAddress.getByName(host));
        pkt.setPort(port);

        socket.send(pkt);
    }
}
