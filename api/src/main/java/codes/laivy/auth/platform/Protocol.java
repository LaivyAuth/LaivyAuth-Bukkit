package codes.laivy.auth.platform;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public enum Protocol {

    v1_0_0("1.0.0", 22),

    v1_1_0("1.1.0", 23),

    v1_2_2("1.2.1-3", 23),
    v1_2_4("1.2.4/5", 29),

    v1_3_1("1.3.1/2", 39),

    v1_4_2("1.4.0-2", 47),
    v1_4_3("1.4.3", 48),
    v1_4_4("1.4.4/5", 49),
    v1_4_6("1.4.6/7", 51),

    v1_5_0("1.5.0/1", 60),
    v1_5_2("1.5.2", 61),

    v1_6_0("1.6.0", 72),
    v1_6_1("1.6.1", 73),
    v1_6_2("1.6.2", 74),
    v1_6_4("1.6.4", 78),

    v1_7_1("1.7.1-5)", 4),
    v1_7_6("1.7.6)", 5),

    v1_8_0("1.8.0)", 47),

    v1_9_0("1.9.0/1)", 107),
    v1_9_2("1.9.2/3)", 109),
    v1_9_4("1.9.4)", 110),

    v1_10_0("1.10.0", 210),

    v1_11_0("1.11.0", 315),
    v1_11_1("1.11.1", 316),

    v1_12_0("1.12.0", 335),
    v1_12_1("1.12.1", 338),
    v1_12_2("1.12.2", 340),

    v1_13_0("1.13.0", 393),
    v1_13_1("1.13.1", 401),
    v1_13_2("1.13.2", 404),

    v1_14_0("1.14.0", 477),
    v1_14_1("1.14.1", 480),
    v1_14_2("1.14.2", 485),
    v1_14_3("1.14.3", 490),
    v1_14_4("1.14.4", 498),

    v1_15_0("1.15.0", 573),
    v1_15_1("1.15.1", 575),
    v1_15_2("1.15.2", 578),

    v1_16_0("1.16.0", 735),
    v1_16_1("1.16.1", 736),
    v1_16_2("1.16.2", 751),
    v1_16_3("1.16.3", 753),
    v1_16_4("1.16.4", 754),
    v1_16_5("1.16.5", 754),

    v1_17_0("1.17.0", 755),
    v1_17_1("1.17.1", 756),

    v1_18_0("1.18.0", 757),
    v1_18_1("1.18.1", 757),
    v1_18_2("1.18.2", 758),

    v1_19_0("1.19.0", 759),
    v1_19_1("1.19.1", 760),
    v1_19_2("1.19.2", 760),
    v1_19_3("1.19.3", 761),
    v1_19_4("1.19.4", 762),

    v1_20_0("1.20.0/1", 763),
    v1_20_2("1.20.2", 764),
    v1_20_3("1.20.3/4", 765),
    v1_20_5("1.20.5", 766),

    v1_21_0("1.21.0/1", 767),
    v1_21_2("1.21.2", 768),
    ;

    // Static initializers

    public static @NotNull Protocol getByProtocol(int protocol) {
        return Arrays.stream(values()).filter(version -> version.getVersion() == protocol).findFirst().orElseThrow(() -> new IllegalArgumentException("there's no available version with protocol '" + protocol + "'"));
    }

    // Object

    private final @NotNull String name;
    private final int version;

    Protocol(@NotNull String name, int version) {
        this.name = name;
        this.version = version;
    }

    // Getters

    public @NotNull String getName() {
        return name;
    }
    public int getVersion() {
        return version;
    }

}
