package com.marsvard.rxcompasssample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.marsvard.northwest.NorthWest;

import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class CompassActivity extends AppCompatActivity {

    TextView degreesTextView;
    private Subscription degreesSubscription = Subscriptions.empty();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        degreesTextView = (TextView) findViewById(R.id.degrees);

        degreesSubscription = NorthWest.getDegrees(this)
                .subscribe(new Subscriber<Double>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Double degrees) {
                        degreesTextView.setText(String.valueOf(degrees));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        degreesSubscription.unsubscribe();
        super.onDestroy();
    }
}
