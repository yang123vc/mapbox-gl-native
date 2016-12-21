package com.mapbox.mapboxsdk.maps;

import android.location.Location;

/**
 * Interface definition to provide location updates to the Mapbox Android SDK.
 * <p>
 * The default implementation relies on using Mapzen LOST as a location source, you can provide your own implementation
 * through {@link MapboxMap#setLocationSource}.
 * </p>
 * <p>
 * A MapboxMap object activates its location source by calling
 * {@link LocationSource#activate(OnLocationChangedListener) and deactivates it by
 * {@link LocationSource#deactivate(OnLocationChangedListener). While active the LocationSource is responsible
 * for providing location updates to the SDK with {@link OnLocationChangedListener#onLocationChanged(Location)}.
 * </p>
 */
public interface LocationSource {

  void activate(LocationSource.OnLocationChangedListener listener);

  void deactivate(LocationSource.OnLocationChangedListener listener);

  /**
   * Interface definition that provides location updates.
   */
  public interface OnLocationChangedListener {

    /**
     * Called to provide a location update.
     *
     * @param location the new location
     */
    void onLocationChanged(Location location);
  }
}
