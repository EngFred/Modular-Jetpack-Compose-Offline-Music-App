package com.engfred.musicplayer.core.util

/**
 * Small text utilities.
 *
 * CHANGE: Added to centralize pluralization logic so the UI does not show "1 songs".
 *
 * Note: This is intentionally simple and English-only. If you want i18n/localized resources
 * use Android string resources with quantity (plurals.xml) instead.
 */
object TextUtils {
    /**
     * Return a correctly pluralized phrase for a given count.
     *
     * Examples:
     *  pluralize(1, "song") -> "1 song"
     *  pluralize(2, "song") -> "2 songs"
     *  pluralize(0, "song") -> "0 songs"
     *
     * @param count number of items
     * @param singular singular noun (e.g., "song")
     * @param plural optional explicit plural form (e.g., "children"). If null we append 's'.
     */
    fun pluralize(count: Int, singular: String, plural: String? = null): String {
        return if (count == 1) {
            "1 $singular"
        } else {
            val pluralForm = plural ?: (singular + "s")
            "$count $pluralForm"
        }
    }
}