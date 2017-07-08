package com.github.ericksonalves.uavcontroller.planner;

import com.o3dr.services.android.lib.coordinate.LatLong;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductionLine {
    public final LatLong position;
    public final Map<String, Integer> rule;
    public List<String> inputs;
    public ProductionLineStatus status;
    public int start;
    public int waitTime;

    public ProductionLine(double latitude, double longitude, Map<String, Integer> rule, int waitTime) {
        position = new LatLong(latitude, longitude);
        this.rule = rule;
        inputs = new ArrayList<>();
        status = ProductionLineStatus.Idle;
        start = 0;
        this.waitTime = waitTime;
    }

    public boolean canProduce() {
        for (String key : rule.keySet()) {
            int count = countInputsOfType(key);
            if (count < rule.get(key)) {
                return false;
            }
        }
        return true;
    }

    public int countInputsOfType(String type) {
        int count = 0;
        for (String input : inputs) {
            if (input.equals(type)) {
                count++;
            }
        }
        return count;
    }

    public String getMissing() {
        for (String key : rule.keySet()) {
            int count = countInputsOfType(key);
            if (count < rule.get(key)) {
                return key;
            }
        }
        return "";
    }

    public void produce(int time) {
        status = ProductionLineStatus.Producing;
        for (String key : rule.keySet()) {
            int amount = rule.get(key);
            while (amount-- > 0) {
                inputs.remove(key);
            }
        }
        start = time;
    }

    public ProductionLineStatus status(int time) {
        if (status == ProductionLineStatus.Producing && time - start >= waitTime) {
            status = ProductionLineStatus.Ready;
        }
        return status;
    }
}
