package main.java.networktool.logic.scan.schedule;

import java.time.LocalTime;

/** Tägliches Wake-on-LAN-Ziel: Geräte-MAC, Broadcast-Adresse und Uhrzeit. */
public record WolSchedule(String mac, String broadcast, LocalTime time) {}
