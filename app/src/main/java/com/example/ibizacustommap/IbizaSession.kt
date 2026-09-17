package com.example.ibizacustommap

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class IbizaSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        // Here we tell it to launch our new GT3 canvas
        return MainScreen(carContext)
    }
}