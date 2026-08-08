package com.stayalert.system

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.stayalert.domain.Clock
import com.stayalert.domain.OverlayController
import com.stayalert.domain.PatternDetector
import com.stayalert.domain.SessionConstants
import com.stayalert.domain.SessionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SystemOverlayController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val patternDetector: PatternDetector,
    private val onEvent: (SessionEvent) -> Unit
) : OverlayController {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    override fun show() {
        scope.launch {
            val start = clock.now()
            try {
                if (!Settings.canDrawOverlays(context)) {
                    onEvent(SessionEvent.OverlayFailed("permiso de overlay revocado"))
                    return@launch
                }
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = View(context).apply {
                    setBackgroundColor(Color.BLACK)
                    setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            val detected = patternDetector.onTouch(
                                x = event.x,
                                y = event.y,
                                width = width.toFloat(),
                                height = height.toFloat()
                            )
                            if (detected) {
                                onEvent(SessionEvent.PatternDetected)
                            }
                        }
                        true
                    }
                }
                val params = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        LayoutParams.TYPE_PHONE
                    },
                    LayoutParams.FLAG_NOT_FOCUSABLE or
                        LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        LayoutParams.FLAG_KEEP_SCREEN_ON or
                        LayoutParams.FLAG_SECURE,
                    PixelFormat.OPAQUE
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }
                wm.addView(view, params)
                overlayView = view
                windowManager = wm
                val deployTime = clock.now() - start
                if (deployTime > SessionConstants.OVERLAY_DEPLOY_SLA_MS) {
                    android.util.Log.w("OverlayController", "Overlay desplegado en ${deployTime}ms (SLA ${SessionConstants.OVERLAY_DEPLOY_SLA_MS}ms)")
                }
                onEvent(SessionEvent.OverlayShown)
            } catch (e: Exception) {
                android.util.Log.e("OverlayController", "No se pudo desplegar el overlay", e)
                onEvent(SessionEvent.OverlayFailed(e.message ?: "desconocido"))
            }
        }
    }

    override fun hide() {
        scope.launch {
            try {
                overlayView?.let { view ->
                    windowManager?.removeView(view)
                }
            } catch (e: Exception) {
                android.util.Log.e("OverlayController", "No se pudo ocultar el overlay", e)
            } finally {
                overlayView = null
                windowManager = null
                patternDetector.reset()
            }
        }
    }

    override fun isVisible(): Boolean = overlayView != null
}
