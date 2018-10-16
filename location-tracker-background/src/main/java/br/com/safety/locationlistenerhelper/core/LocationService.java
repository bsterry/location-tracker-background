package br.com.safety.locationlistenerhelper.core;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import static br.com.safety.locationlistenerhelper.core.SettingsLocationTracker.ACTION_CURRENT_LOCATION_BROADCAST;

/**
 * @author netodevel
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = LocationService.class.getSimpleName();

    protected GoogleApiClient mGoogleApiClient;

    protected LocationRequest mLocationRequest;

    protected Location mCurrentLocation;

    protected long interval;

    protected float smallestDisplacement = 0.0f;

    protected long maxWaitTime = 0;

    protected String actionReceiver;

    protected Boolean gps;

    protected Boolean netWork;

    protected Boolean runInForeground;

    protected String foregroundNotificationTitle;
    protected String foregroundNotificationText;
    protected String foregroundNotificationTicker;
    protected String foregroundNotificationChannelId;

    private AppPreferences appPreferences;

    public static boolean isRunning(Context context) {
        return AppUtils.isServiceRunning(context, LocationService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appPreferences = new AppPreferences(getBaseContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (this.actionReceiver == null) {
            this.actionReceiver = this.appPreferences.getString("ACTION", "LOCATION.ACTION");
        }

        if (this.interval <= 0){
            this.interval = this.appPreferences.getLong("LOCATION_INTERVAL", 10000L);
        }

        if (this.smallestDisplacement <= 0.0f){
            this.smallestDisplacement = (float)this.appPreferences.getLong("LOCATION_SMALLEST_DISPLACEMENT", 0L);
        }

        if (this.maxWaitTime <= 0) {
            this.maxWaitTime = this.appPreferences.getLong("LOCATION_MAX_WAIT_TIME", 0L);
        }

        if (this.gps == null) {
            this.gps = this.appPreferences.getBoolean("GPS", true);
        }

        if (this.runInForeground == null) {
            this.runInForeground = this.appPreferences.getBoolean("RUN_IN_FOREGROUND", true);
            this.foregroundNotificationTitle = this.appPreferences.getString("FOREGROUND_NOTIFICATION_TITLE", null);
            this.foregroundNotificationText = this.appPreferences.getString("FOREGROUND_NOTIFICATION_TEXT", null);
            this.foregroundNotificationTicker = this.appPreferences.getString("FOREGROUND_NOTIFICATION_TICKER", null);
            this.foregroundNotificationChannelId = this.appPreferences.getString("FOREGROUND_NOTIFICATION_CHANNEL_ID", null);
        }

        if (this.netWork == null) {
            this.netWork = this.appPreferences.getBoolean("NETWORK", false);
        }

        if (this.runInForeground) {
            int FOREGROUND_ID = 1338;
            startForeground(FOREGROUND_ID, this.buildNotification());
        }

        buildGoogleApiClient();

        mGoogleApiClient.connect();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
        return START_STICKY;
    }

    protected Notification buildNotification() {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, this.foregroundNotificationChannelId);
        b.setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(this.foregroundNotificationTitle)
                .setContentText(this.foregroundNotificationText)
                .setTicker(this.foregroundNotificationTicker);

        return (b.build());

    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(this.interval);
        mLocationRequest.setFastestInterval(this.interval / 2);
        mLocationRequest.setSmallestDisplacement(this.smallestDisplacement);
        mLocationRequest.setMaxWaitTime(this.maxWaitTime);
        if (this.gps) {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else if (this.netWork){
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
    }

    protected void startLocationUpdates() {
        try {
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        } catch (SecurityException ex) {
        }
    }

    private void updateService() {
        if (null != mCurrentLocation) {
            sendLocationBroadcast(this.mCurrentLocation);
            sendCurrentLocationBroadCast(this.mCurrentLocation);
            Log.d("Info: ", "send broadcast location data");
        } else {
            sendPermissionDeinedBroadCast();
            Log.d("Error: ", "Permission deined");
        }
    }

    private void sendLocationBroadcast(Location sbLocationData) {
        Intent locationIntent = new Intent();
        locationIntent.setAction(this.actionReceiver);
        locationIntent.putExtra(SettingsLocationTracker.LOCATION_MESSAGE, sbLocationData);
        sendBroadcast(locationIntent);
    }

    private void sendCurrentLocationBroadCast(Location sbLocationData) {
        Intent locationIntent = new Intent();
        locationIntent.setAction(ACTION_CURRENT_LOCATION_BROADCAST);
        locationIntent.putExtra(SettingsLocationTracker.LOCATION_MESSAGE, sbLocationData);
        sendBroadcast(locationIntent);
    }

    private void sendPermissionDeinedBroadCast() {
        Intent locationIntent = new Intent();
        locationIntent.setAction(SettingsLocationTracker.ACTION_PERMISSION_DEINED);
        sendBroadcast(locationIntent);
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle connectionHint) throws SecurityException {
        Log.i(TAG, "Connected to GoogleApiClient");
        if (mCurrentLocation == null) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            updateService();
        }
        startLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        updateService();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
