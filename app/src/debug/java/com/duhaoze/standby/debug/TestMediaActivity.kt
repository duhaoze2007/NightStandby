package com.duhaoze.standby.debug

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class TestMediaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForegroundService(Intent(this, TestMediaService::class.java))
        finish()
    }
}
