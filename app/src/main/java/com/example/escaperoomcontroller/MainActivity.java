package com.example.escaperoomcontroller;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.net.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        setContentView(R.layout.activity_main);

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
                    final InetAddress serverAddr = InetAddress.getByName("192.168.1.9");
                    final DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.length(), serverAddr, 9001);
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