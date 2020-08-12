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
import java.util.*;

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private TextView textView;
    private String serverIp;
    private int serverPort;

    final float NS2S = 1.0f / 1000000000.0f;
    private Vector3D acceleration;
    private Vector3D velocity;
    private Vector3D position;
    long acceleratorLastTimestamp = 0;
    int countOfZeroAccelerations = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        serverIp = intent.getStringExtra(MainActivity.EXTRA_IP);
        serverPort = Integer.parseInt(intent.getStringExtra(MainActivity.EXTRA_PORT));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        setContentView(R.layout.activity_game);

        textView = findViewById(R.id.textView);

        initializeSensorValues();
    }

    private void initializeSensorValues() {
        acceleration = new Vector3D();
        velocity = new Vector3D();
        position = new Vector3D();
        acceleratorLastTimestamp = 0;
        countOfZeroAccelerations = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,
//                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        String sensorName = "";
        if (sensorEvent.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            sensorName = "Accelerator";
            if (acceleratorLastTimestamp == 0) {
                acceleratorLastTimestamp = sensorEvent.timestamp;
                return;
            }
            calculateVelocityAndPosition(sensorEvent);
        }

        final String msg = String.format("%s %d\n", position, sensorEvent.timestamp);
        textView.setText(msg);
        sendMessage(msg);
    }

    private void calculateVelocityAndPosition(SensorEvent sensorEvent) {
        float[] values = sensorEvent.values;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(values[i]) < 0.2) {
                values[i] = 0;
            }
        }

        Vector3D newAcceleration = new Vector3D(values);
        if (newAcceleration.isZero()) {
            countOfZeroAccelerations++;
            if (countOfZeroAccelerations > 25) {
                velocity.setZero();
            }
        } else {
            countOfZeroAccelerations = 0;
        }

        float dt = (sensorEvent.timestamp - acceleratorLastTimestamp) * NS2S;
        velocity.add(acceleration.getSum(newAcceleration).getMultipliedBy(dt / 2));
        position.add(velocity.getMultipliedBy(dt));

        acceleration.setZero().add(newAcceleration);
        acceleratorLastTimestamp = sensorEvent.timestamp;
    }

    private void sendMessage(final String message) {

        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            String stringData;

            @Override
            public void run() {
                final Random rand = new Random();
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
                        if (rand.nextInt(1000) < 20
                        ) {
                            System.out.println(message);
                        }
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