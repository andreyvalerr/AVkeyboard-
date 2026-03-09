package com.avkeyboard.app.keyboard

import com.avkeyboard.app.keyboard.internal.PopupKeySpec

fun findPopupHintLabel(popupKeys: Array<PopupKeySpec>?, oldHintLabel: String?): String? {
    if (popupKeys == null || oldHintLabel == null) return null
    if (popupKeys.any { it.mLabel == oldHintLabel }) return oldHintLabel
    return popupKeys.firstOrNull { it.mLabel != null }?.mLabel
}