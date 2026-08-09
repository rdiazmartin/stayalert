package com.stayalert.system

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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
    @Volatile
    private var hideRequested = false

    override fun show() {
        scope.launch {
            val start = clock.now()
            try {
                if (!Settings.canDrawOverlays(context)) {
                    onEvent(SessionEvent.OverlayFailed("permiso de overlay revocado"))
                    return@launch
                }
                hideRequested = false
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val view = View(context).apply {
                    setBackgroundColor(Color.BLACK)
                    isClickable = true
                    setOnKeyListener { _, keyCode, _ ->
                        if (keyCode == KeyEvent.KEYCODE_BACK) true else false
                    }
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        setOnApplyWindowInsetsListener { v, insets ->
                            v.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                            insets
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_FULLSCREEN
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
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        LayoutParams.FLAG_FULLSCREEN or
                        LayoutParams.FLAG_KEEP_SCREEN_ON or
                        LayoutParams.FLAG_SECURE,
                    PixelFormat.OPAQUE
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }
                if (hideRequested) {
                    android.util.Log.w("OverlayController", "hide solicitado durante el despliegue; overlay no añadido")
                    return@launch
                }
                wm.addView(view, params)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                }
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

    override suspend fun hide() {
        hideRequested = true
        try {
            if (overlayView != null && windowManager == null) {
                throw IllegalStateException("overlayView presente sin windowManager")
            }
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            android.util.Log.e("OverlayController", "No se pudo ocultar el overlay", e)
            throw e
        } finally {
            overlayView = null
            windowManager = null
            runCatching { patternDetector.reset() }
        }
    }

    override fun isVisible(): Boolean = overlayView != null
}
