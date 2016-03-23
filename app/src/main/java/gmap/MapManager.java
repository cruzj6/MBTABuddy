package gmap;

import android.content.Context;

import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import com.Activities.StationActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import directions.AbstractRouting;
import directions.RouteException;
import directions.Routing;
import directions.RoutingListener;
import mbta.Line;
import mbta.Lines;
import mbta.MBTA;
import mbta.Station;
import mbta.mbtaAPI.Vehicle;
import mbta.mbtabuddy.R;

/**
 * Created by cruzj6 on 2/10/2016.
 */
public class MapManager implements RoutingListener{

    private Context context;
    private GoogleMap map;
    private Marker myMarker;

    //TrainMarker objects, keys being their MBTA trip num from MBTA api
    private List<TrainMarker> trainMarkers = new ArrayList<TrainMarker>();
    private List<StationMarker> stationMarkers = new ArrayList<StationMarker>();

    public MapManager(){

    }

    public MapManager(Context context){
        this.context = context;
    }

    public MapManager(Context context, GoogleMap map){
        this.context = context;
        this.map = map;
    }

    public void SetContext(Context con) {
        context = con;
    }

    public void setMap(GoogleMap map) {
        this.map = map;
        this.map.setInfoWindowAdapter(new TrainInfoWindow(context, this));
        this.map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //For train markers
                if (getTrainMarkerFromId(marker.getId()) != null) {
                    //...
                    return false;//We want to show info window
                }
                //For station markers
                if (getStationMarkerById(marker.getId()) != null) {
                    //...
                    Intent intent = new Intent(context, StationActivity.class);
                    intent.putExtra("ID", marker.getTitle());
                    context.startActivity(intent);
                }
                return true;//True overrides default behavior
            }
        });
    }

    public void zoomToLocation(LatLng location, int zoomAmnt) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomAmnt));
    }

    //region TrainMarkers

    // Move a train already on the map
    public void moveTrainMarker(String tripNum, LatLng newPos) {
        TrainMarker train = getTrainMarkerFromVehicleNum(tripNum);
        train.GetMarker().setPosition(newPos);
    }

    public TrainMarker getTrainMarkerFromId(String markerId) {
        for (TrainMarker train : trainMarkers) {
            String id = train.GetMarker().getId();
            if (id.equals(markerId)) {
                return train;
            }
        }
        return null;
    }

    /**
     * @param vehicleNum Given by MBTA API
     * @return TrainMarker object associated with this tripNum on creation
     */
    public TrainMarker getTrainMarkerFromVehicleNum(String vehicleNum) {
        for (TrainMarker train : trainMarkers) {
            if (train.GetVehicleNum().equals(vehicleNum)) {
                return train;
            }
        }

        return null;
    }

    public void zoomTwoPoints(LatLng swPoint, LatLng nePoint) {
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(swPoint, nePoint), 10));
    }

    public void zoomToTrainMarker(String vehicleNum, int zoomNum) {
        TrainMarker train = getTrainMarkerFromVehicleNum(vehicleNum);
        LatLng position = train.GetMarker().getPosition();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoomNum));
    }

    //Add a train to the map
    public void addTrainMarker(String vehicleNum, LatLng location, String title, Line line) {
        Marker newMarker = map.addMarker(new MarkerOptions()
                        .position(location)
                        .title(title)
        );
        TrainMarker newtm = new TrainMarker(line, newMarker, vehicleNum, context);
        trainMarkers.add(newtm);
    }

    public void addTrainMarker(String vehicleNum, LatLng location, String title, Line line, String statId){
        Marker newMarker = map.addMarker(new MarkerOptions()
                        .position(location)
                        .title(title)
        );
        TrainMarker newtm = new TrainMarker(line, newMarker, vehicleNum, context);
        newtm.SetSetStationId(statId);
        trainMarkers.add(newtm);
    }


    //endregion

    //region StationMarkers
    public StationMarker getStationMarker(String stationName) {
        for (StationMarker stat : stationMarkers) {
            if (stat.getStationName().equals(stationName))
                return stat;
        }

        return null;
    }

    public StationMarker getStationMarkerById(String markerId) {
        for (StationMarker stat : stationMarkers) {
            String id = stat.getMarker().getId();
            if (id.equals(markerId)) {
                return stat;
            }
        }

        return null;
    }


    public void zoomToStationMarker(String stationName, int zoomNum) {
        StationMarker sm = getStationMarker(stationName);
        LatLng pos = sm.getMarker().getPosition();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, zoomNum));
    }

    public void addStationMarker(String stationName, LatLng location) {
        Marker newMarker = map.addMarker(new MarkerOptions()
                        .position(location)
                        .title(stationName)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_station)));
        StationMarker newsm = new StationMarker(stationName, newMarker);
        stationMarkers.add(newsm);
    }
    //endregion

    //region MyLocation
    public void moveMyLocationMarker(LatLng newLoc) {
        if(myMarker == null) {
            addMyLocationMarker("Me", newLoc);
        }
        else {
            myMarker.setPosition(newLoc);
        }
    }

    public void addMyLocationMarker(String title, LatLng location) {
        //TODO: Make icon smaller
        if(map != null) {
            Marker meMarker = map.addMarker(new MarkerOptions()
                            .position(location)
                            .title(title)
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_myloc)));
            myMarker = meMarker;
        }
    }
    //endregion

    public void drawLine(Line line) {
        if(line.getMapPoints(this.context) == null) {
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.TRANSIT)
                    .waypoints(line.getTerminalStation1().getLatLan(), line.getTerminalStation2().getLatLan())
                    .key("AIzaSyAuq6B6ktEChZCEfB-LbwyxshF44bWKItM")
                    .withListener(this)
                    .withLine(line)
                    .build();
            routing.execute();
        } else {
            line.drawLine(map);
        }
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        Log.v("MBTA", "ROUTE FAILED");
    }

    @Override
    public void onRoutingStart() {
        Log.v("MBTA", "ROUTE START");
    }

    @Override
    public void onRoutingSuccess(ArrayList<directions.Route> route, int shortestRouteIndex,Line line) {
        route.get(0).getPoints().remove(0);
        route.get(0).getPoints().remove(route.get(0).getPoints().size() - 1);
        line.setMapPoints(route.get(0).getPoints());
        line.drawLine(map);
        line.adjustStations();
    }

    private void moveStations(Line line) {
        for(Station station:line.getStations()){
            StationMarker stationMarker = this.getStationMarker(station.getStationID());
            stationMarker.move(station.getLatLan());
        }
    }

    @Override
    public void onRoutingCancelled() {
        Log.v("MBTA", "ROUTE CANCELLED");
    }

    public void drawAllTrainLines(){
        for(Line line: Lines.getInstance().values()){
            this.drawLine(line);
        }
    }

    public void drawAllStations(){
        for(Line line: Lines.getInstance().values()){
            this.drawStations(line);
        }
    }

    private void drawStations(Line line) {
        for(Station station : line.getStations()){
            this.addStationMarker(station.getStationID(), station.getLatLan());
        }
    }

    public void drawStations(List<Line> lines) {
        for(Line line: lines){
            for(Station station : line.getStations()){
                this.addStationMarker(station.getStationID(), station.getLatLan());
            }
        }
    }

    public void drawTrainLines(List<Line> lines) {
        for(Line line:lines){
            this.drawLine(line);
        }
    }

    public void drawAdjustedStations(List<Line> lines) {
        for(Line line: lines){
            line.adjustStations();
            for(Station station : line.getStations()){
                this.addStationMarker(station.getStationID(), station.getLatLan());
            }
        }
    }

    public void drawAdjustedStations(Line line) {
        line.adjustStations();
        for(Station station : line.getStations()){
            this.addStationMarker(station.getStationID(), station.getLatLan());
        }
    }

    public void addTrains(List<Line> lines, Station station) {
        MBTA mbta = MBTA.getInstance();
        for(Line line: lines) {
            for (Vehicle vehicle : MBTA.getInstance().getVehiclesByRoute(line)) {
                line.adjustVehicles(vehicle);
                this.addTrainMarker(vehicle.getVehicleId(), vehicle.getLatLng(), "TESTING", line, station.getStationID());
            }
        }
    }
}
