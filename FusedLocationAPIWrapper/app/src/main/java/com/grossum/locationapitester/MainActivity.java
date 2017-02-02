package com.grossum.locationapitester;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.BuildConfig;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.grossum.location.LocationApiWrapperService;
import com.grossum.location.LocationTrackingActivity;
import com.grossum.location.LocationUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.grossum.locationapitester.MainActivity.Mode.LOG;
import static com.grossum.locationapitester.MainActivity.Mode.MAP;

public class MainActivity extends LocationTrackingActivity {

    private static final String IS_LOCATION_UPDATING_ARGS = "is_location_updating";
    private static final String MODE_ARGS = "mode";
    private static final String LAST_UPDATE_ARGS = "last_update";
    private static final String LOG_ARGS = "log";

    @BindView(R.id.tv_text)
    TextView tvText;

    @BindView(R.id.tv_ideal_location)
    TextView tvIdealLocation;

    @BindView(R.id.sp_priority)
    Spinner spPriority;

    @BindView(R.id.et_frequency)
    EditText etFrequency;
    @BindView(R.id.et_frequency_max)
    EditText etFrequencyMax;

    @BindView(R.id.btn_start_stop_update)
    Button btnStartStop;

    @BindView(R.id.btn_apply_settings)
    Button btnApplySettings;

    @BindView(R.id.cnt_map)
    View cntMap;
    @BindView(R.id.cnt_logs)
    View cntLogs;


    private GoogleMap googleMap;
    private boolean isLocationUpdating;
    private boolean hasZoomed;
    private Mode mode = MAP;
    private long lastUpdate;

    private String[] priorities = LocationApiWrapperService.PriorityMapPair.getLabelsArray();
    private MarkerOptions markerOptionsCurrent, markerOptionsIdeal;
    private Marker markerCurrent, markerIdeal;
    private SharedPreferencesManager sharedPreferencesManager;

    public enum Mode {
        MAP, LOG
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        PermissionsManager.checkLocationPermissionGranted(this);

        sharedPreferencesManager = new SharedPreferencesManager(this);
        if (BuildConfig.DEBUG){
            hardcodeDebugIdealPosition();
        }

        setupMap();

