package com.github.ericksonalves.uavcontroller.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ericksonalves.uavcontroller.R;
import com.github.ericksonalves.uavcontroller.planner.Planner;
import com.github.ericksonalves.uavcontroller.planner.PlannerListener;
import com.github.ericksonalves.uavcontroller.util.Utils;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionResult;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.VehicleMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements DroneListener, PlannerListener, TowerListener {
    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;
    @BindView(R.id.button_connect)
    public Button connectButton;
    @BindView(R.id.button_disconnect)
    public Button disconnectButton;
    @BindView(R.id.button_start_production)
    public Button startProductionButton;
    @BindView(R.id.edit_text_amount_of_x)
    public EditText amountOfXEditText;
    @BindView(R.id.edit_text_amount_of_y)
    public EditText amountOfYEditText;
    @BindView(R.id.spinner_connection_type)
    public Spinner connectionTypeSpinner;
    @BindView(R.id.text_view_altitude)
    public TextView altitudeTextView;
    @BindView(R.id.text_view_battery)
    public TextView batteryTextView;
    @BindView(R.id.text_view_position)
    public TextView positionTextView;
    @BindView(R.id.text_view_produced_x)
    public TextView producedXTextView;
    @BindView(R.id.text_view_produced_y)
    public TextView producedYTextView;
    @BindView(R.id.text_view_speed)
    public TextView speedTextView;
    private ControlTower mControlTower;
    private Drone mDrone;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mControlTower = new ControlTower(this);
        mDrone = new Drone();
        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateTelemetryValues(0.0, 0.0, 0.0, 0.0, 0.0);
        toggleConnectionButtons(true);
        mControlTower.connect(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mDrone.isConnected()) {
            mDrone.disconnect();
            toggleConnectionButtons(true);
        }
        mControlTower.unregisterDrone(mDrone);
        mControlTower.disconnect();
    }

    @OnClick(R.id.button_connect)
    public void onConnectButtonClicked() {
        int selectedConnectionType = connectionTypeSpinner.getSelectedItemPosition();
        Bundle extraParams = new Bundle();
        if (selectedConnectionType == ConnectionType.TYPE_USB) {
            extraParams.putInt(ConnectionType.EXTRA_USB_BAUD_RATE, DEFAULT_USB_BAUD_RATE);
        } else {
            extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, DEFAULT_UDP_PORT);
        }
        ConnectionParameter connectionParams = new ConnectionParameter(selectedConnectionType, extraParams, null);
        mDrone.connect(connectionParams);
    }

    @OnClick(R.id.button_disconnect)
    public void onDisconnectButtonClicked() {
        mDrone.disconnect();
    }

    @OnClick(R.id.button_start_production)
    public void onStartProductionButtonClicked() {
        amountOfXEditText.setEnabled(false);
        amountOfYEditText.setEnabled(false);
        startProductionButton.setEnabled(false);
        updateProduction(0, 0);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                double latitude = 0.0;
                double longitude = 0.0;
                Gps gps = mDrone.getAttribute(AttributeType.GPS);
                if (gps != null && gps.getPosition() != null) {
                    latitude = gps.getPosition().getLatitude();
                    longitude = gps.getPosition().getLongitude();
                }
                Planner planner = new Planner(latitude, longitude);
                int x = Integer.parseInt(amountOfXEditText.getText().toString());
                int y = Integer.parseInt(amountOfYEditText.getText().toString());
                planner.addListener(MainActivity.this);
                planner.plan(mDrone, x, y);
                planner.removeListener(MainActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        amountOfXEditText.setEnabled(true);
                        amountOfYEditText.setEnabled(true);
                        startProductionButton.setEnabled(true);
                    }
                });
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void toggleConnectionButtons(boolean state) {
        connectButton.setEnabled(state);
        disconnectButton.setEnabled(!state);
        amountOfXEditText.setEnabled(!state);
        amountOfYEditText.setEnabled(!state);
        startProductionButton.setEnabled(!state);
    }

    private void updateAltitude(double altitude) {
        altitudeTextView.setText(String.format(getResources().getString(R.string.altitude_format),
                String.valueOf(Utils.round(altitude, 3))));
    }

    private void updateBattery(double battery) {
        batteryTextView.setText(String.format(getResources().getString(R.string.battery_format), String.valueOf(Utils
                .round(battery, 3))));
    }

    private void updatePosition(double latitude, double longitude) {
        positionTextView.setText(String.format(getResources().getString(R.string.position_format),
                String.valueOf(latitude), String.valueOf(longitude)));
    }

    private void updateProduction(int x, int y) {
        producedXTextView.setText(String.valueOf(x));
        producedYTextView.setText(String.valueOf(y));
    }

    private void updateSpeed(double speed) {
        speedTextView.setText(String.format(getResources().getString(R.string.speed_format),
                String.valueOf(Utils.round(speed, 3))));
    }

    private void updateTelemetryValues(double altitude, double battery, double latitude, double longitude,
                                       double speed) {
        altitudeTextView.setText(String.format(getResources().getString(R.string.altitude_format),
                String.valueOf(altitude)));
        batteryTextView.setText(String.format(getResources().getString(R.string.battery_format), String.valueOf(battery)));
        positionTextView.setText(String.format(getResources().getString(R.string.position_format),
                String.valueOf(latitude), String.valueOf(longitude)));
        speedTextView.setText(String.format(getResources().getString(R.string.speed_format),
                String.valueOf(speed)));
    }

    @Override
    public void onDroneConnectionFailed(ConnectionResult result) {
        showToast("Connection failed: " + result.getErrorMessage());
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                showToast("Drone Connected");
                mDrone.changeVehicleMode(VehicleMode.COPTER_GUIDED);
                toggleConnectionButtons(false);
                break;
            case AttributeEvent.STATE_DISCONNECTED:
                showToast("Drone Disconnected");
                toggleConnectionButtons(mDrone.isConnected());
                break;
            case AttributeEvent.SPEED_UPDATED:
                Speed speed = mDrone.getAttribute(AttributeType.SPEED);
                updateSpeed(speed.getGroundSpeed());
                break;
            case AttributeEvent.ALTITUDE_UPDATED:
                Altitude altitude = mDrone.getAttribute(AttributeType.ALTITUDE);
                updateAltitude(altitude.getAltitude());
                break;
            case AttributeEvent.BATTERY_UPDATED:
                Battery battery = mDrone.getAttribute(AttributeType.BATTERY);
                updateBattery(battery.getBatteryCurrent());
                break;
            default:
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {
    }

    @Override
    public void onTowerConnected() {
        showToast("3DR Services Connected");
        mControlTower.registerDrone(mDrone, mHandler);
        mDrone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        showToast("3DR Services Interrupted");
    }

    @Override
    public void onDelivered(final String product) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int x = Integer.parseInt(producedXTextView.getText().toString());
                int y = Integer.parseInt(producedYTextView.getText().toString());
                if (product.equals("X")) {
                    x++;
                } else {
                    y++;
                }
                updateProduction(x, y);
            }
        });
    }

    @Override
    public void onPositionUpdated(final double latitude, final double longitude) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updatePosition(latitude, longitude);
            }
        });
    }
}
