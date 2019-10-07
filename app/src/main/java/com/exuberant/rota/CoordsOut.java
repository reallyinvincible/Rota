package com.exuberant.rota;

public class CoordsOut {

    double latitude;
    double longitude;
    float bearing;

    public CoordsOut() {
    }

    public CoordsOut(double latitude, double longitude, float bearing) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.bearing = bearing;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }
}
