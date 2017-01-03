package com.mapbox.mapboxsdk;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.exceptions.InvalidAccessTokenException;
import com.mapbox.mapboxsdk.location.DefaultLocationSource;
import com.mapbox.mapboxsdk.maps.LocationSource;
import com.mapbox.mapboxsdk.net.ConnectivityReceiver;
import com.mapbox.mapboxsdk.telemetry.MapboxEventManager;
import com.mapbox.mapboxsdk.telemetry.TelemetryLocationReceiver;
import com.mapzen.android.lost.api.LocationListener;
import com.mapzen.android.lost.api.LocationRequest;
import com.mapzen.android.lost.api.LostApiClient;

import static com.mapzen.android.lost.api.LocationServices.FusedLocationApi;

public final class Mapbox {

  private static Mapbox INSTANCE;
  private Context context;
  private String accessToken;
  private Boolean connected;
  private LocationSourceManager locationSourceManager;

  public static synchronized Mapbox getInstance(@NonNull Context context, @NonNull String accessToken) {
    if (INSTANCE == null) {
      Context appContext = context.getApplicationContext();
      INSTANCE = new Mapbox(appContext, accessToken);
      MapboxEventManager.getMapboxEventManager().initialize(appContext, accessToken);
      ConnectivityReceiver.instance(appContext);
    }
    return INSTANCE;
  }

  private Mapbox(@NonNull Context context, @NonNull String accessToken) {
    this.context = context;
    this.accessToken = accessToken;
    this.locationSourceManager = new LocationSourceManager(context);
  }

  /**
   * Access Token for this application.
   *
   * @return Mapbox Access Token
   */
  public static String getAccessToken() {
    validateAccessToken();
    return INSTANCE.accessToken;
  }

  /**
   * Runtime validation of Access Token.
   *
   * @throws InvalidAccessTokenException exception thrown when not using a valid accessToken
   */
  private static void validateAccessToken() throws InvalidAccessTokenException {
    String accessToken = INSTANCE.accessToken;
    if (TextUtils.isEmpty(accessToken) || (!accessToken.toLowerCase(MapboxConstants.MAPBOX_LOCALE).startsWith("pk.")
      && !accessToken.toLowerCase(MapboxConstants.MAPBOX_LOCALE).startsWith("sk."))) {
      throw new InvalidAccessTokenException();
    }
  }

  /**
   * Application context
   */
  public static Context getApplicationContext() {
    return INSTANCE.context;
  }

  /**
   * Manually sets the connectivity state of the app. This is useful for apps that control their
   * own connectivity state and want to bypass any checks to the ConnectivityManager.
   *
   * @param connected flag to determine the connectivity state, true for connected, false for
   *                  disconnected, null for ConnectivityManager to determine.
   */
  public static void setConnected(Boolean connected) {
    // Connectivity state overridden by app
    INSTANCE.connected = connected;
  }

  /**
   * Determines whether we have an Internet connection available. Please do not rely on this
   * method in your apps, this method is used internally by the SDK.
   *
   * @return true if there is an Internet connection, false otherwise
   */
  public static Boolean isConnected() {
    if (INSTANCE.connected != null) {
      // Connectivity state overridden by app
      return INSTANCE.connected;
    }

    ConnectivityManager cm = (ConnectivityManager) INSTANCE.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return (activeNetwork != null && activeNetwork.isConnected());
  }

  public static void setLocationSource(@Nullable LocationSource locationSource) {
    INSTANCE.locationSourceManager.setLocationSource(locationSource);
  }

  public static void activateLocationSource(LocationSource.OnLocationChangedListener locationChangedListener) {
    INSTANCE.locationSourceManager.activate(locationChangedListener);
  }

  public static void deactivateLocationSource() {
    INSTANCE.locationSourceManager.deactivate();
  }

  private static class LocationSourceManager implements LocationSource.OnLocationChangedListener {

    private LocationSource activeLocationSource;
    private DefaultLocationSource defaultLocationSource;
    private LocationSource.OnLocationChangedListener locationChangeListener;

