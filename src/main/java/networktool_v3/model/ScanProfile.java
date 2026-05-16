package main.java.networktool_v3.model;

import java.util.ArrayList;
import java.util.List;

public final class ScanProfile {

    public String       name;
    public List<String> cidrs    = new ArrayList<>();
    public String       osFilter = "";
    public String       hnFilter = "";
    public List<Integer> ports   = new ArrayList<>();
    public boolean      autoSave = false;
    public String       category = "";
    public String       lastRun  = "";

    public ScanProfile(String name) {
        this.name = name;
    }

    public String summary() {
        return name + "  ["
                + (cidrs.isEmpty() ? "lokal" : String.join(", ", cidrs))
                + (osFilter.isBlank() ? "" : "  OS:" + osFilter)
                + (hnFilter.isBlank() ? "" : "  HN:" + hnFilter)
                + "]";
    }
}