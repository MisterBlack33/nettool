package main.java.networktool.storage;

/**
 * Canonical identifiers for JUnit test entries.
 *
 * All test-created networks/profiles use the TEST_PREFIX so that:
 *  - NetworkStore.getAll() / getAllHosts() excludes them from GUI
 *  - GuiNetworkMap skips them
 *  - They are clearly identifiable in logs
 *
 * Display names follow the pattern "test.eintrag.N" (N starting at 1).
 */
public final class TestConstants {

    private TestConstants() {}

    /** Prefix that marks any network/profile as a JUnit test artifact. */
    public static final String TEST_PREFIX = "__junit__";

    // ── Network names ─────────────────────────────────────────────────────

    public static final String NET_STANDARD  = TEST_PREFIX + "standard";
    public static final String NET_EXT       = TEST_PREFIX + "ext";
    public static final String NET_FIX       = TEST_PREFIX + "fix";
    public static final String NET_RENAME_SRC = TEST_PREFIX + "rename_src";
    public static final String NET_RENAME_DST = TEST_PREFIX + "rename_dst";
    public static final String NET_NETWORK    = TEST_PREFIX + "network";

    // ── Profile names ─────────────────────────────────────────────────────

    public static final String PROFILE_STANDARD = TEST_PREFIX + "profile";
    public static final String PROFILE_EXT      = TEST_PREFIX + "profile_ext";
    public static final String PROFILE_SCHED    = TEST_PREFIX + "sched";
    public static final String PROFILE_SCHED_FIX = TEST_PREFIX + "sched_fix";

    // ── Display host names (shown in GUI if ever leaked) ──────────────────

    public static final String HOST_1 = "test.eintrag.1";
    public static final String HOST_2 = "test.eintrag.2";
    public static final String HOST_3 = "test.eintrag.3";
    public static final String HOST_4 = "test.eintrag.4";
    public static final String HOST_5 = "test.eintrag.5";
    public static final String HOST_6 = "test.eintrag.6";
    public static final String HOST_7 = "test.eintrag.7";
    public static final String HOST_8 = "test.eintrag.8";
    public static final String HOST_9 = "test.eintrag.9";

    // ── Test IP ranges (RFC 5737 documentation range — never routable) ────

    public static final String IP_1 = "88.88.0.1";
    public static final String IP_2 = "88.88.0.2";
    public static final String IP_3 = "88.88.0.3";
    public static final String IP_4 = "88.88.0.4";
    public static final String IP_5 = "88.88.0.5";
    public static final String IP_6 = "88.88.0.9";
    public static final String IP_7 = "99.99.0.1";
    public static final String IP_8 = "99.99.0.2";
    public static final String IP_9 = "99.99.0.3";
    public static final String IP_10 = "99.99.0.4";

    public static final String PREFIX_88 = "88.88.";
    public static final String PREFIX_99 = "99.99.";

    // ── Import/export category names ──────────────────────────────────────

    public static final String IMPORT_CAT     = TEST_PREFIX + "ImportCat";
    public static final String FIX_IMPORT_CAT = TEST_PREFIX + "FixImportCat";
}