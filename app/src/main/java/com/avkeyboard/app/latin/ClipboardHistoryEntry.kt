// SPDX-License-Identifier: GPL-3.0-only

package com.avkeyboard.app.latin

import com.avkeyboard.app.latin.settings.Settings

class ClipboardHistoryEntry(
    val id: Long,
    var timeStamp: Long,
    var isPinned: Boolean,
    val text: String
) : Comparable<ClipboardHistoryEntry> {
    override fun compareTo(other: ClipboardHistoryEntry): Int {
        val result = other.isPinned.compareTo(isPinned)
        if (result == 0) return other.timeStamp.compareTo(timeStamp)
        if (Settings.getValues()?.mClipboardHistoryPinnedFirst == false) return -result
        return result
    }
}
