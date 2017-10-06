package com.dompeters.houston;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dominic on 2017-09-08.
 */

public class houstonGraph {

    private boolean isInteracting;
    private long scalingAnimationDelay;
    private int zoomedExtents;
    private float x;
    private float y;
    private List<Entry> telemPoints;
    private LineDataSet telemDataSet;
    private LineData telemData;
    private LineChart telemGraph;
    private YAxis yAxisLeft;
    private  YAxis yAxisRight;
    private XAxis xAxis;
    public Activity activity;
    public MainActivity mainActivity;

    public houstonGraph(Activity _activity, MainActivity _mainActivity, int graphID) {

        activity = _activity;
        mainActivity = _mainActivity;

        telemPoints = new ArrayList<>();
        telemDataSet = new LineDataSet(telemPoints, "Power");
        telemData = new LineData(telemDataSet);
        telemGraph = (LineChart)this.activity.findViewById(graphID);
        yAxisLeft = telemGraph.getAxisLeft();
        yAxisRight = telemGraph.getAxisRight();
        xAxis = telemGraph.getXAxis();
        zoomedExtents = 16;


        telemGraph.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                isInteracting = true;
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

                mainActivity.onGaugeClick(false);
                isInteracting = false;
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });

        telemDataSet.setDrawFilled(true);
        Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.graph_gradient);
        telemDataSet.setFillDrawable(drawable);
        telemDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        telemDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        telemDataSet.setColor(Color.BLACK);
        telemDataSet.setDrawCircles(false);
        telemDataSet.setDrawValues(false);
        telemDataSet.setLineWidth(0f);
        telemDataSet.setHighlightEnabled(false);
        yAxisLeft.setDrawGridLines(false);
        yAxisRight.setDrawGridLines(false);
        xAxis.setDrawGridLines(false);
        yAxisLeft.setDrawAxisLine(false);
        yAxisRight.setDrawAxisLine(false);
        xAxis.setDrawAxisLine(false);
        telemGraph.getLegend().setEnabled(false);
        telemGraph.getDescription().setEnabled(false);

        addData(0, 0, 0);
    }
    
    public void addData(float _x, float _y, int index) {
        x = _x;
        y = _y;
        telemData.addEntry(new Entry(x, y), index);

        if((System.currentTimeMillis() - scalingAnimationDelay) < 2000) {return;} // Don't fuck with graph while animating

        telemGraph.setData(telemData);
        telemGraph.notifyDataSetChanged();

        if(isInteracting) {
            telemGraph.setVisibleXRangeMaximum(1000);
        }
        else {
            telemGraph.setVisibleXRangeMaximum(zoomedExtents);
            telemGraph.moveViewToX(x-zoomedExtents);
        }

        telemGraph.invalidate();
    }

    public void animate(final int duration) {
        scalingAnimationDelay = System.currentTimeMillis();
        telemGraph.animateX(duration, Easing.EasingOption.EaseInSine);
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                float xScale = telemGraph.getVisibleXRange()/zoomedExtents;
                //telemGraph.zoomAndCenterAnimated(xScale,1,x-zoomedExtents,y, YAxis.AxisDependency.LEFT, duration);
                telemGraph.zoomAndCenterAnimated(xScale,1,0,0, YAxis.AxisDependency.LEFT, duration);
                telemGraph.moveViewToAnimated(x-zoomedExtents, y, YAxis.AxisDependency.LEFT, (duration*2));
            }
        }, (duration));
    }

    public void clearData(int index){
        telemDataSet.clear();
        addData(0,0,index);
        telemGraph.moveViewToX(0);
    }
    public void fitView(){
        telemGraph.fitScreen();
    }

    // When using a fragment to retain the graph object through activity re-creations, the graph needs
    // to re-attach to the current activity. Generally it's bad practice to pass objects that are tied to activities
    // in fragments because it screws with garbage collection, and I should feel ashamed.
    public void attachActivity(Activity _activity, MainActivity _mainActivity, int graphID) {
        activity = _activity;
        mainActivity = _mainActivity;

        telemGraph = (LineChart)this.activity.findViewById(graphID);
        yAxisLeft = telemGraph.getAxisLeft();
        yAxisRight = telemGraph.getAxisRight();
        xAxis = telemGraph.getXAxis();

        telemGraph.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                isInteracting = true;
            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

                mainActivity.onGaugeClick(false);
                isInteracting = false;
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });

        Drawable drawable = ContextCompat.getDrawable(activity, R.drawable.graph_gradient);
        telemDataSet.setFillDrawable(drawable);

        yAxisLeft.setDrawGridLines(false);
        yAxisRight.setDrawGridLines(false);
        xAxis.setDrawGridLines(false);
        yAxisLeft.setDrawAxisLine(false);
        yAxisRight.setDrawAxisLine(false);
        xAxis.setDrawAxisLine(false);
        telemGraph.getLegend().setEnabled(false);
        telemGraph.getDescription().setEnabled(false);
    }
}

