package main.java.networktool.logic.visualize;

/** Ein Traffic-Delta-Sample (Rx/Tx-Bytes seit letzter Messung) für die Diagramm-Anzeige. */
public record TrafficSample(long timestampMs, long rxDelta, long txDelta) {}