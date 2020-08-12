package com.example.escaperoomcontroller;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private TextView textView;
    private String serverIp;
    private int serverPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        serverIp = intent.getStringExtra(MainActivity.EXTRA_IP);
        serverPort = Integer.parseInt(intent.getStringExtra(MainActivity.EXTRA_PORT));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        setContentView(R.layout.activity_game);

        textView = findViewById(R.id.textView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
//                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        float[] values = sensorEvent.values;
        final String msg = String.format("%f %f %f %d\n", -1 * values[0], values[1], values[2], sensorEvent.timestamp);
        textView.setText(msg);
        sendMessage(msg);
    }

    private void sendMessage(final String message) {

        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            String stringData;

            @Override
            public void run() {
                try (DatagramSocket datagramSocket = new DatagramSocket()) {
                    // IP Address below is the IP address of that Device where server socket is opened.
                    final InetAddress serverAddr = InetAddress.getByName(serverIp);
                    final DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.length(), serverAddr, serverPort);
                    datagramSocket.send(datagramPacket);

//                    byte[] lMsg = new byte[1000];
//                    final DatagramPacket resp = new DatagramPacket(lMsg, lMsg.length);
//                    datagramSocket.receive(resp);
//                    stringData = new String(lMsg, 0, resp.getLength());

                } catch (IOException e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println(message);
                    }
                });
            }
        });

        thread.start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}