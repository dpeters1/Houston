package com.dompeters.houston;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.sccomponents.gauges.ScArcGauge;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    final int NUMBER_OF_CELLS = 12;
    final float MAX_POWER = 2500;
    final float MIN_VOLTAGE = 3.4f;
    final float MAX_VOLTAGE = 4.2f;
    final int REQUEST_TIMEOUT = 1000;
    final int REQUEST_RETRIES = 0;
    final double BATTERY_CAPACITY = 10.4;
    final String TAG_RETAINED_FRAGMENT = "retainedFragment";

    TextView Voltage;
    TextView Current;
    TextView avgConsumption;
    TextView Throttle;
    TextView Steering;
    TextView EscTemp;
    TextView BattTemp;
    TextView connStatus;
    TextView powerMeter;
    TextView batteryMeter;
    TextView tripTime;
    TextView estRemTime;
    TextView graphTitle;
    TextView trimText;
    ScArcGauge powerGauge;
    ScArcGauge batteryGauge;
    houstonGauge hPowerGauge;
    houstonGauge hBatteryGauge;
    SeekBar throttleSlider;
    SeekBar steeringTrimSeekbar;
    SensorManager mSensorManager;
    Sensor mAccel;
    BroadcastReceiver WifiChangeBroadcast;
    FrameLayout connectionBar;
    FrameLayout powerGaugeLayout;
    FrameLayout batteryGaugeLayout;
    RelativeLayout graphLayout;
    LinearLayout telemDataLayout;
    Handler handler;
    RetainedFragment dataFragment;
    DateFormat df;
    houstonGraph powerGraph;
    houstonTelemetry telemetry;
    int runnableCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up ui elements

        if (getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_main);

            trimText = (TextView) findViewById(R.id.trimTextview);
            avgConsumption = (TextView) findViewById(R.id.consumption);
            tripTime = (TextView) findViewById(R.id.tripTime);
            estRemTime = (TextView) findViewById(R.id.remTime);
            steeringTrimSeekbar = (SeekBar) findViewById(R.id.trimSeekbar);
            powerGaugeLayout = (FrameLayout) findViewById(R.id.powerGaugeLayout);
            batteryGaugeLayout = (FrameLayout) findViewById(R.id.batteryGaugeLayout);
            graphLayout = (RelativeLayout) findViewById(R.id.graphLayout);
            telemDataLayout = (LinearLayout) findViewById(R.id.telemLayout);
        }

        else {
            setContentView(R.layout.landscape_controller);
            Steering = (TextView) findViewById(R.id.steering);
            throttleSlider = (SeekBar) findViewById(R.id.throttleSlider);
        }

        // Common elements in portrait and landscape view
        Voltage = (TextView) findViewById(R.id.Voltage);
        Current = (TextView) findViewById(R.id.Current);
        Throttle = (TextView) findViewById(R.id.throttle);
        EscTemp = (TextView) findViewById(R.id.escTemp);
        BattTemp = (TextView) findViewById(R.id.battTemp);
        connStatus = (TextView) findViewById(R.id.conStatus);
        powerMeter = (TextView) findViewById(R.id.powerMeter);
        batteryMeter = (TextView) findViewById(R.id.batteryMeter);
        graphTitle = (TextView) findViewById(R.id.graphTitle);
        powerGauge = (ScArcGauge) findViewById(R.id.powerGauge);
        batteryGauge = (ScArcGauge) findViewById(R.id.batteryGauge);
        connectionBar = (FrameLayout) findViewById(R.id.connectionBar);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        WifiChangeBroadcast = new WifiBroadcastRecv();
        handler = new Handler();
        df = new SimpleDateFormat("HH:mm:ss");

        hPowerGauge = new houstonGauge(powerGauge, 0, MAX_POWER);
        hBatteryGauge = new houstonGauge(batteryGauge, MIN_VOLTAGE, MAX_VOLTAGE);
        df.setTimeZone(TimeZone.getTimeZone("GMT")); // Needs to setup timezone for trip time formatting;

        // Retrieve objects lost during orientation change
        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag(TAG_RETAINED_FRAGMENT);

        if(dataFragment == null) {
            dataFragment = new RetainedFragment();
            fm.beginTransaction().add(dataFragment, TAG_RETAINED_FRAGMENT).commit();

            powerGraph = new houstonGraph(this, MainActivity.this, R.id.powerGraph);
            telemetry = new houstonTelemetry();
            dataFragment.setData(powerGraph, telemetry);
        }
        else {
            telemetry = dataFragment.getTelemData();
            powerGraph = dataFragment.getGraphData();
            powerGraph.attachActivity(this, MainActivity.this, R.id.powerGraph);
        }

        // Run only if screen is in portrait
        if (getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT) {
            telemetry.manualControl = true;
            avgConsumption.setText(String.format("%.0f",telemetry.avgPower));
            tripTime.setText(df.format(new Date(0)));
            estRemTime.setText(df.format(new Date(0)));
            steeringTrimSeekbar.setProgress(telemetry.steeringTrim+10);

            steeringTrimSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int prog = progress - 10;
                    telemetry.steeringTrim = prog; // Middle of seekbar is 10
                    trimText.setText(String.valueOf(prog));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    new Handler().postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            trimText.setText("Steering Trim");
                        }
                    }, (1000));
                }
            });

            tripTime.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    // Reset trip time
                    telemetry.tripTime = System.currentTimeMillis();
                    // flush the old graph values
                    powerGraph.clearData(0);
                    Toast.makeText(MainActivity.this, "Reset trip time",
                            Toast.LENGTH_LONG).show();
                    return true;
                }
            });

            powerGaugeLayout.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onGaugeClick(true);
                }
            });
            batteryGaugeLayout.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    onGaugeClick(true);
                }
            });
        }
        // Run only if screen is in landscape
        else {
            telemetry.manualControl = false;
            throttleSlider.setProgress(telemetry.throttlePosition);
            throttleSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                    telemetry.throttlePosition = progressValue;
                    Throttle.setText(String.valueOf(telemetry.throttlePosition));
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekbar) {
                }

            });
        }
        // Common setters
        Voltage.setText(String.format("%.2f",telemetry.voltage));
        Current.setText(String.format("%.2f",telemetry.current));
        EscTemp.setText(String.format("%.2f",telemetry.escTemp));
        BattTemp.setText(String.format("%.2f",telemetry.battTemp));
        Throttle.setText(String.valueOf(telemetry.throttlePosition));
        telemetry.cellVoltage = telemetry.voltage / NUMBER_OF_CELLS;
        hPowerGauge.setValue((float)telemetry.power, true);
        powerMeter.setText(String.format("%.0f", (float)telemetry.power));
        hBatteryGauge.setValue((float)telemetry.cellVoltage, true);
        batteryMeter.setText(String.format("%.0f", hBatteryGauge.getValuePercent()));
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        double accelData = event.values[1];
        if(accelData > 7) {accelData = 7;}
        if(accelData < -7) {accelData = -7;}

        telemetry.steeringPosition = (int)(map(accelData, -7, 7, 0, 180)+0.5);
        Steering.setText(String.valueOf((telemetry.steeringPosition-90)/2));
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(WifiChangeBroadcast, filter);
        if(!telemetry.manualControl) {
            mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(WifiChangeBroadcast);
        handler.removeCallbacks(pollData);
        if(!telemetry.manualControl) {
            mSensorManager.unregisterListener(this);
            telemetry.manualControl = true;
            sendRequest(false);
        }
    }


    public String getWifiName(Context context) {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return "";
    }

    public void changeStatusColor(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(id));
        }
    }

    public class WifiBroadcastRecv extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String wifiName = getWifiName(MainActivity.this);
            wifiName = wifiName.replaceAll("^\"|\"$", "");
            if (wifiName.contentEquals("Kayak Controller")) {
                connStatus.setText("Connected to Controller");
                connectionBar.setBackgroundColor(getResources().getColor(R.color.greenConn));
                changeStatusColor(R.color.greenConnDark);
                if(telemetry.connectedState == false) { //If
                    telemetry.tripTime = System.currentTimeMillis();
                    powerGraph.clearData(0);
                }
                telemetry.connectedState = true;
                handler.post(pollData);

            } else {
                connStatus.setText("No Connection to Controller");
                connectionBar.setBackgroundColor(getResources().getColor(R.color.gauges));
                changeStatusColor(R.color.gaugesDark);
                telemetry.connectedState = false;
                handler.removeCallbacks(pollData);
            }
        }
    }

    public void sendRequest(boolean getTelemetryResponse) {

        String serverUrl = String.format(Locale.US, "http://192.168.4.1:9701/?telemetry=%b&throttle=%d&steering=%d&trim=%d&manual=%b", getTelemetryResponse, telemetry.throttlePosition,
                telemetry.steeringPosition, telemetry.steeringTrim, telemetry.manualControl);

        //Log.d("HOUSTON", serverUrl);

        StringRequest stringRequest;

        if(getTelemetryResponse) { // If response is required, set up a listener

            stringRequest = new StringRequest(Request.Method.GET, serverUrl,

                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            try {
                                JSONObject jsonObject = new JSONObject(response);

                                telemetry.voltage = Double.valueOf(jsonObject.getString("Voltage"));
                                telemetry.current = Double.valueOf(jsonObject.getString("Current"));
                                telemetry.power = Double.valueOf(jsonObject.getString("Power"));
                                telemetry.battTemp = Double.valueOf(jsonObject.getString("battTemp"));
                                telemetry.escTemp = Double.valueOf(jsonObject.getString("escTemp"));
                                telemetry.avgPower = Double.valueOf(jsonObject.getString("powerAvg"));
                                telemetry.cellVoltage = telemetry.voltage / NUMBER_OF_CELLS;
                                if(telemetry.manualControl) {
                                    telemetry.throttlePosition = Integer.valueOf(jsonObject.getString("throttlePos"));
                                }
                            } catch (JSONException e) {
                                Log.d("HOUSTON", "unexpected JSON exception");
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                    Log.e("HOUSTON", "onErrorResponse: Bad response");
                    error.printStackTrace();

                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Accept", "application/xhtml+xml");

                    return params;
                }
            };
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    REQUEST_TIMEOUT,
                    REQUEST_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        }
        // If not expecting response
        else {
            stringRequest = new StringRequest(Request.Method.GET, serverUrl, null, null)  {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("Accept", "application/xhtml+xml");

                    return params;
                }
            };
        }
        ReqQueueSingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private final Runnable pollData = new Runnable() {
        @Override
        public void run() {
            try {
                updateUI(false); // Only update trip time
                runnableCounter += 1;
                if(runnableCounter == 10) {
                    runnableCounter = 0;
                    updateUI(true);
                    sendRequest(true);
                }
                else if (runnableCounter % 2 == 0) {
                    sendRequest(false); // to speed up outboard requests, don't wait for response
                }

                handler.postDelayed(this, 50);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public double map(double input, double inMin, double inMax, double outMin, double outMax) {
        return ((input - inMin) * (outMax - outMin) / (inMax - inMin) + outMin);
    }

    public void onGaugeClick(boolean showGraph) {

        if (showGraph) {
            int gaugeHeight = powerGaugeLayout.getHeight();
            powerGaugeLayout.setVisibility(View.GONE);
            batteryGaugeLayout.setVisibility(View.GONE);


            graphLayout.getLayoutParams().height = gaugeHeight;
            powerGraph.fitView();
            graphTitle.setText("Power (W) vs Time (s)");
            graphLayout.setVisibility(View.VISIBLE);
            graphTitle.setVisibility(View.VISIBLE);
            powerGraph.animate(1000);
        } else {
            graphLayout.setVisibility(View.GONE);
            graphTitle.setVisibility(View.GONE);
            powerGaugeLayout.setVisibility(View.VISIBLE);
            batteryGaugeLayout.setVisibility(View.VISIBLE);

            hPowerGauge.setValue((float)telemetry.power, true);
            hBatteryGauge.setValue((float)telemetry.cellVoltage, true);
        }
    }

    public void updateUI(boolean telem) {

        long diffMillis = System.currentTimeMillis() - telemetry.tripTime;

        if(telem) {
            float diffSeconds = diffMillis / 1000f;
            powerGraph.addData(diffSeconds, (float) telemetry.power, 0);

            Voltage.setText(String.format("%.2f",telemetry.voltage));
            Current.setText(String.format("%.2f",telemetry.current));
            BattTemp.setText(String.format("%.2f",telemetry.battTemp));
            EscTemp.setText(String.format("%.2f",telemetry.escTemp));
            Throttle.setText(String.valueOf(telemetry.throttlePosition));

            if(hBatteryGauge.getValue() == 0 && telemetry.cellVoltage != 0) {
                hPowerGauge.setValue((float)telemetry.power, true);
                hBatteryGauge.setValue((float)telemetry.cellVoltage, true);
            }
            else {
                hPowerGauge.setValue((float)telemetry.power, false);
                hBatteryGauge.setValue((float)telemetry.cellVoltage, false);
            }
            powerMeter.setText(String.format("%.0f", hPowerGauge.getValue()));
            batteryMeter.setText(String.format("%.0f", hBatteryGauge.getValuePercent()));


            if (telemetry.manualControl) {
                avgConsumption.setText(String.format("%.0f",telemetry.avgPower));
                estRemTime.setText(df.format(new Date(telemetry.estimateRuntime(BATTERY_CAPACITY))));
            }
        }
        else if(telemetry.manualControl) {
            Date difference = new Date(diffMillis);
            tripTime.setText(df.format(difference));
        }
    }
}

