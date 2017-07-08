package com.github.ericksonalves.uavcontroller.planner;

public interface PlannerListener {
    void onDelivered(String product);
    void onPositionUpdated(double latitude, double longitude);
}
