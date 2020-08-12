package com.example.escaperoomcontroller;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
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
    private boolean isCalibrating = false;
    long calibrateStartTimestamp = 0;
    long calibrateCounter = 0;
    private boolean isStarted = false;
    private Vector3D accelerationBias;
    private Vector3D accelerationBiasMin;
    private Vector3D accelerationBiasMax;
    private Vector3D acceleration;
    private Vector3D velocity;
    private Vector3D position;
    private Vector3D tempAcceleration;
    long acceleratorLastTimestamp = 0;
    int countOfZeroAccelerations = 0;
    int positionChangeCounter = 0;

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
        tempAcceleration = new Vector3D();
        accelerationBias = new Vector3D();
        accelerationBiasMin = new Vector3D();
        accelerationBiasMax = new Vector3D();
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
            if (isCalibrating) {
                if (calibrateStartTimestamp == 0) {
                    calibrateStartTimestamp = sensorEvent.timestamp;
                    return;
                }

                float[] values = sensorEvent.values;
                if (values[0] > accelerationBiasMax.getX()) {
                    accelerationBiasMax.setX(values[0]);
                }
                if (values[1] > accelerationBiasMax.getY()) {
                    accelerationBiasMax.setY(values[1]);
                }
                if (values[2] > accelerationBiasMax.getZ()) {
                    accelerationBiasMax.setZ(values[2]);
                }

                if (values[0] < accelerationBiasMin.getX()) {
                    accelerationBiasMin.setX(values[0]);
                }
                if (values[1] < accelerationBiasMin.getY()) {
                    accelerationBiasMin.setY(values[1]);
                }
                if (values[2] < accelerationBiasMin.getZ()) {
                    accelerationBiasMin.setZ(values[2]);
                }

                accelerationBias.add(new Vector3D(sensorEvent.values));
                calibrateCounter++;
                if ((sensorEvent.timestamp - calibrateStartTimestamp) * NS2S > 10) {
                    accelerationBias.multiply(-1f / calibrateCounter);
                    isCalibrating = false;
                    isStarted = true;
                }
            } else if (isStarted) {
                if (acceleratorLastTimestamp == 0) {
                    acceleratorLastTimestamp = sensorEvent.timestamp;
                    return;
                }

                boolean isChanged = calculateVelocityAndPosition(sensorEvent);
                if (isChanged) {
//                    final String msg = String.format("\n%s\n%s\n%s\n%s\n %d\n", position, velocity, acceleration, accelerationBias, sensorEvent.timestamp);
                    final String msg = String.format("%s", velocity);
                    textView.setText(msg);
                    sendMessage(msg);
                }
            }
        }
    }

    private boolean calculateVelocityAndPosition(SensorEvent sensorEvent) {
        float[] values = sensorEvent.values;

        if (values[0] < accelerationBiasMax.getX() && values[0] > accelerationBiasMin.getX()) {
            values[0] = 0;
        }
        if (values[1] < accelerationBiasMax.getY() && values[1] > accelerationBiasMin.getY()) {
            values[1] = 0;
        }
        if (values[2] < accelerationBiasMax.getZ() && values[2] > accelerationBiasMin.getZ()) {
            values[2] = 0;
        }

        Vector3D newAcceleration = new Vector3D(values);

//        Vector3D newAcceleration = new Vector3D(values).add(accelerationBias);
//
//        if (Math.abs(newAcceleration.getX()) < 0.1) {
//            newAcceleration.setX(0f);
//        }
//        if (Math.abs(newAcceleration.getY()) < 0.1) {
//            newAcceleration.setY(0f);
//        }
//        if (Math.abs(newAcceleration.getZ()) < 0.1) {
//            newAcceleration.setZ(0f);
//        }

        tempAcceleration.add(newAcceleration);
        positionChangeCounter++;
        if (positionChangeCounter >= 1) {
            if (tempAcceleration.isZero()) {
                countOfZeroAccelerations++;
//                if (countOfZeroAccelerations > 25) {
//                    velocity.setZero();
//                }
            } else {
                countOfZeroAccelerations = 0;
            }

            tempAcceleration.multiply(1f / positionChangeCounter);
            float dt = (sensorEvent.timestamp - acceleratorLastTimestamp) * NS2S;
//            velocity.add(acceleration.getSum(tempAcceleration).getMultipliedBy(dt / 2));
            velocity.add(tempAcceleration.getMultipliedBy(dt));
            if (velocity.getLength() > .1) {
                position.add(velocity.getMultipliedBy(dt));
            }
            velocity.multiply(0.9f);
            if (velocity.getLength() < 0.001) {
                velocity.setZero();
            }

            acceleration.setZero().add(tempAcceleration);
            acceleratorLastTimestamp = sensorEvent.timestamp;
            tempAcceleration.setZero();
            positionChangeCounter = 0;
            return true;
        }
        return false;
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
                        if (rand.nextInt(1000) < 200) {
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

    public void calibrate(View view) {
        calibrateCounter = 0;
        isCalibrating = true;
        initializeSensorValues();
    }
}
