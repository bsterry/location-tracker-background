package br.com.safety.locationlistenerhelper.core;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.github.kayvannj.permission_utils.Func2;
import com.github.kayvannj.permission_utils.PermissionUtil;

import java.io.Serializable;

/**
 * @author netodevel
 */
public class LocationTracker implements Serializable {

    /**
     * ask permissions
     */
    private PermissionUtil.PermissionRequestObject mBothPermissionRequest;

    /**
     * interval to send gps data
     */
    private long interval = 0;

    /**
     * minimum displacement for location updates
     */
    private long smallestDisplacement = 0;

    /**
     * maximum time to wait before delivering batched location updates
     */
    private long maxWaitTime = 0;

    /**
     * name of action to send gps data
     * for your broadcast receiver
     */
    private String actionReceiver;

    /**
     * use gps provider
     */
    private Boolean gps;

    /**
     * run the location service in the foreground
     */
    private Boolean runInForeground = false;

    /**
     * use network provider
     */
    private Boolean netWork;


    private String foregroundNotificationTitle;
    private String foregroundNotificationText;
    private String foregroundNotificationTicker;
    private String foregroundNotificationChannelId;

    /**
     * broadcast to get current location
     */
    private BroadcastReceiver currentLocationReceiver;

    public LocationTracker(String actionReceiver) {
        this.actionReceiver = actionReceiver;
    }

    /**
     * Set currentLocationReceiver to get current location data
     * @param currentLocationReceiver
     */
    public LocationTracker currentLocation(BroadcastReceiver currentLocationReceiver) {
        this.currentLocationReceiver = currentLocationReceiver;
        return this;
    }

    public LocationTracker setInterval(long value) {
        this.interval = value;
        return this;
    }

    public LocationTracker setSmallestDisplacement(long value) {
        this.smallestDisplacement = value;
        return this;
    }

    public LocationTracker setMaxWaitTime(long value) {
        this.maxWaitTime = value;
        return this;
    }

    public LocationTracker setGps(Boolean value) {
        this.gps = value;
        return this;
    }

    public LocationTracker setRunInForeground(Boolean value) {
        this.runInForeground = value;
        return this;
    }

    public LocationTracker setNetWork(Boolean netWork) {
        this.netWork = netWork;
        return this;
    }

    public LocationTracker setForegroundNotificationTitle(String value) {
        this.foregroundNotificationTitle = value;
        return this;
    }

    public LocationTracker setForegroundNotificationText(String value) {
        this.foregroundNotificationText = value;
        return this;
    }

    public LocationTracker setForegroundNotificationTicker(String value) {
        this.foregroundNotificationTicker = value;
        return this;
    }

    public LocationTracker setForegroundNotificationChannelId(String value) {
        this.foregroundNotificationChannelId = value;
        return this;
    }

    public LocationTracker start(Context context, AppCompatActivity appCompatActivity) {
        validatePermissions(context, appCompatActivity);

        if (this.currentLocationReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(SettingsLocationTracker.ACTION_CURRENT_LOCATION_BROADCAST);
            intentFilter.addAction(SettingsLocationTracker.ACTION_PERMISSION_DEINED);
            context.registerReceiver(this.currentLocationReceiver, intentFilter);
        }

        return this;
    }

    public LocationTracker start(Context context) {
        startLocationService(context);

        if (this.currentLocationReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(SettingsLocationTracker.ACTION_CURRENT_LOCATION_BROADCAST);
            intentFilter.addAction(SettingsLocationTracker.ACTION_PERMISSION_DEINED);
            context.registerReceiver(this.currentLocationReceiver, intentFilter);
        }
        return this;
    }

    private void startLocationService(Context context) {
        Intent serviceIntent = new Intent(context, LocationService.class);
        saveSettingsInLocalStorage(context);
        context.startService(serviceIntent);
    }

    /**
     * Stop locaiton service if running
     * @param context Context
     */
    public void stopLocationService(Context context){
        if(LocationService.isRunning(context)) {

            if (currentLocationReceiver != null) {
                context.unregisterReceiver(currentLocationReceiver);
            }

            Intent serviceIntent = new Intent(context, LocationService.class);
            context.stopService(serviceIntent);
        }
    }

    public void validatePermissions(Context context, AppCompatActivity appCompatActivity) {
        if (AppUtils.hasM() && !(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            askPermissions(context, appCompatActivity);
        } else {
             startLocationService(context);
        }
    }

    public void askPermissions(final Context context, final AppCompatActivity appCompatActivity) {
        mBothPermissionRequest =
            PermissionUtil.with(appCompatActivity).request(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).onResult(
                    new Func2() {
                        @Override
                        protected void call(int requestCode, String[] permissions, int[] grantResults) {
                            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                                startLocationService(context);
                            } else {
                                Toast.makeText(context, "Permission Deined", Toast.LENGTH_LONG).show();
                            }
                        }

                    }).ask(SettingsLocationTracker.PERMISSION_ACCESS_LOCATION_CODE);
    }

    public void onRequestPermission(int requestCode, String[] permissions, int[] grantResults) {
        if (null != mBothPermissionRequest) {
            mBothPermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void saveSettingsInLocalStorage(Context context) {
        AppPreferences appPreferences = new AppPreferences(context);
        if (this.interval != 0) { appPreferences.putLong("LOCATION_INTERVAL", this.interval); }
        if (this.smallestDisplacement != 0) { appPreferences.putLong("LOCATION_SMALLEST_DISPLACEMENT", this.smallestDisplacement); }
        if (this.maxWaitTime != 0) {
            appPreferences.putLong("LOCATION_MAX_WAIT_TIME", this.maxWaitTime);
        }
        appPreferences.putString("ACTION", this.actionReceiver);
        appPreferences.putBoolean("GPS", this.gps);
        appPreferences.putBoolean("RUN_IN_FOREGROUND", this.runInForeground);
        appPreferences.putBoolean("NETWORK", this.netWork);

        appPreferences.putString("FOREGROUND_NOTIFICATION_TITLE", this.foregroundNotificationTitle);
        appPreferences.putString("FOREGROUND_NOTIFICATION_TEXT", this.foregroundNotificationText);
        appPreferences.putString("FOREGROUND_NOTIFICATION_TICKER", this.foregroundNotificationTicker);
        appPreferences.putString("FOREGROUND_NOTIFICATION_CHANNEL_ID", this.foregroundNotificationChannelId);

    }

}