        tvText.setMovementMethod(new ScrollingMovementMethod());
        spPriority.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities));
        etFrequency.setText(String.valueOf(LocationApiWrapperService.DEFAULT_UPDATE_INTERVAL));
        etFrequencyMax.setText(String.valueOf(LocationApiWrapperService.DEFAULT_UPDATE_FASTEST_INTERVAL));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_change_mode:
                if (mode == MAP) {
                    mode = LOG;
                } else {
                    mode = MAP;
                }
                updateModeView(item);
                break;
            case R.id.btn_add_ideal:
                showIdealPositionDlg();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        updateModeView(menu.findItem(R.id.btn_change_mode));
        return super.onCreateOptionsMenu(menu);
    }

    private void showIdealPositionDlg(){
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this);

        ViewGroup rootView = (ViewGroup) ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dlg_ideal_location, null);
        final EditText etLat = (EditText) rootView.findViewById(R.id.et_lat);
        final EditText etLng = (EditText) rootView.findViewById(R.id.et_lng);

        Float lat = sharedPreferencesManager.getRealLocationLat();
        Float lng = sharedPreferencesManager.getRealLocationLng();
        if (lat != null && lng != null) {
            etLat.setText(String.valueOf(lat));
            etLng.setText(String.valueOf(lng));
        }
        builder.customView(rootView, false);
        builder.positiveText(R.string.save);
        builder.negativeText(android.R.string.cancel);
        builder.onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                try {
                    double lat = Double.parseDouble(etLat.getText().toString());
                    double lng = Double.parseDouble(etLng.getText().toString());

                    sharedPreferencesManager.setRealLocationLat((float) lat);
                    sharedPreferencesManager.setRealLocationLng((float) lng);

                    updateIdealLocation();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, R.string.enter_lat_lng_coord_properly, Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.build().show();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                MainActivity.this.googleMap = googleMap;
                markerOptionsCurrent = new MarkerOptions();
                markerOptionsCurrent.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_location));

                markerOptionsIdeal = new MarkerOptions();
                markerOptionsIdeal.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_ideal_location));

                googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                updateIdealLocation();
            }
        });
    }

    @OnClick(R.id.btn_apply_settings)
    public void onApply(View v) {
        String frequency = etFrequency.getText().toString();
        String fastestFrequency = etFrequencyMax.getText().toString();
        int priorityPosition = spPriority.getSelectedItemPosition();
        try {
            int frequecyInt = Integer.parseInt(frequency);
            int frequecyMax = Integer.parseInt(fastestFrequency);
            LocationApiWrapperService.changeUpdateSettings(this, frequecyInt, frequecyMax, LocationApiWrapperService.PriorityMapPair.values()[priorityPosition].code);
            appendLogText("     --SETTINGS_UPDATED--", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            isLocationUpdating = savedInstanceState.getBoolean(IS_LOCATION_UPDATING_ARGS);
            mode = Mode.valueOf(savedInstanceState.getString(MODE_ARGS));
            lastUpdate = savedInstanceState.getLong(LAST_UPDATE_ARGS);

            tvText.setText(savedInstanceState.getString(LOG_ARGS));
            updateStartUpdateButtonView(isLocationUpdating);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_LOCATION_UPDATING_ARGS, isLocationUpdating);
        outState.putString(MODE_ARGS, mode.name());
        outState.putLong(LAST_UPDATE_ARGS, lastUpdate);
        outState.putString(LOG_ARGS, tvText.getText().toString());
    }

    @Override
    protected void onLastKnownLocation(Location location) {
        updateCurrentLocationMarker(location);
        String locationStr = LocationUtils.getFormattedLatLng(location) + "  " +
                LocationUtils.getFormattedAccuracy(location);
        appendLogText("  Last known: " + locationStr);
    }

    @Override
    protected void onUpdateLocationEvent(Location location) {
        updateCurrentLocationMarker(location);
        String locationStr = LocationUtils.getFormattedLatLng(location) + "  " +
                LocationUtils.getFormattedAccuracy(location);

        String lastUpdateDelta = lastUpdate == 0 ? "" : " (+" + (System.currentTimeMillis() - lastUpdate) + ")";
        appendLogText(lastUpdateDelta + "  Update: " + locationStr);
        lastUpdate = System.currentTimeMillis();
    }

    @Override
    protected void onUpdatingStarted(int interval, int fastestInterval, String priority) {
        appendLogText(generateSettingsLogText(interval, fastestInterval, priority), true);
    }

    @OnClick(R.id.btn_get_last_loc)
    public void onLastLocClick(View v) {
        LocationApiWrapperService.callLastLocation(this);
    }

    private void updateStartUpdateButtonView(boolean isUpdating) {
        if (isUpdating) {
            btnStartStop.setText(R.string.stop_updating);
            btnApplySettings.setEnabled(true);
            btnApplySettings.setAlpha(1);
        } else {
            btnStartStop.setText(R.string.start_updating);
            btnApplySettings.setEnabled(false);
            btnApplySettings.setAlpha(0.6f);
        }
    }

    private void updateModeView(MenuItem item) {
        if (mode == LOG) {
            item.setIcon(R.drawable.ic_map);
            cntLogs.setVisibility(View.VISIBLE);
            cntMap.setVisibility(View.GONE);
        } else {
            item.setIcon(R.drawable.ic_log);
            cntLogs.setVisibility(View.GONE);
            cntMap.setVisibility(View.VISIBLE);
        }
    }


    @OnClick(R.id.btn_start_stop_update)
    public void onStartStopClick(View v) {
        updateStartUpdateButtonView(!isLocationUpdating);
        if (!isLocationUpdating) {
            appendLogText("     --START_UPDATING--- ", true);
            LocationApiWrapperService.startLocationUpdates(this);
        } else {
            appendLogText("     --STOP_UPDATING--- ", true);
            LocationApiWrapperService.stopLocationUpdates(this);
        }
        isLocationUpdating = !isLocationUpdating;
    }

    private String getTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void updateIdealLocation() {
        Float lat = sharedPreferencesManager.getRealLocationLat();
        Float lng = sharedPreferencesManager.getRealLocationLng();
        if (lat != null && lng != null) {
            tvIdealLocation.setText(getString(R.string.ideal_location, String.valueOf(lat), String.valueOf(lng)));
            updateIdealLocationMarker(new LatLng(lat, lng));
        } else {
            tvIdealLocation.setText(getString(R.string.ideal_location, getString(R.string.dash), getString(R.string.dash)));
        }
    }

    private void updateIdealLocationMarker(LatLng latLng) {
        markerOptionsIdeal.position(latLng);
        if (markerIdeal != null) {
            markerIdeal.remove();
        }
        markerIdeal = googleMap.addMarker(markerOptionsIdeal);
    }

    private void updateCurrentLocationMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        markerOptionsCurrent.position(latLng);
        if (markerCurrent != null) {
            markerCurrent.remove();
        }
        markerCurrent = googleMap.addMarker(markerOptionsCurrent);
        if (!hasZoomed) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
            hasZoomed = true;
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    private String generateSettingsLogText(int interval, int fastestInterval, String priority){
        return "     --UPDATE_INTERVAL" + interval + "_ms--- " + "\n" +
                "     --FASTEST_UPDATE_INTERVAL_" + fastestInterval + "_ms--- " + "\n" +
                "     --PRIORITY_" + priority + "--- ";
    }

    private void appendLogText(String text) {
        appendLogText(text, false);
    }

    private void appendLogText(String text, boolean withMargin) {
        String appendText = withMargin ?
                ("\n" + getTimeFormatted() + text + "\n\n") :
                (getTimeFormatted() + text + "\n");
        tvText.setText(tvText.getText() + appendText);
    }

    private void hardcodeDebugIdealPosition(){
        sharedPreferencesManager.setRealLocationLat(50.41817f);
        sharedPreferencesManager.setRealLocationLng(30.51710f);
    }
}
