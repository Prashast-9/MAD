package com.example.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int SENSOR_RATE = SensorManager.SENSOR_DELAY_NORMAL;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor lightSensor;
    private Sensor proximitySensor;

    private TextView valueAccelerometerX;
    private TextView valueAccelerometerY;
    private TextView valueAccelerometerZ;
    private TextView valueLight;
    private TextView valueProximity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        valueAccelerometerX = findViewById(R.id.value_accelerometer_x);
        valueAccelerometerY = findViewById(R.id.value_accelerometer_y);
        valueAccelerometerZ = findViewById(R.id.value_accelerometer_z);
        valueLight = findViewById(R.id.value_light);
        valueProximity = findViewById(R.id.value_proximity);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        applyUnavailableUi();
    }

    private void applyUnavailableUi() {
        if (accelerometer == null) {
            String msg = getString(R.string.sensor_not_available);
            valueAccelerometerX.setText(msg);
            valueAccelerometerY.setText(msg);
            valueAccelerometerZ.setText(msg);
        }
        if (lightSensor == null) {
            valueLight.setText(getString(R.string.sensor_not_available));
        }
        if (proximitySensor == null) {
            valueProximity.setText(getString(R.string.sensor_not_available));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager == null) {
            return;
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SENSOR_RATE);
        }
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SENSOR_RATE);
        }
        if (proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SENSOR_RATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        if (values == null || values.length == 0) {
            return;
        }

        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ACCELEROMETER && accelerometer != null) {
            valueAccelerometerX.setText(formatTwoDecimals(values[0]));
            valueAccelerometerY.setText(formatTwoDecimals(values[1]));
            valueAccelerometerZ.setText(formatTwoDecimals(values[2]));
        } else if (type == Sensor.TYPE_LIGHT && lightSensor != null) {
            valueLight.setText(String.format(Locale.US, "%.2f lx", values[0]));
        } else if (type == Sensor.TYPE_PROXIMITY && proximitySensor != null) {
            valueProximity.setText(proximityState(values[0]));
        }
    }

    private String proximityState(float rawValue) {
        // Typical binary proximity: small values (often 0) when near, maxRange when far.
        float maxRange = proximitySensor.getMaximumRange();
        boolean near = maxRange > 0f ? rawValue < maxRange : rawValue <= 0f;
        return near ? getString(R.string.proximity_near) : getString(R.string.proximity_far);
    }

    private static String formatTwoDecimals(float value) {
        return String.format(Locale.US, "%.2f", value);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No UI change required for this app.
    }
}
