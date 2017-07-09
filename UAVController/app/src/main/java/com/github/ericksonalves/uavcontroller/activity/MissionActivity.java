package com.github.ericksonalves.uavcontroller.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ericksonalves.uavcontroller.R;
import com.github.ericksonalves.uavcontroller.planner.Planner;
import com.github.ericksonalves.uavcontroller.planner.PlannerListener;
import com.github.ericksonalves.uavcontroller.util.Utils;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MissionActivity extends AppCompatActivity implements OnMapReadyCallback, PlannerListener {
    @BindView(R.id.text_view_production_position)
    public TextView positionTextView;
    @BindView(R.id.text_view_produced_x)
    public TextView producedXTextView;
    @BindView(R.id.text_view_produced_y)
    public TextView producedYTextView;
    private GoogleMap mGoogleMap;
    private MarkerOptions mDroneMarker;
    private int mX;
    private int mY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);
        ButterKnife.bind(this);
        int production[] = getIntent().getIntArrayExtra(Utils.PRODUCTION);
        mX = production[0];
        mY = production[1];
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        double latitude = -3.098071;
        double longitude = -59.975839;
        final Planner planner = new Planner(latitude, longitude);
        for (LatLng point : planner.getPoints()) {
            googleMap.addMarker(new MarkerOptions().position(point));
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50);
        mGoogleMap.animateCamera(cameraUpdate);
        mDroneMarker = new MarkerOptions().position(new LatLng(latitude, longitude)).icon
                (BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        mGoogleMap.addMarker(mDroneMarker);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                planner.addListener(MissionActivity.this);
                planner.plan(MainActivity.sDrone, mX, mY);
                planner.removeListener(MissionActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("Finished");
                    }
                });
            }
        });
    }

    private void updateProduction(int x, int y) {
        producedXTextView.setText(String.valueOf(x));
        producedYTextView.setText(String.valueOf(y));
    }

    private void updatePosition(double latitude, double longitude) {
        positionTextView.setText(String.format(getResources().getString(R.string.position_format),
                String.valueOf(latitude), String.valueOf(longitude)));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
