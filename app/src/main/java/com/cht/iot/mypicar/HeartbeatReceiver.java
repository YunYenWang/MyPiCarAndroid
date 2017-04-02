package com.cht.iot.mypicar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rickwang on 2017/4/2.
 */

public class HeartbeatReceiver {
    static final Logger LOG = LoggerFactory.getLogger(HeartbeatReceiver.class);

    final DatagramSocket socket;
    int port = 60000;

    final Listener listener;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    public HeartbeatReceiver(Listener listener) throws IOException {
        socket = new DatagramSocket(port);

        LOG.info("HeartbeatReceiver listens at {}", port);

        this.listener = listener;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                process();
            }
        });
    }

    public void close() {
        socket.close();
    }

    void process() {
        try {
            byte[] bytes = new byte[32];
            DatagramPacket pkt = new DatagramPacket(bytes, 0, bytes.length);
            socket.receive(pkt);

            try {
                String host = pkt.getAddress().getHostAddress();
                listener.onHeartbeat(host);

            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }

        } catch (IOException e) {
            LOG.error("Socket is closed"); // socket is closed
        }
    }

    public static interface Listener {
        void onHeartbeat(String host);
    }
}