    LocationSourceManager(Context context) {
      defaultLocationSource = new DefaultLocationSource(context);

      defaultLocationSource.activate(this);
      activeLocationSource = defaultLocationSource;
    }

    void setLocationSource(@Nullable LocationSource locationSource) {
      activeLocationSource.deactivate(this);

      if (locationSource == null) {
        // restore default location source, reactivate
        locationSource = defaultLocationSource;
        defaultLocationSource.activate(this);
      }
      activeLocationSource = locationSource;
    }

    void activate(LocationSource.OnLocationChangedListener listener) {
      locationChangeListener = listener;

      if (activeLocationSource instanceof DefaultLocationSource) {
        defaultLocationSource.requestActiveLocationUpdates();
      } else {
        activeLocationSource.activate(this);
      }
    }

    void deactivate() {
      locationChangeListener = null;

      if (activeLocationSource instanceof DefaultLocationSource) {
        defaultLocationSource.requestPassiveLocationUpdates();
      } else {
        activeLocationSource.deactivate(this);
      }
    }

    @Override
    public void onLocationChanged(Location location) {
      if (locationChangeListener != null) {
        locationChangeListener.onLocationChanged(location);
      }

      // Update the Telemetry Receiver
      Intent locIntent = new Intent(TelemetryLocationReceiver.INTENT_STRING);
      locIntent.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
      LocalBroadcastManager.getInstance(Mapbox.getApplicationContext()).sendBroadcast(locIntent);
    }
  }

  private static class DefaultLocationSource implements LocationSource, LostApiClient.ConnectionCallbacks, com.mapzen.android.lost.api.LocationListener {

    private Context context;
    private LostApiClient lostApiClient;
    private LocationRequest locationRequest;
    private OnLocationChangedListener locationChangedListener;

    DefaultLocationSource(@NonNull Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
      if (locationChangedListener != null) {
        throw new IllegalStateException("Location source was already active, can't activate");
      }
      locationChangedListener = listener;
      if (lostApiClient == null) {
        lostApiClient = new LostApiClient.Builder(context).addConnectionCallbacks(this).build();
      }
    }

    @Override
    public void deactivate(OnLocationChangedListener listener) {
      if (locationChangedListener == null) {
        throw new IllegalStateException("Location source was not active, can't deactivate.");
      }
      locationChangedListener = null;
    }

    @Override
    public void onConnected() {
      cancelCurrentRequest();
      requestPassiveLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
      if (locationChangedListener != null) {
        locationChangedListener.onLocationChanged(location);
      }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onConnectionSuspended() {
      lostApiClient = null;
      if (locationChangedListener != null) {
        deactivate(locationChangedListener);
      }
    }

    private void requestActiveLocationUpdates() {
      if (locationRequest != null) {
        cancelCurrentRequest();
      }

      locationRequest = LocationRequest.create()
        .setFastestInterval(1000)
        .setSmallestDisplacement(3.0f)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
      //noinspection MissingPermission
      FusedLocationApi.requestLocationUpdates(lostApiClient, locationRequest, this);
    }

    private void requestPassiveLocationUpdates() {
      if (locationRequest != null) {
        cancelCurrentRequest();
      }

      locationRequest = LocationRequest.create()
        .setFastestInterval(1000)
        .setSmallestDisplacement(3.0f)
        .setPriority(LocationRequest.PRIORITY_NO_POWER);
      //noinspection MissingPermission
      FusedLocationApi.requestLocationUpdates(lostApiClient, locationRequest, this);
    }

    private void cancelCurrentRequest() {
      if (locationRequest != null) {
        FusedLocationApi.removeLocationUpdates(lostApiClient, this);
        locationRequest = null;
      }
    }

    boolean isPermissionsAccepted() {
      return (ContextCompat.checkSelfPermission(getApplicationContext(),
        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        || ContextCompat.checkSelfPermission(getApplicationContext(),
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
  }
}