package com.mapbox.mapboxsdk.testapp.activity.userlocation;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.mapbox.mapboxsdk.maps.LocationSource;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.testapp.R;
import com.mapbox.mapboxsdk.testapp.activity.camera.CameraPositionActivity;

import timber.log.Timber;

public class LocationSourceActivity extends AppCompatActivity implements OnMapReadyCallback, LocationSource {

  private MapView mapView;
  private MapboxMap mapboxMap;
  private OnLocationChangedListener locationChangedListener;
  private Handler handler;
  private UpdateLocationRunnable runnable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_location_source);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setDisplayShowHomeEnabled(true);
    }

    handler = new Handler();
    runnable = new UpdateLocationRunnable();

    mapView = (MapView) findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setColorFilter(ContextCompat.getColor(this, R.color.primary));
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(mapboxMap!=null) {
          mapboxMap.setMyLocationEnabled(!mapboxMap.isMyLocationEnabled());
        }
      }
    });
  }

  @Override
  public void onMapReady(@NonNull final MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setLocationSource(this);
  }

  @Override
  public void activate(OnLocationChangedListener listener) {
    Timber.d("Location source activate");
    locationChangedListener = listener;
    locationChangedListener.onLocationChanged(generateLocation(46.57638889, 8.89263889));
    handler.postDelayed(runnable = new UpdateLocationRunnable(), 2000);
  }

  @Override
  public void deactivate(OnLocationChangedListener listener) {
    Timber.d("Location source deactivate");
    locationChangedListener = null;
  }

  private class UpdateLocationRunnable implements Runnable {

    private int step;

    private double[][] latLngs = new double[][] {
      {46.57608333, 8.89241667},
      {46.57619444, 8.89252778},
      {46.57641667, 8.89266667},
      {46.57650000, 8.89280556},
      {46.57638889, 8.89302778},
      {46.57652778, 8.89322222},
      {46.57661111, 8.89344444}
    };

    @Override
    public void run() {
      if (locationChangedListener != null) {
        locationChangedListener.onLocationChanged(generateLocation(latLngs[step][0], latLngs[step][1]));
        step++;
      }
    }
  }

  private Location generateLocation(double lat, double lng) {
    Location location = new Location(getClass().getSimpleName());
    location.setLatitude(lat);
    location.setLongitude(lng);
    return location;
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
    if (handler != null && runnable != null) {
      handler.removeCallbacks(runnable);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}