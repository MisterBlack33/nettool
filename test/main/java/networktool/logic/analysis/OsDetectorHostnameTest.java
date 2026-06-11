package main.java.networktool.logic.analysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OsDetectorHostnameTest {

    private String c(String h) { return OsDetectorHostname.classify(h); }

    // Apple
    @Test void iphone()        { assertTrue(c("my-iphone").contains("iOS")); }
    @Test void ipad()          { assertTrue(c("my-ipad").contains("iPadOS")); }
    @Test void macbook()       { assertTrue(c("johns-macbook-pro").contains("macOS")); }
    @Test void imac()          { assertTrue(c("studio-imac").contains("macOS")); }
    @Test void macmini()       { assertTrue(c("mac-mini").contains("macOS")); }
    @Test void appletv()       { assertEquals("Apple TV", c("appletv")); }
    @Test void homepod()       { assertEquals("HomePod", c("homepod")); }
    @Test void apple_generic() { assertTrue(c("apple-device").contains("Apple")); }

    // Android
    @Test void samsung()       { assertTrue(c("galaxy-s24").contains("Samsung")); }
    @Test void samsung_sm()    { assertTrue(c("sm-a515f").contains("Samsung")); }
    @Test void pixel()         { assertTrue(c("pixel-7a").contains("Pixel")); }
    @Test void huawei()        { assertTrue(c("huawei-p30").contains("Huawei")); }
    @Test void xiaomi()        { assertTrue(c("redmi-note12").contains("Xiaomi")); }
    @Test void poco()          { assertTrue(c("poco-x5").contains("Xiaomi")); }
    @Test void oneplus()       { assertTrue(c("oneplus-nord").contains("OnePlus")); }
    @Test void nothing()       { assertTrue(c("nothing-phone").contains("Nothing")); }
    @Test void oppo()          { assertTrue(c("oppo-reno").contains("OPPO")); }
    @Test void realme()        { assertTrue(c("realme-gt").contains("Realme")); }
    @Test void motorola()      { assertTrue(c("moto-g72").contains("Motorola")); }
    @Test void sony()          { assertTrue(c("xperia-5").contains("Sony")); }
    @Test void nokia()         { assertTrue(c("nokia-g21").contains("Nokia")); }
    @Test void fairphone()     { assertTrue(c("fairphone-4").contains("Fairphone")); }
    @Test void android_gen()   { assertEquals("Android", c("my-android")); }

    // Network
    @Test void fritzbox()      { assertTrue(c("fritz.box").contains("FRITZ")); }
    @Test void unifi()         { assertTrue(c("unifi-ap").contains("Ubiquiti")); }
    @Test void mikrotik()      { assertTrue(c("mikrotik-rb").contains("MikroTik")); }
    @Test void cisco()         { assertTrue(c("cisco-sw").contains("Cisco")); }
    @Test void openwrt()       { assertTrue(c("openwrt-router").contains("OpenWrt")); }
    @Test void switch_()       { assertTrue(c("sw-core").contains("Switch")); }
    @Test void accesspoint()   { assertTrue(c("ap-floor2").contains("Access Point")); }

    // NAS
    @Test void synology()      { assertTrue(c("synology-ds920").contains("Synology")); }
    @Test void diskstation()   { assertTrue(c("diskstation").contains("Synology")); }
    @Test void qnap()          { assertTrue(c("qnap-ts").contains("QNAP")); }
    @Test void truenas()       { assertTrue(c("truenas-scale").contains("TrueNAS")); }
    @Test void nas_generic()   { assertEquals("NAS", c("my-nas")); }

    // Printer
    @Test void printer()       { assertTrue(c("printer-office").contains("Drucker")); }
    @Test void epson()         { assertTrue(c("epson-l3150").contains("Epson")); }
    @Test void canon()         { assertTrue(c("canon-pixma").contains("Canon")); }
    @Test void brother()       { assertTrue(c("brother-mfc").contains("Brother")); }
    @Test void jetdirect()     { assertTrue(c("jetdirect").contains("HP")); }

    // IoT
    @Test void chromecast()    { assertEquals("Chromecast", c("chromecast")); }
    @Test void echo()          { assertTrue(c("amazon-echo").contains("Echo")); }
    @Test void sonos()         { assertEquals("Sonos", c("sonos-play")); }
    @Test void shelly()        { assertTrue(c("shelly-plug").contains("Shelly")); }
    @Test void tasmota()       { assertTrue(c("tasmota-bulb").contains("Tasmota")); }
    @Test void esp32()         { assertTrue(c("esp32-dev").contains("ESP")); }
    @Test void iot_gen()       { assertTrue(c("smart-lamp").contains("IoT")); }

    // Gaming
    @Test void xbox()          { assertEquals("Xbox", c("my-xbox")); }
    @Test void ps5()           { assertTrue(c("ps5-console").contains("PlayStation")); }
    @Test void nintendo()      { assertTrue(c("nintendo-switch").contains("Nintendo")); }

    // Generic
    @Test void raspberry()     { assertTrue(c("raspberrypi").contains("Raspberry")); }
    @Test void ubuntu()        { assertTrue(c("ubuntu-server").contains("Linux")); }
    @Test void windows()       { assertTrue(c("desktop-abc123").contains("Windows")); }
    @Test void ipcam()         { assertTrue(c("ipcam-01").contains("Kamera")); }
    @Test void voip()          { assertTrue(c("voip-phone").contains("VoIP")); }
    @Test void unknown()       { assertNull(c("xyzzy12345")); }
    @Test void null_input()    { assertNull(c(null)); }
}