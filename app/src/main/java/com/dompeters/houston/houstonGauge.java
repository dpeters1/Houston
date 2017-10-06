package com.dompeters.houston;

import android.animation.ValueAnimator;
import android.graphics.Paint;
import android.os.Handler;

import com.sccomponents.gauges.ScArcGauge;

/**
 * Created by Dominic on 2017-09-09.
 */

public class houstonGauge {
    private ScArcGauge gauge;
    private float value;
    private float valuePercent;
    private float lowValue;
    private float highValue;
    private boolean isBusy;

    public houstonGauge(ScArcGauge _gauge, float _lowValue, float _highValue) {

        gauge = _gauge;
        lowValue = _lowValue;
        highValue = _highValue;
        isBusy = false;
        gauge.findFeature(ScArcGauge.BASE_IDENTIFIER)
                .getPainter().setStrokeCap(Paint.Cap.ROUND);
        gauge.findFeature(ScArcGauge.PROGRESS_IDENTIFIER)
                .getPainter().setStrokeCap(Paint.Cap.ROUND);

        gauge.setAngleStart(90);
    }

    public void setValue(float _value, boolean fullAnimation) {

        if(isBusy){return;}

        isBusy = true;

        value = _value;

        valuePercent = (mapPercent(value, this.lowValue, this.highValue));
        ValueAnimator animator = (ValueAnimator)gauge.getHighValueAnimator();
        int duration = 0;
        if(fullAnimation){
            duration = 1500;
            animator.setFloatValues(0, valuePercent);
        }
        else {animator.setFloatValues(gauge.getHighValue(), valuePercent);}

        animator.setDuration(duration);
        animator.start();

        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run() {isBusy = false;}
        }, (duration));
    }

    public float getValue() {
        return value;
    }

    public float getValuePercent() {
        return valuePercent;
    }

    public static float mapPercent(float value, float lowValue, float highValue) {
        float valuePercent = (value - lowValue) * (100) / (highValue - lowValue);
        if(valuePercent < 0){
            valuePercent = 0;
        }
        if(valuePercent > 100) {
            valuePercent = 100;
        }
        return valuePercent;
    }
}
