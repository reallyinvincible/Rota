package com.exuberant.rota;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.etiennelawlor.discreteslider.library.ui.DiscreteSlider;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private PolylineOptions bluePolylineOptions, pinkPolylineOptions, redPolylineOptions, greenPolylineOptions;
    private Polyline pinkPolyline, bluePolyline, redPolyline, greenPolyline;
    private LatLng startLocation;
    private DiscreteSlider discreteSlider;
    private com.google.android.gms.maps.model.Marker inputMarker, outputMarker;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference rootReference, pointReference, interactionReference, outputReference;
    ValueAnimator inputPolylineAnimator, outputPolylineAnimator;
    int timeFactor = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        initialize();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void initialize() {
        discreteSlider = findViewById(R.id.discrete_slider);
        discreteSlider.setPosition(2);
        firebaseDatabase = FirebaseDatabase.getInstance();
        rootReference = firebaseDatabase.getReference();
        interactionReference = rootReference.child("interactions");


        int n = discreteSlider.getPosition();
        outputReference = rootReference.child("output").child("path" + String.valueOf(n + 1));
        pointReference = rootReference.child("training").child("path " + String.valueOf(n + 1));
        discreteSlider.setOnDiscreteSliderChangeListener(new DiscreteSlider.OnDiscreteSliderChangeListener() {
            @Override
            public void onPositionChanged(int position) {
                interactionReference.child("currentPath").setValue(position + 1);
                startPrediction(position);
            }
        });
    }

    private void startPrediction(int position) {
        pointReference = rootReference.child("training").child("path " + String.valueOf(position + 1));
        outputReference = rootReference.child("output").child("path " + String.valueOf(position + 1));
        registerTrainingFirebaseListeners(pointReference);
        registerOutputTrainingListeners(outputReference);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style));
        } catch (Exception e) {
            Toast.makeText(this, "Can't apply style", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerTrainingFirebaseListeners(DatabaseReference reference) {
        
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Coords> poi = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Coords coords = snapshot.getValue(Coords.class);
                    poi.add(coords);
                }
                processTrainingPoints(poi, false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                int c = 10;
            }
        });
    }

    private void registerOutputTrainingListeners(DatabaseReference reference){
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Coords> predictedPath = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Coords coords = snapshot.getValue(Coords.class);
                    predictedPath.add(coords);
                }
                Toast.makeText(MapsActivity.this, String.valueOf(predictedPath.size()) + " points loaded. Starting predictions", Toast.LENGTH_SHORT).show();
                processOutputPoints(predictedPath, false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }



    private void processTrainingPoints(List<Coords> points, boolean clear) {

        mMap.clear();

        if (clear)
            mMap.clear();

        if (inputPolylineAnimator != null && inputPolylineAnimator.isRunning()) {
            inputPolylineAnimator.cancel();
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        List<LatLng> path = new ArrayList<>();
        for (Coords point : points) {
            LatLng latLng = new LatLng(point.latitude, point.longitude);
            builder.include(latLng);
            path.add(latLng);
        }
        startLocation = path.get(0);
        addInputStartMarker(startLocation);
        LatLngBounds bounds = builder.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
        mMap.animateCamera(cameraUpdate);

        Drawable drawable = getDrawable(R.drawable.ic_car_input);
        BitmapDescriptor icon = getMarkerIconFromDrawable(drawable);
        inputMarker = mMap.addMarker(new MarkerOptions().position(startLocation)
                .position(startLocation)
                .icon(icon));

        bluePolylineOptions = new PolylineOptions();
        bluePolylineOptions.color(Color.parseColor("#29B6F6"));
        bluePolylineOptions.width(5);
        bluePolylineOptions.startCap(new SquareCap());
        bluePolylineOptions.endCap(new SquareCap());
        bluePolylineOptions.jointType(JointType.ROUND);
        bluePolylineOptions.addAll(path);
        bluePolyline = mMap.addPolyline(bluePolylineOptions);

        pinkPolylineOptions = new PolylineOptions();
        pinkPolylineOptions.color(Color.parseColor("#EC407A"));
        pinkPolylineOptions.width(8);
        pinkPolylineOptions.startCap(new SquareCap());
        pinkPolylineOptions.endCap(new SquareCap());
        pinkPolylineOptions.jointType(JointType.ROUND);
        pinkPolylineOptions.addAll(path);
        pinkPolyline = mMap.addPolyline(pinkPolylineOptions);
        addEndMarker(path.get(path.size() - 1));

        inputPolylineAnimator = ValueAnimator.ofInt(0, 100);
        inputPolylineAnimator.setDuration(points.size() * timeFactor);
        inputPolylineAnimator.setInterpolator(new LinearInterpolator());
        inputPolylineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                List<LatLng> points = bluePolyline.getPoints();
                int percentValue = (int) valueAnimator.getAnimatedValue();
                int size = points.size();
                int newPoints = (int) (size * (percentValue / 100.0));
                List<LatLng> p = points.subList(0, newPoints);
                if (p.size() > 1) {
                    inputMarker.setPosition(p.get(p.size() - 1));
                    inputMarker.setAnchor(0.5f, 0.5f);
                    inputMarker.setRotation(getBearing(p.get(p.size() - 2), p.get(p.size() - 1)));
                }
                pinkPolyline.setPoints(p);
            }
        });
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                inputPolylineAnimator.start();
            }
        }, 10000);
    }

    private void processOutputPoints(List<Coords> points, boolean clear) {

        if (outputPolylineAnimator != null && outputPolylineAnimator.isRunning()) {
            outputPolylineAnimator.cancel();
        }

        List<LatLng> path = new ArrayList<>();
        for (Coords point : points) {
            LatLng latLng = new LatLng(point.latitude, point.longitude);
            path.add(latLng);
        }
        startLocation = path.get(0);
        addOutputStartMarker(startLocation);

        Drawable drawable = getDrawable(R.drawable.ic_car);
        BitmapDescriptor icon = getMarkerIconFromDrawable(drawable);
        outputMarker = mMap.addMarker(new MarkerOptions().position(startLocation)
                .position(startLocation)
                .icon(icon));

        redPolylineOptions = new PolylineOptions();
        redPolylineOptions.color(Color.TRANSPARENT);
        redPolylineOptions.width(5);
        redPolylineOptions.startCap(new SquareCap());
        redPolylineOptions.endCap(new SquareCap());
        redPolylineOptions.jointType(JointType.ROUND);
        redPolylineOptions.addAll(path);
        redPolyline = mMap.addPolyline(redPolylineOptions);

        greenPolylineOptions = new PolylineOptions();
        greenPolylineOptions.color(Color.parseColor("#64DD17"));
        greenPolylineOptions.width(8);
        greenPolylineOptions.startCap(new SquareCap());
        greenPolylineOptions.endCap(new SquareCap());
        greenPolylineOptions.jointType(JointType.ROUND);
        greenPolylineOptions.addAll(path);
        greenPolyline = mMap.addPolyline(greenPolylineOptions);

        outputPolylineAnimator = ValueAnimator.ofInt(0, 100);
        outputPolylineAnimator.setDuration(points.size() * timeFactor);
        outputPolylineAnimator.setInterpolator(new LinearInterpolator());
        outputPolylineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                List<LatLng> points = redPolyline.getPoints();
                int percentValue = (int) valueAnimator.getAnimatedValue();
                int size = points.size();
                int newPoints = (int) (size * (percentValue / 100.0));
                List<LatLng> p = points.subList(0, newPoints);
                if (p.size() > 1) {
                    outputMarker.setPosition(p.get(p.size() - 1));
                    outputMarker.setAnchor(0.5f, 0.5f);
                    outputMarker.setRotation(getBearing(p.get(p.size() - 2), p.get(p.size() - 1)));
                }
                greenPolyline.setPoints(p);
            }
        });
        outputPolylineAnimator.start();
        
    }

    private float getBearing(LatLng startLocation, LatLng newPos) {
        double lat = Math.abs(startLocation.latitude - newPos.latitude);
        double lng = Math.abs(startLocation.longitude - newPos.longitude);
        if (startLocation.latitude < newPos.latitude && startLocation.longitude < newPos.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (startLocation.latitude >= newPos.latitude && startLocation.longitude < newPos.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (startLocation.latitude >= newPos.latitude && startLocation.longitude >= newPos.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (startLocation.latitude < newPos.latitude && startLocation.longitude >= newPos.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    private void addInputStartMarker(LatLng latLng) {
        Drawable drawable = getDrawable(R.drawable.ic_input_tracking_start);
        BitmapDescriptor icon = getMarkerIconFromDrawable(drawable);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                .icon(icon)
                .anchor(0.5f, 0.5f);
        mMap.addMarker(markerOptions).setTag("Tracks");
    }

    private void addOutputStartMarker(LatLng latLng) {
        Drawable drawable = getDrawable(R.drawable.ic_output_tracking_start);
        BitmapDescriptor icon = getMarkerIconFromDrawable(drawable);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                .icon(icon)
                .anchor(0.5f, 0.5f);
        mMap.addMarker(markerOptions).setTag("Tracks");
    }

    private void addEndMarker(LatLng latLng) {
        Drawable drawable = getDrawable(R.drawable.ic_input_tracking_end);
        BitmapDescriptor icon = getMarkerIconFromDrawable(drawable);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                .anchor(0.5f, 0.5f)
                .icon(icon);
        mMap.addMarker(markerOptions).setTag("Tracks");
    }


    private void addPathMarker(LatLng latLng) {
        Drawable drawable = getDrawable(R.drawable.ic_input_tracking_marker);
        BitmapDescriptor icon = getMarkerIconFromDrawable(drawable);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng)
                .icon(icon);
        mMap.addMarker(markerOptions).setTag("Tracks");
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /*private void addTickMarkTextLabels(){
        int tickMarkCount = discreteSlider.getTickMarkCount();
        float tickMarkRadius = discreteSlider.getTickMarkRadius();
        int width = tickMarkLabelsRelativeLayout.getMeasuredWidth();

        int discreteSliderBackdropLeftMargin = DisplayUtility.dp2px(getContext(), 32);
        int discreteSliderBackdropRightMargin = DisplayUtility.dp2px(getContext(), 32);
        float firstTickMarkRadius = tickMarkRadius;
        float lastTickMarkRadius = tickMarkRadius;
        int interval = (width - (discreteSliderBackdropLeftMargin+discreteSliderBackdropRightMargin) - ((int)(firstTickMarkRadius+lastTickMarkRadius)) )
                / (tickMarkCount-1);

        String[] tickMarkLabels = {"$", "$$", "$$$", "$$$$", "$$$$$"};
        int tickMarkLabelWidth = DisplayUtility.dp2px(getContext(), 40);

        for(int i=0; i<tickMarkCount; i++) {
            TextView tv = new TextView(getContext());

            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    tickMarkLabelWidth, RelativeLayout.LayoutParams.WRAP_CONTENT);

            tv.setText(tickMarkLabels[i]);
            tv.setGravity(Gravity.CENTER);
            if(i==discreteSlider.getPosition())
                tv.setTextColor(getResources().getColor(R.color.colorPrimary));
            else
                tv.setTextColor(getResources().getColor(R.color.grey_400));

//                    tv.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));

            int left = discreteSliderBackdropLeftMargin + (int)firstTickMarkRadius + (i * interval) - (tickMarkLabelWidth/2);

            layoutParams.setMargins(left,
                    0,
                    0,
                    0);
            tv.setLayoutParams(layoutParams);

            tickMarkLabelsRelativeLayout.addView(tv);
        }
    }
*/
}
