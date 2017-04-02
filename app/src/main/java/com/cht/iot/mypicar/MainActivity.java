package com.cht.iot.mypicar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    static final Logger LOG = LoggerFactory.getLogger(MainActivity.class);

    HeartbeatReceiver heartbeatReceiver;

    int cameraClientPort = 20000;
    int cameraClientTimeout = 5000;

    MyPiCarClient client;
    int carClientPort = 10000;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    TextView hostTextView;
    ImageView snapshotImageView;

    Button forward;
    Button backward;
    Button right;
    Button left;

    int direction = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hostTextView = (TextView) findViewById(R.id.host);
        snapshotImageView = (ImageView) findViewById(R.id.snapshot);

        forward = (Button) findViewById(R.id.forward);
        forward.setOnTouchListener(new DirectionOnTouchListener(0x01, 0x0E));

        backward = (Button) findViewById(R.id.backward);
        backward.setOnTouchListener(new DirectionOnTouchListener(0x02, 0x0D));

        right = (Button) findViewById(R.id.right);
        right.setOnTouchListener(new DirectionOnTouchListener(0x04, 0x0B));

        left = (Button) findViewById(R.id.left);
        left.setOnTouchListener(new DirectionOnTouchListener(0x08, 0x07));

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                doControl();
            }
        }, 500, 500, TimeUnit.MILLISECONDS);

        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    heartbeatReceiver = new HeartbeatReceiver(new HeartbeatReceiver.Listener() {
                        @Override
                        public void onHeartbeat(final String host) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hostTextView.setText(host);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                heartbeatReceiver.close();

                if (client != null) {
                    try {
                        client.close();

                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        });

        super.onDestroy();
    }

    public void onVideoStreaming(View view) {
        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String host = hostTextView.getText().toString();

                    client = new MyPiCarClient(host, carClientPort);

                    PiCameraClient pcc = new PiCameraClient(host, cameraClientPort, cameraClientTimeout, new PiCameraClient.Listener() {
                        @Override
                        public void onSnapshot(final byte[] snapshot) {
                            Bitmap b = BitmapFactory.decodeByteArray(snapshot, 0, snapshot.length);

                            Matrix matrix = new Matrix();
                            matrix.postRotate(180f);
                            final Bitmap bitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    snapshotImageView.setImageBitmap(bitmap);
                                }
                            });
                        }
                    });

                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
    }

    void control(final int west, final int east, final long duration) {
        LOG.info("Control - west: {}, east: {}", west, east);

        if (client == null) {
            LOG.warn("MyPiCarClient is not initialized");
            return;
        }

        scheduler.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    client.control(west, east, duration);

                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        });
    }

    public void onForward(View view) {
        control(100, 100, 1000L);
    }

    public void onBackward(View view) {
        control(-100, -100, 1000L);
    }

    public void onRight(View view) {
        control(100, 0, 500L);
    }

    public void onLeft(View view) {
        control(0, 100, 500L);
    }

    boolean is(int mask) {
        return ((direction & mask) > 0);
    }

    void doControl() {
        if (direction != 0) {
            int west = 0;
            int east = 0;

            if (is(0x01)) { // forward
                west = 100;
                east = 100;

                if (is(0x04)) { // right
                    east -= 50;

                } else if (is(0x08)) { // left
                    west -= 50;
                }

            } else if (is(0x02)) { // backward
                west = -100;
                east = -100;

                if (is(0x04)) { // right
                    west += 50;

                } else if (is(0x08)) { // left
                    east += 50;
                }

            } else if (is(0x04)) { // turn right
                west = 100;
                east = 0;

            } else if (is(0x08)) { // turn left
                west = 0;
                east = 100;
            }

            control(west, east, 1000L);
        }
    }

    class DirectionOnTouchListener implements View.OnTouchListener {
        final int down;
        final int up;

        public DirectionOnTouchListener(int down, int up) {
            this.down = down;
            this.up = up;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                direction = direction | down;

            } else if (action == MotionEvent.ACTION_UP) {
                direction = direction & up;
            }

            return false;
        }
    }
}
