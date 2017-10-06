package com.dompeters.houston;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Created by Dominic on 2017-07-28.
 */

public class ReqQueueSingleton {
    private static ReqQueueSingleton mInstance;
    private RequestQueue mRequestQueue;
    private Context mCtx;

    private ReqQueueSingleton(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();

    }

    public static synchronized ReqQueueSingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ReqQueueSingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
            Log.d("HOUSTON", "Created new RequestQueue");
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

}

