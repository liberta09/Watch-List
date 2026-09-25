package com.kaan.watchlist

import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.kaan.watchlist.navigation.SetupNavGraph
import com.kaan.watchlist.ui.theme.WatchListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WatchListTheme {
                val navController = rememberNavController()
                SetupNavGraph(navController = navController)
            }
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_MOUSE != 0) {
            if (event.action == MotionEvent.ACTION_BUTTON_PRESS && 
                event.buttonState == MotionEvent.BUTTON_PRIMARY) {
                
                val downTime = event.eventTime
                val eventTime = event.eventTime
                val x = event.x
                val y = event.y

                val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
                dispatchTouchEvent(downEvent)
                downEvent.recycle()

                val upEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0)
                dispatchTouchEvent(upEvent)
                upEvent.recycle()

                return true
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }
}
