package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.AzanPlayerService

class StopAzanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val stopIntent = Intent(context, AzanPlayerService::class.java).apply {
            action = AzanPlayerService.ACTION_STOP_AZAN
        }
        context.startService(stopIntent)
    }
}
