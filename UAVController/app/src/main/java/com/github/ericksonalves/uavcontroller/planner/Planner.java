package com.github.ericksonalves.uavcontroller.planner;

import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.drone.ExperimentalApi;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Planner {
    private final ProductionLine X;
    private final ProductionLine Y;
    private final LatLong client;
    private final LatLong start;
    private final LatLong warehouse;
    private List<PlannerListener> listeners;

    public Planner(double latitude, double longitude) {
        Map<String, Integer> xRule = new HashMap<>();
        xRule.put("A", 2);
        xRule.put("B", 0);
        xRule.put("C", 1);
        Map<String, Integer> yRule = new HashMap<>();
        yRule.put("A", 0);
        yRule.put("B", 2);
        yRule.put("C", 2);
        X = new ProductionLine(-3.098030, -59.975911, xRule, 4);
        Y = new ProductionLine(-3.098101, -59.975911, yRule, 4);
        client = new LatLong(-3.098071, -59.975839);
        start = new LatLong(latitude, longitude);
        warehouse = new LatLong(-3.098071, 59.975988);
        listeners = new ArrayList<>();
    }

    public void addListener(PlannerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PlannerListener listener) {
        listeners.remove(listener);
    }

    public void plan(Drone drone, int xAmount, int yAmount) {
        int time = 0;
        drone.arm(true);
        drone.doGuidedTakeoff(1.0);
        waitAction();
        while (xAmount > 0 || yAmount > 0) {
            if (X.status(time) == ProductionLineStatus.Ready) {
                drone.sendGuidedPoint(X.position, true);
                for (PlannerListener listener : listeners) {
                    listener.onPositionUpdated(X.position.getLatitude(), X.position.getLongitude());
                }
                waitAction();
                time++;
                ExperimentalApi.setServo(drone, 10, 1000);
                waitAction();
                drone.sendGuidedPoint(client, true);
                for (PlannerListener listener : listeners) {
                    listener.onPositionUpdated(client.getLatitude(), client.getLongitude());
                }
                waitAction();
                time++;
                ExperimentalApi.setServo(drone, 10, 1000);
                waitAction();
                xAmount--;
                for (PlannerListener listener : listeners) {
                    listener.onDelivered("X");
                }
                X.status = ProductionLineStatus.Idle;
            } else if (Y.status(time) == ProductionLineStatus.Ready) {
                drone.sendGuidedPoint(Y.position, true);
                for (PlannerListener listener : listeners) {
                    listener.onPositionUpdated(Y.position.getLatitude(), Y.position.getLongitude());
                }
                waitAction();
                time++;
                ExperimentalApi.setServo(drone, 10, 1000);
                waitAction();
                drone.sendGuidedPoint(client, true);
                for (PlannerListener listener : listeners) {
                    listener.onPositionUpdated(client.getLatitude(), client.getLongitude());
                }
                waitAction();
                time++;
                ExperimentalApi.setServo(drone, 10, 1000);
                waitAction();
                yAmount--;
                for (PlannerListener listener : listeners) {
                    listener.onDelivered("Y");
                }
                Y.status = ProductionLineStatus.Idle;
            } else if (xAmount > 0) {
                if (X.canProduce()) {
                    X.produce(time);
                } else {
                    String input = X.getMissing();
                    drone.sendGuidedPoint(warehouse, true);
                    for (PlannerListener listener : listeners) {
                        listener.onPositionUpdated(warehouse.getLatitude(), warehouse.getLongitude());
                    }
                    waitAction();
                    time++;
                    ExperimentalApi.setServo(drone, 10, 1000);
                    waitAction();
                    drone.sendGuidedPoint(X.position, true);
                    for (PlannerListener listener : listeners) {
                        listener.onPositionUpdated(X.position.getLatitude(), X.position.getLongitude());
                    }
                    waitAction();
                    time++;
                    ExperimentalApi.setServo(drone, 10, 1000);
                    waitAction();
                    X.inputs.add(input);
                }
            } else if (yAmount > 0) {
                if (Y.canProduce()) {
                    Y.produce(time);
                } else {
                    String input = Y.getMissing();
                    drone.sendGuidedPoint(warehouse, true);
                    for (PlannerListener listener : listeners) {
                        listener.onPositionUpdated(warehouse.getLatitude(), warehouse.getLongitude());
                    }
                    waitAction();
                    time++;
                    ExperimentalApi.setServo(drone, 10, 1000);
                    waitAction();
                    drone.sendGuidedPoint(Y.position, true);
                    for (PlannerListener listener : listeners) {
                        listener.onPositionUpdated(Y.position.getLatitude(), Y.position.getLongitude());
                    }
                    waitAction();
                    time++;
                    ExperimentalApi.setServo(drone, 10, 1000);
                    waitAction();
                    Y.inputs.add(input);
                }
            }
        }
        drone.sendGuidedPoint(start, true);
        waitAction();
    }

    private void waitAction() {
        try {
            Thread.sleep(250);
        } catch (Exception e) { }
    }
}
