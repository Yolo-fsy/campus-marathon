package com.campus.marathon.model;

/**
 * 参赛项目及名额配置。
 */
public enum EventCategory {

    HALF("HALF", "半程马拉松", "21.0975 km", 300),
    MINI("MINI", "迷你马拉松", "5 km", 500),
    FUN("FUN", "欢乐跑", "3 km", 800);

    private final String code;
    private final String label;
    private final String distance;
    private final int capacity;

    EventCategory(String code, String label, String distance, int capacity) {
        this.code = code;
        this.label = label;
        this.distance = distance;
        this.capacity = capacity;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDistance() {
        return distance;
    }

    public int getCapacity() {
        return capacity;
    }

    public static EventCategory fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (EventCategory category : values()) {
            if (category.code.equalsIgnoreCase(code.trim())) {
                return category;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
