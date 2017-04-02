package com.cht.iot.mypicar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rickwang on 2017/4/2.
 */

public class PiCameraClient {
    static final Logger LOG = LoggerFactory.getLogger(PiCameraClient.class);

    final Socket socket;

    final Listener listener;

    ExecutorService executor = Executors.newSingleThreadExecutor();

    public PiCameraClient(String host, int port, int timeout, Listener listener) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        socket.setSoTimeout(timeout);

        this.listener = listener;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                process();
            }
        });
    }

    public void close() throws IOException {
        socket.close();
    }

    void process() {
        try {
            InputStream is = socket.getInputStream();
            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));

            for (;;) {
                int length = dis.readInt();
                byte[] snapshot = new byte[length];

                dis.readFully(snapshot);

                listener.onSnapshot(snapshot);
            }

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static interface Listener {
        void onSnapshot(byte[] snapshot);
    }
}
