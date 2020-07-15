package com.inderjit.myapplication;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private static final int REQUEST_CODE = 1;
    private Marker homeMarker;
    private Marker destMarker;

    Polyline line;
    Polygon shape;
    private static final int POLYGON_SIDES = 4;
    List<Marker> markers = new ArrayList();
    List<Polyline> polylines = new ArrayList();

    // location with location manager and listener
    LocationManager locationManager;
    LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LatLng canada = new LatLng(52.85, -108.83);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(canada, 5));
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
//                setHomeMarker(location);
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override
            public void onProviderEnabled(String provider) { }
            @Override
            public void onProviderDisabled(String provider) { }
        };

        if (!hasLocationPermission())
            requestLocationPermission();
        else
            startUpdateLocation();

        // apply long press gesture
        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {

                float[] values = new float[1];
                Location.distanceBetween(polyline.getPoints().get(0).latitude, polyline.getPoints().get(0).longitude,
                        polyline.getPoints().get(1).latitude, polyline.getPoints().get(1).longitude, values);
                float distance = values[0];
                LatLng latLng = new LatLng((polyline.getPoints().get(0).latitude + polyline.getPoints().get(1).latitude) / 2, (polyline.getPoints().get(0).longitude + polyline.getPoints().get(1).longitude) / 2);

                MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(stringToImage(distance / 1000 + " Km", 60, getColor(R.color.magenta)))).position(latLng);

                mMap.addMarker(markerOptions);
            }
        });
        mMap.setOnPolygonClickListener(new GoogleMap.OnPolygonClickListener() {
            @Override
            public void onPolygonClick(Polygon polygon) {
                LatLng latLng = new LatLng((markers.get(0).getPosition().latitude + markers.get(2).getPosition().latitude) / 2, (markers.get(0).getPosition().longitude + markers.get(2).getPosition().longitude) / 2);
                float distance = (float) calculateQuadrilateral();
                Paint paint = new Paint();
                Rect bounds = new Rect();
                paint.setTextSize(25);// have this the same as your text size

                String text = distance + " Km";

                paint.getTextBounds(text, 0, text.length(), bounds);
                MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(stringToImage(text,50f, getColor(R.color.magenta)))).position(latLng);

                mMap.addMarker(markerOptions);
            }
        });
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
//                Location location = new Location("Your Destination");
//                location.setLatitude(latLng.latitude);
//                location.setLongitude(latLng.longitude);
                // set marker
                setMarker(latLng);
            }

            private void setMarker(LatLng latLng) {
                Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.ENGLISH);
                try {
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    String locality = "", adminArea = "", subThoroughfare = "", postalCode = "", thoroughfare = "";
                    MarkerOptions options;
                    if (addresses.size() > 0) {
                        Address fetchedAddress = addresses.get(0);
                        StringBuilder strAddress = new StringBuilder();
                        for (int i = 0; i < fetchedAddress.getMaxAddressLineIndex(); i++) {
                            strAddress.append(fetchedAddress.getAddressLine(i)).append(" ");

                        }
                        locality = addresses.get(0).getLocality();
                        adminArea = addresses.get(0).getAdminArea();
                        subThoroughfare = addresses.get(0).getSubThoroughfare();
                        postalCode = addresses.get(0).getPostalCode();
                        thoroughfare = addresses.get(0).getThoroughfare();

                        options = new MarkerOptions().position(latLng)
                                .title(thoroughfare + " | " + subThoroughfare + " | " + postalCode).snippet(locality + " | " + adminArea);
                        if (markers.size() == 0) {
                            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.a));
                        } else if (markers.size() == 1) {
                            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.b));
                        } else if (markers.size() == 2) {
                            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.c));
                        } else if (markers.size() == 3) {
                            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.d));
                        }
                        markers.add(mMap.addMarker(options));
                        if (markers.size()>1) {
                            drawLine(markers.get(markers.size()-2),markers.get(markers.size()-1));
                            if (markers.size()==4){
                                drawLine(markers.get(markers.size()-1),markers.get(0));
                            }
                        }
                        if (markers.size() == POLYGON_SIDES)
                            drawShape();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
//                    printToast("Could not get address..!");
                }


                /*if (destMarker != null) clearMap();
                destMarker = mMap.addMarker(options);*/


                // check if there are already the same number of markers, we clear the map.
                if (markers.size() == 5)
                    clearMap();


            }

            private void drawShape() {
                PolygonOptions options = new PolygonOptions()
                        .fillColor(getColor(R.color.greenOverlay))
                        .strokeColor(Color.RED)
                        .strokeWidth(5);

                for (int i = 0; i < POLYGON_SIDES; i++) {
                    options.add(markers.get(i).getPosition());
                }

                shape = mMap.addPolygon(options);
                shape.setClickable(true);

            }

            private void clearMap() {

                /*if (destMarker != null) {
                    destMarker.remove();
                    destMarker = null;
                }
                line.remove();*/

                for (Marker marker : markers)
                    marker.remove();

                markers.clear();
                if (shape != null)
                    shape.remove();
                shape = null;
            }
            private void drawLine(Marker first,Marker second) {
                PolylineOptions options = new PolylineOptions()
                        .color(Color.RED)
                        .width(10)
                        .add(first.getPosition(), second.getPosition());
                line = mMap.addPolyline(options);
                line.setClickable(true);
                polylines.add(line);
            }
        });
    }
    public Bitmap drawTextToBitmap(Context gContext,
                                   int gResId,
                                   String gText) {
        Resources resources = gContext.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap =
                BitmapFactory.decodeResource(resources, gResId);

        android.graphics.Bitmap.Config bitmapConfig =
                bitmap.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(61, 61, 61));
        paint.setTextSize((int) (14 * scale));
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width())/2;
        int y = (bitmap.getHeight() + bounds.height())/2;
        canvas.drawText(gText, x, y, paint);
        return bitmap;
    }
    double calculateQuadrilateral() {
        double perLineDistance = 0.0;
        double totalDistance = 0;
        for (int i = 0; i < markers.size(); i++) {
            if (i < POLYGON_SIDES-1) {
                perLineDistance=distance(markers.get(i).getPosition().latitude, markers.get(i).getPosition().longitude,
                        markers.get(i + 1).getPosition().latitude, markers.get(i + 1).getPosition().longitude);
            } else {
               perLineDistance=distance(markers.get(i).getPosition().latitude, markers.get(i).getPosition().longitude,
                        markers.get(0).getPosition().latitude, markers.get(0).getPosition().longitude);
            }
            totalDistance = totalDistance + perLineDistance;
        }
        return totalDistance;
    }
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
    private void startUpdateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);

        /*Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        setHomeMarker(lastKnownLocation);*/
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    public Bitmap stringToImage(String text, float textSize, int textColor) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }

    private void setHomeMarker(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions options = new MarkerOptions().position(userLocation)
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .snippet("Your Location");
        homeMarker = mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (REQUEST_CODE == requestCode) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
            }
        }
    }
}
