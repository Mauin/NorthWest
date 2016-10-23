package com.marsvard.northwest;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class NorthWest implements Observable.OnSubscribe<Double> {
    private final String TAG = NorthWest.class.getSimpleName();
    private final Context context;
    GeomagneticField magneticField;

    public static Observable<Double> getDegrees(Context context) {
        return Observable.create(new NorthWest(context));
    }

    private NorthWest(Context context) {
        this.context = context;
    }

    @Override
    public void call(Subscriber<? super Double> subscriber) {
        final SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        final SensorEventListener listener = createSensorEventListener(subscriber);
        sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            magneticField = new GeomagneticField(
                    (float) location.getLatitude(),
                    (float) location.getLongitude(),
                    (float) location.getAltitude(),
                    System.currentTimeMillis()
            );

        } catch (Exception ex) {
            Log.i(TAG, "RxCompass: not calculating magnetic field declination because app has no permission for location");
        }

        subscriber.add(Subscriptions.create(new Action0() {
            @Override
            public void call() {
                sensorManager.unregisterListener(listener);
            }
        }));
    }

    private SensorEventListener createSensorEventListener(final Subscriber<? super Double> subscriber) {
        return new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
				float[] orientation = getOrientationFromSensorEvent(event);
				double angle = calculateAngleFromOrientation(orientation[0]);
				subscriber.onNext(angle);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // nothing to do
            }
        };
    }

	double calculateAngleFromOrientation(float orientation) {
		// if we were able to get the magnetic field based on the user's last know location we should take that in consideration
		// otherwise we are left with the sensor data which might be a little off
		double angle;
		if (magneticField != null) {
			angle = Math.toDegrees(orientation) + magneticField.getDeclination();
		} else {
			angle = Math.toDegrees(orientation);
		}

		angle = (angle + 360 ) % 360;
		return angle;
	}

	float[] getOrientationFromSensorEvent(SensorEvent event) {
		// calculate degrees to north
		float[] rotationMatrix = new float[16];
		float[] truncatedRotationVector = new float[4];

		if (event.values.length > 4) {
			Log.d(TAG, "Sensor vector > 4");
			// On some Samsung devices, an exception is thrown if this vector > 4 (see #39)
			// Truncate the array, since we only care about the first 4 values anyway
			System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
			SensorManager.getRotationMatrixFromVector(rotationMatrix, truncatedRotationVector);
		} else {
			SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
		}

		float[] orientation = new float[3];
		SensorManager.getOrientation(rotationMatrix, orientation);
		return orientation;
	}
}
