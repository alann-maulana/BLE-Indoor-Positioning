package com.nexenio.bleindoorpositioningdemo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.nexenio.bleindoorpositioning.ble.Beacon;
import com.nexenio.bleindoorpositioning.location.Location;
import com.nexenio.bleindoorpositioning.location.listener.LocationListener;
import com.nexenio.bleindoorpositioning.location.projection.CanvasProjection;
import com.nexenio.bleindoorpositioning.location.projection.EquirectangularProjection;
import com.nexenio.bleindoorpositioning.location.provider.LocationProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by steppschuh on 16.11.17.
 */

public class BeaconMap extends BeaconView {

    protected ValueAnimator deviceRadiusAnimator;

    protected Location topLeftLocation;
    protected Location bottomRightLocation;
    protected LocationAnimator topLeftLocationAnimator;
    protected LocationAnimator bottomRightLocationAnimator;

    protected CanvasProjection canvasProjection;

    public BeaconMap(Context context) {
        super(context);
    }

    public BeaconMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BeaconMap(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BeaconMap(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void initialize() {
        super.initialize();
        canvasProjection = new CanvasProjection();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        canvasProjection.setCanvasWidth(canvasWidth);
        canvasProjection.setCanvasHeight(canvasHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLegend(canvas);
    }

    @Override
    protected void drawDevice(Canvas canvas) {
        PointF point = (deviceLocationAnimator == null) ? canvasCenter : getPointFromLocation(deviceLocationAnimator.getLocation());
        float animationValue = (deviceRadiusAnimator == null) ? 0 : (float) deviceRadiusAnimator.getAnimatedValue();
        float deviceRadius = (pixelsPerDip * 8) + (pixelsPerDip * 24 * animationValue);
        canvas.drawCircle(point.x, point.y, deviceRadius, deviceRadiusPaint);
        canvas.drawCircle(point.x, point.y, pixelsPerDip * 32, deviceRadiusPaint);
        canvas.drawCircle(point.x, point.y, pixelsPerDip * 10, whiteFillPaint);
        canvas.drawCircle(point.x, point.y, pixelsPerDip * 10, primaryStrokePaint);
        canvas.drawCircle(point.x, point.y, pixelsPerDip * 8, primaryFillPaint);
    }

    @Override
    protected void drawBeacon(Canvas canvas, Beacon beacon) {
        PointF point = getPointFromLocation(beacon.getLocation());
        float beaconAdvertisingRange = 50; // in meters TODO: get real value based on tx power
        float advertisingRadius = (float) canvasProjection.getCanvasUnitsFromMeters(beaconAdvertisingRange);
        canvas.drawCircle(point.x, point.y, advertisingRadius, deviceRadiusPaint);

        float beaconRadius = pixelsPerDip * 8;
        int beaconCornerRadius = (int) pixelsPerDip * 2;
        RectF rect = new RectF(point.x - beaconRadius, point.y - beaconRadius, point.x + beaconRadius, point.y + beaconRadius);
        canvas.drawRoundRect(rect, beaconCornerRadius, beaconCornerRadius, whiteFillPaint);
        canvas.drawRoundRect(rect, beaconCornerRadius, beaconCornerRadius, primaryStrokePaint);

        beaconRadius = beaconRadius - pixelsPerDip * 2;
        rect = new RectF(point.x - beaconRadius, point.y - beaconRadius, point.x + beaconRadius, point.y + beaconRadius);
        canvas.drawRoundRect(rect, beaconCornerRadius, beaconCornerRadius, primaryFillPaint);
    }

    protected void drawLegend(Canvas canvas) {
        float canvasPadding = canvasWidth * canvasProjection.getPaddingFactor();
        float referenceCanvasWidth = canvasWidth - (2 * canvasPadding);

        Paint legendPaint = new Paint(textPaint);
        legendPaint.setAlpha(50);
        legendPaint.setTextSize(pixelsPerDip * 12);

        float referenceYOffset = canvasHeight - (pixelsPerDip * 16);
        PointF referenceStartPoint = new PointF(canvasPadding, referenceYOffset);
        PointF referenceEndPoint = new PointF(canvasWidth - canvasPadding, referenceYOffset);

        // horizontal line
        canvas.drawRect(
                referenceStartPoint.x,
                referenceStartPoint.y,
                referenceEndPoint.x,
                referenceEndPoint.y - pixelsPerDip,
                legendPaint
        );

        // left vertical line
        canvas.drawRect(
                referenceStartPoint.x,
                referenceStartPoint.y - (pixelsPerDip * 8),
                referenceStartPoint.x + pixelsPerDip,
                referenceStartPoint.y,
                legendPaint
        );

        // right vertical line
        canvas.drawRect(
                referenceEndPoint.x,
                referenceEndPoint.y - (pixelsPerDip * 8),
                referenceEndPoint.x + pixelsPerDip,
                referenceEndPoint.y,
                legendPaint
        );

        // text
        float referenceDistance = (float) canvasProjection.getMetersFromCanvasUnits(referenceCanvasWidth);
        String referenceText = String.valueOf(Math.round(referenceDistance)) + " meters";
        float referenceTextWidth = legendPaint.measureText(referenceText);
        canvas.drawText(
                referenceText,
                (canvasWidth / 2) - (referenceTextWidth / 2),
                referenceStartPoint.y - (pixelsPerDip * 4),
                legendPaint
        );
    }

    protected PointF getPointFromLocation(Location location) {
        float x = canvasProjection.getXFromLocation(location);
        float y = canvasProjection.getYFromLocation(location);
        return new PointF(x, y);
    }

    public void fitToCurrentLocations() {
        topLeftLocation = null;
        bottomRightLocation = null;
        onLocationsChanged();
    }

    private void updateEdgeLocations() {
        List<Location> locations = new ArrayList<>();
        for (Beacon beacon : beacons) {
            locations.add(beacon.getLocation());
        }
        if (deviceLocationAnimator != null) {
            locations.add(deviceLocationAnimator.getLocation());
        }
        if (topLeftLocationAnimator != null) {
            locations.add(topLeftLocationAnimator.getLocation());
        }
        if (bottomRightLocationAnimator != null) {
            locations.add(bottomRightLocationAnimator.getLocation());
        }
        topLeftLocation = EquirectangularProjection.getTopLeftLocation(locations);
        bottomRightLocation = EquirectangularProjection.getBottomRightLocation(locations);

        if (edgeLocationsChanged()) {
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationUpdated(LocationProvider locationProvider, Location location) {
                    if (locationProvider == topLeftLocationAnimator) {
                        canvasProjection.setTopLeftLocation(location);
                    } else if (locationProvider == bottomRightLocationAnimator) {
                        canvasProjection.setBottomRightLocation(location);
                    }
                    invalidate();
                }
            };
            topLeftLocationAnimator = startLocationAnimation(topLeftLocationAnimator, topLeftLocation, locationListener);
            bottomRightLocationAnimator = startLocationAnimation(bottomRightLocationAnimator, bottomRightLocation, locationListener);
        }
    }

    private boolean edgeLocationsChanged() {
        if (topLeftLocationAnimator == null || bottomRightLocationAnimator == null) {
            return true;
        }
        boolean topLeftChanged = !topLeftLocation.latitudeAndLongitudeEquals(topLeftLocationAnimator.getTargetLocation());
        boolean bottomRightChanged = !bottomRightLocation.latitudeAndLongitudeEquals(bottomRightLocationAnimator.getTargetLocation());
        return topLeftChanged || bottomRightChanged;
    }

    @Override
    public void onLocationsChanged() {
        updateEdgeLocations();
        super.onLocationsChanged();
    }

    @Override
    public void onDeviceLocationChanged() {
        startDeviceRadiusAnimation();
        super.onDeviceLocationChanged();
    }

    protected void startDeviceRadiusAnimation() {
        deviceRadiusAnimator = ValueAnimator.ofFloat(0, 1);
        deviceRadiusAnimator.setDuration(LocationAnimator.ANIMATION_DURATION_LONG);
        deviceRadiusAnimator.setRepeatCount(1);
        deviceRadiusAnimator.setRepeatMode(ValueAnimator.REVERSE);
        deviceRadiusAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                invalidate();
            }
        });
        deviceRadiusAnimator.start();
    }

}
