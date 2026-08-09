package com.microsoft.teams.activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent

class MainActivity : Activity() {

    companion object {
        private const val TAG = "MockTeams"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "lifecycle created")
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "lifecycle resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "lifecycle paused")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "lifecycle stopped")
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            Log.i(TAG, "touch x=${ev.x} y=${ev.y}")
        }
        return super.dispatchTouchEvent(ev)
    }
}
