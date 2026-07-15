package com.example.common.constants;

/** Generic constants shared across services (default pagination, date formats, etc). */
public final class AppConstants {

    private AppConstants() {
    }

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
}
