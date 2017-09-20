package com.bkx.lab.view;

import android.Manifest;
import android.location.Location;
import android.support.annotation.RequiresPermission;

import com.bkx.lab.model.LocationEntity;
import com.google.android.gms.location.LocationRequest;
import com.patloew.rxlocation.RxLocation;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class RxLocationManager {

    private final RxLocation rxLocation;

    private final LocationRequest locationRequest;

    private static LocationEntity location = new LocationEntity();

    private Disposable locationUpdatesDisposable;


    public RxLocationManager(RxLocation rxLocation) {
        this.rxLocation = rxLocation;
        locationRequest = buildLocationRequest();
    }


    @SuppressWarnings("MissingPermission")
    @RequiresPermission(anyOf = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    public void startLocationUpdates(boolean checkLocationSettings) {
        stopLocationUpdates();
        locationUpdatesDisposable = locationSettingsCheck(checkLocationSettings)
                .flatMapObservable(ignore -> locationUpdates()
                        .startWith(lastLocation()))
                .map(this::transformLocation)
                .toFlowable(BackpressureStrategy.LATEST)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setLocation,
                        error -> Timber.e("Failed to get location updates", error));
    }

    public void stopLocationUpdates() {
        if (locationUpdatesDisposable != null && !locationUpdatesDisposable.isDisposed()) {
            locationUpdatesDisposable.dispose();
            locationUpdatesDisposable = null;
        }
    }

    public LocationEntity getLocation() {
        Timber.d("getLocation: " + location);
        return location;
    }


    private void setLocation(LocationEntity location) {
        Timber.d("setLocation: " + location);
        this.location = location;
    }

    private LocationEntity transformLocation(Location location) {
        LocationEntity locationEntity = new LocationEntity();
        locationEntity.setLatitude(location.getLatitude());
        locationEntity.setLongitude(location.getLongitude());
        return locationEntity;
    }

    @SuppressWarnings("MissingPermission")
    @RequiresPermission(anyOf = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    private Observable<Location> lastLocation() {
        return rxLocation.location().lastLocation().toObservable()
                .doOnSubscribe(ignore -> Timber.d("lastLocation: on subscribe"))
                .doOnNext(item -> Timber.d("lastLocation: item: " + item))
                .doOnComplete(() -> Timber.d("lastLocation: on complete"))
                .doOnDispose(() -> Timber.d("lastLocation: on dispose"));
    }

    @SuppressWarnings("MissingPermission")
    @RequiresPermission(anyOf = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    })
    private Observable<Location> locationUpdates() {
        return rxLocation.location().updates(locationRequest)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(ignore -> Timber.d("locationUpdates: on subscribe"))
                .doOnNext(item -> Timber.d("locationUpdates: item: " + item))
                .doOnComplete(() -> Timber.d("locationUpdates: on complete"))
                .doOnDispose(() -> Timber.d("locationUpdates: on dispose"));
    }

    private Single<Boolean> locationSettingsCheck(boolean enabled) {
        if (!enabled) {
            return Observable.just(Boolean.TRUE).singleOrError()
                    .doOnSubscribe(ignore -> Timber.d("locationSettings.disabled: on subscribe"))
                    .doOnEvent((event, error) -> Timber.d("locationSettings.disabled: event: " + event + ", error: " + error))
                    .doOnSuccess(ignore -> Timber.d("locationSettings.disabled: on success"))
                    .doOnDispose(() -> Timber.d("locationSettings.disabled: on dispose"));
        }
        return rxLocation.settings().checkAndHandleResolution(locationRequest)
                .doOnSubscribe(ignore -> Timber.d("locationSettings: on subscribe"))
                .doOnEvent((event, error) -> Timber.d("locationSettings: event: " + event + ", error: " + error))
                .doOnSuccess(ignore -> Timber.d("locationSettings: on success"))
                .doOnDispose(() -> Timber.d("locationSettings: on dispose"));
    }

    private LocationRequest buildLocationRequest() {
        return LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
}
