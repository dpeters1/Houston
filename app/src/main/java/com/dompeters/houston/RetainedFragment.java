package com.dompeters.houston;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Created by Dominic on 2017-09-09.
 */

public class RetainedFragment extends Fragment {
    private houstonGraph powerGraph;
    private houstonTelemetry telemetry;

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    public void setData(houstonGraph _powerGraph, houstonTelemetry _telemetry) {
        this.powerGraph = _powerGraph;
        this.telemetry = _telemetry;
    }

    public houstonGraph getGraphData() {
        return powerGraph;
    }

    public houstonTelemetry getTelemData() {
        return telemetry;
    }
}
