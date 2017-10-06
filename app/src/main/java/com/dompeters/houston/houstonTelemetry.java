package com.dompeters.houston;

/**
 * Created by Dominic on 2017-09-11.
 */

public class houstonTelemetry {

    public int throttlePosition;
    public int steeringPosition;
    public int steeringTrim;
    public double voltage;
    public double current;
    public double power;
    public double avgPower;
    public double battTemp;
    public double escTemp;
    public double cellVoltage;
    public long tripTime;
    public boolean manualControl;
    public boolean connectedState;

    public houstonTelemetry() {}

    public long estimateRuntime(double batteryCapacity) {
        double cellValuePercent = (double)houstonGauge.mapPercent((float)cellVoltage, 3.4f, 4.2f);

        double capacityLeft = batteryCapacity * cellValuePercent * voltage / 100;
        if(avgPower < 10){
            return 0;
        }
        double timeLeftMills = capacityLeft/avgPower * 3600 * 1000;
        return (long)timeLeftMills;
    }
}
