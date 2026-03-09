package com.avkeyboard.app.latin.utils

import android.content.Context
import android.content.Intent
import com.avkeyboard.app.keyboard.internal.keyboard_parser.floris.KeyCode
import com.avkeyboard.app.latin.inputlogic.InputLogic
import com.avkeyboard.app.latin.utils.Log.i

object IntentUtils {
    val TAG: String = InputLogic::class.java.simpleName
    private const val ACTION_SEND_INTENT = "com.avkeyboard.app.latin.ACTION_SEND_INTENT"
    private const val EXTRA_NUMBER = "EXTRA_NUMBER"

    @JvmStatic
    fun handleSendIntentKey(context: Context, mKeyCode: Int) {
        val intentNumber = (KeyCode.SEND_INTENT_ONE + 1) - mKeyCode

        val intent: Intent = Intent(ACTION_SEND_INTENT).apply {
            putExtra(EXTRA_NUMBER, intentNumber)
        }

        context.sendBroadcast(intent)
        i(TAG, "Sent broadcast for intent number: $intentNumber")
    }
}

