package com.example.escaperoomcontroller;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
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
    final float CALIBRATION_DURATION = 10;

    private boolean isStarted = false;
    private boolean isCalibrating = false;

    private long accelerationCalibrateStartTimestamp = 0;
    private long accelerationCalibrateCounter = 0;
    private long gameRotationCalibrateStartTimestamp = 0;
    private long gameRotationCalibrateCounter = 0;

    private Vector3D accelerationBias;
    private Vector3D accelerationBiasMin;
    private Vector3D accelerationBiasMax;
    private Vector3D acceleration;
    private Vector3D velocity;
    private Vector3D position;
    private long acceleratorLastTimestamp = 0;

    private long gameRotationLastTimestamp = 0;
    private float[] calibrationRotationMatrix;
    private float[] lastRotationMatrix;
    private Vector3D currentRotationAngle;
    private Vector3D lastRotationAngle;

    private boolean shouldAccelerometerUpdateBeSent = false;
    private boolean shouldRotationUpdateBeSent = false;
    private boolean isBeingTouched = false;

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
        accelerationBias = new Vector3D();
        accelerationBiasMin = new Vector3D();
        accelerationBiasMax = new Vector3D();

        lastRotationMatrix = new float[9];
        calibrationRotationMatrix = new float[9];
        Arrays.fill(calibrationRotationMatrix, 0);
        currentRotationAngle = new Vector3D();
        lastRotationAngle = new Vector3D();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_GAME);

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
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

        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                handleAccelerationSensorEvent(sensorEvent);
                if (!shouldAccelerometerUpdateBeSent) {
                    shouldAccelerometerUpdateBeSent = true;
                    return;
                }
                shouldAccelerometerUpdateBeSent = false;
                break;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                handleGameRotationSensorEvent(sensorEvent);
                if (!shouldRotationUpdateBeSent) {
                    shouldRotationUpdateBeSent = true;
                    return;
                }
                shouldRotationUpdateBeSent = false;
                break;
        }

        if (isStarted) {
            final String msg = String.format("%s%s%s%s", acceleration, currentRotationAngle, lastRotationAngle, isBeingTouched ? '1' : '0');
            textView.setText(msg);
            sendMessage(msg);
        }
    }

    private void handleAccelerationSensorEvent(SensorEvent sensorEvent) {
        if (isCalibrating) {
            if (accelerationCalibrateStartTimestamp == 0) {
                accelerationCalibrateStartTimestamp = sensorEvent.timestamp;
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
            accelerationCalibrateCounter++;
            if ((sensorEvent.timestamp - accelerationCalibrateStartTimestamp) * NS2S > CALIBRATION_DURATION) {
                accelerationBias.multiply(-1f / accelerationCalibrateCounter);
                isCalibrating = false;
                isStarted = true;
            }
        } else if (isStarted) {
            if (acceleratorLastTimestamp == 0) {
                acceleratorLastTimestamp = sensorEvent.timestamp;
                return;
            }

            UpdateAcceleration(sensorEvent);
        }
    }

    private void handleGameRotationSensorEvent(SensorEvent sensorEvent) {
        float[] values = sensorEvent.values;

        if (isCalibrating) {
            if (gameRotationCalibrateStartTimestamp == 0) {
                gameRotationCalibrateStartTimestamp = sensorEvent.timestamp;
                return;
            }

            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, values);
            for (int i = 0; i < rotationMatrix.length; i++) {
                calibrationRotationMatrix[i] += rotationMatrix[i];
            }

            gameRotationCalibrateCounter++;
            if ((sensorEvent.timestamp - gameRotationCalibrateStartTimestamp) * NS2S > CALIBRATION_DURATION) {
                for (int i = 0; i < rotationMatrix.length; i++) {
                    calibrationRotationMatrix[i] /= gameRotationCalibrateCounter;
                }

                isCalibrating = false;
                isStarted = true;
            }
        } else if (isStarted) {
            if (gameRotationLastTimestamp == 0) {
                gameRotationLastTimestamp = sensorEvent.timestamp;
                SensorManager.getRotationMatrixFromVector(lastRotationMatrix, values);
                return;
            }


            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, values);

            float[] angleChange = new float[3];
            // ANGLE CHANGE : (z, x, y)
            SensorManager.getAngleChange(angleChange, rotationMatrix, calibrationRotationMatrix);
            for (int i = 0; i < angleChange.length; i++) {
                angleChange[i] *= (180 / Math.PI);
                if (Math.abs(angleChange[i]) < 0.2) {
                    angleChange[i] = 0;
                }
            }
            currentRotationAngle = new Vector3D(angleChange[1], angleChange[2], angleChange[0]);

            SensorManager.getAngleChange(angleChange, rotationMatrix, lastRotationMatrix);
            for (int i = 0; i < angleChange.length; i++) {
                angleChange[i] *= (180 / Math.PI);
                if (Math.abs(angleChange[i]) < 0.2) {
                    angleChange[i] = 0;
                }
            }
            lastRotationAngle = new Vector3D(angleChange[1], angleChange[2], angleChange[0]);

            lastRotationMatrix = rotationMatrix;
            gameRotationLastTimestamp = sensorEvent.timestamp;
        }
    }

    private void UpdateAcceleration(SensorEvent sensorEvent) {
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

        float dt = (sensorEvent.timestamp - acceleratorLastTimestamp) * NS2S;
        velocity.add(newAcceleration.getMultipliedBy(dt));
        if (velocity.getLength() > .1) {
            position.add(velocity.getMultipliedBy(dt));
        }
        velocity.multiply(0.9f);
        if (velocity.getLength() < 0.001) {
            velocity.setZero();
        }

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
                    // IP Address below is the IP address of that Device where server socket is
                    // opened.
                    final InetAddress serverAddr = InetAddress.getByName(serverIp);
                    final DatagramPacket datagramPacket = new DatagramPacket(message.getBytes(), message.length(),
                            serverAddr, serverPort);
                    datagramSocket.send(datagramPacket);

                    // byte[] lMsg = new byte[1000];
                    // final DatagramPacket resp = new DatagramPacket(lMsg, lMsg.length);
                    // datagramSocket.receive(resp);
                    // stringData = new String(lMsg, 0, resp.getLength());

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
        accelerationCalibrateCounter = 0;
        accelerationCalibrateStartTimestamp = 0;
        acceleratorLastTimestamp = 0;

        gameRotationCalibrateCounter = 0;
        gameRotationCalibrateStartTimestamp = 0;
        gameRotationLastTimestamp = 0;

        isBeingTouched = false;

        initializeSensorValues();
        isCalibrating = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isBeingTouched = true;
                break;
            case MotionEvent.ACTION_UP:
                isBeingTouched = false;
                break;
        }
        return super.onTouchEvent(event);
    }
}
