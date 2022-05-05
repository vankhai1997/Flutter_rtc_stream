package com.example.fltuter_rtc_cc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.PermissionChecker.checkCallingOrSelfPermission
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import de.tavendo.autobahn.WebSocket
import io.antmedia.webrtcandroidframework.*
import io.antmedia.webrtcandroidframework.apprtc.CallActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import org.webrtc.DataChannel
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import java.util.*


@SuppressLint("WrongConstant")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class AndroidRTCView(
    var activity: Activity,
    var context: Context,
    messenger: BinaryMessenger?,
) :
    PlatformView, MethodChannel.MethodCallHandler, LifecycleOwner, IWebRTCListener,
    IDataChannelObserver {
    val RECONNECTION_PERIOD_MLS = 100

    private var mContainer = FrameLayout(context)

    private val surfaceViewRender: SurfaceViewRenderer by lazy {
        SurfaceViewRenderer(context)
    }


    private val pipViewRenderer: SurfaceViewRenderer by lazy {
        SurfaceViewRenderer(context)
    }
    private var stoppedStream = false

    private var webRTCClient: WebRTCClient? = null
    private val webRTCMode = IWebRTCClient.MODE_PUBLISH

    private val mRegistry = LifecycleRegistry(this)
    private var channel: MethodChannel =
        MethodChannel(messenger, "com.example.fltuter_rtc_cc/webrtc")

    var reconnectionHandler = Handler()
    private var reconnectionRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!webRTCClient!!.isStreaming) {
                attempt2Reconnect()
                // call the handler again in case startStreaming is not successful
                reconnectionHandler.postDelayed(this, RECONNECTION_PERIOD_MLS.toLong())
            }
        }
    }


    init {
        activity.window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        channel.setMethodCallHandler(this)
        mContainer.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        mContainer.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mContainer.addView(pipViewRenderer)
        mContainer.addView(surfaceViewRender)


        // Check for mandatory permissions.
        for (permission: String in CallActivity.MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(
                    activity,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    activity,
                    "Permission $permission is not granted",
                    Toast.LENGTH_SHORT
                )
                    .show()
                break

            }
        }

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCameraIfReady() {

    }

    private fun attempt2Reconnect() {
        Log.w(javaClass.simpleName, "Attempt2Reconnect called")
        if (!webRTCClient!!.isStreaming) {
            webRTCClient!!.startStream()
            if (webRTCMode === IWebRTCClient.MODE_JOIN) {
                pipViewRenderer.setZOrderOnTop(true)
            }
        }
    }

    override fun getView(): View {
        return mContainer
    }

    private fun onInitStream(call: MethodCall) {
        val socketURL = call.argument<String>("socketURL")
        val streamId = call.argument<String>("streamId")
        val tokenId = call.argument<String>("tokenId")
        val intent = Intent()
        intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, true)
        intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, 30)
        intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, 1500)
        intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, true)
        intent.putExtra(CallActivity.EXTRA_DATA_CHANNEL_ENABLED, true)

        webRTCClient = WebRTCClient(this, activity)
        webRTCClient!!.setVideoRenderers(surfaceViewRender, pipViewRenderer)

        webRTCClient!!.init(socketURL, streamId, webRTCMode, tokenId, intent)
        webRTCClient!!.setDataChannelObserver(this)

    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "switchCamera" -> onCameraSwitch()
            "init" -> onInitStream(call)
            "onOffAudio" -> onOffAudio()
            "startStreaming" -> startStreaming()
            "stopStreaming" -> stopStreaming()
            "onOffVideo" -> onOffVideo()
            else -> { // Note the block
                print("x is neither 1 nor 2")
            }
        }
    }

    override fun dispose() {
        mRegistry.currentState = Lifecycle.State.CREATED
        mRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle(): Lifecycle {
        return mRegistry
    }

    override fun onDisconnected(streamId: String?) {
        Toast.makeText(context, "Disconnected", Toast.LENGTH_LONG).show()
        // handle reconnection attempt
        if (!stoppedStream) {
            Toast.makeText(context, "Disconnected Attempting to reconnect", Toast.LENGTH_LONG)
                .show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!reconnectionHandler.hasCallbacks(reconnectionRunnable)) {
                    reconnectionHandler.postDelayed(
                        reconnectionRunnable,
                        RECONNECTION_PERIOD_MLS.toLong()
                    )
                }
            } else {
                reconnectionHandler.postDelayed(
                    reconnectionRunnable,
                    RECONNECTION_PERIOD_MLS.toLong()
                )
            }
        } else {
            Toast.makeText(context, "Stopped the stream", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPublishFinished(streamId: String?) {
        invokeMethod("onPublishFinished")

    }

    override fun onPlayFinished(streamId: String?) {
        invokeMethod("onPlayFinished")

    }


    override fun onPublishStarted(streamId: String?) {
        invokeMethod("onPublishStarted")

    }

    override fun onPlayStarted(streamId: String?) {
        webRTCClient!!.switchVideoScaling(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        webRTCClient!!.getStreamInfoList()
    }

    override fun noStreamExistsToPlay(streamId: String?) {
    }

    override fun onError(description: String?, streamId: String?) {
    }

    override fun onSignalChannelClosed(
        code: WebSocket.WebSocketConnectionObserver.WebSocketCloseNotification?,
        streamId: String?
    ) {
    }

    override fun streamIdInUse(streamId: String?) {
    }

    override fun onIceConnected(streamId: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (reconnectionHandler.hasCallbacks(reconnectionRunnable)) {
                reconnectionHandler.removeCallbacks(reconnectionRunnable, null)
            }
        } else {
            reconnectionHandler.removeCallbacks(reconnectionRunnable, null)
        }
    }

    override fun onIceDisconnected(streamId: String?) {
        invokeMethod("onIceDisconnected")

    }

    override fun onTrackList(tracks: Array<out String>?) {
        invokeMethod("onTrackList")

    }

    override fun onBitrateMeasurement(
        streamId: String?,
        targetBitrate: Int,
        videoBitrate: Int,
        audioBitrate: Int
    ) {
        Log.e(
            javaClass.simpleName,
            "st:$streamId tb:$targetBitrate vb:$videoBitrate ab:$audioBitrate"
        )
        if (targetBitrate < (videoBitrate + audioBitrate)) {
            Toast.makeText(context, "low bandwidth", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStreamInfoList(streamId: String?, streamInfoList: ArrayList<StreamInfo>?) {
        Log.d(MainActivity::class.java.name, "onStreamInfoList ")
        invokeMethod("onStreamInfoList")

    }

    override fun onBufferedAmountChange(previousAmount: Long, dataChannelLabel: String?) {
        Log.d(MainActivity::class.java.name, "onBufferedAmountChange ")
        invokeMethod("onBufferedAmountChange")

    }

    override fun onStateChange(state: DataChannel.State?, dataChannelLabel: String?) {
        Log.d(MainActivity::class.java.name, "Data channel state changed: ")
        invokeMethod("onStateChange")
    }

    override fun onMessage(buffer: DataChannel.Buffer?, dataChannelLabel: String?) {

    }

    override fun onMessageSent(buffer: DataChannel.Buffer?, successful: Boolean) {

    }

    private fun invokeMethod(method: String) {
        channel.invokeMethod(method, null, object : MethodChannel.Result {
            override fun success(o: Any?) {
                Log.d("Results", o.toString())
            }

            override fun error(s: String, s1: String?, o: Any?) {}
            override fun notImplemented() {}
        })
    }

    private fun onOffVideo() {
        if (webRTCClient!!.isVideoOn) {
            webRTCClient!!.disableVideo()
        } else {
            webRTCClient!!.enableVideo()
        }
    }

    fun startStreaming() {
        if (!webRTCClient!!.isStreaming) {
            webRTCClient!!.startStream()
            if (webRTCMode === IWebRTCClient.MODE_JOIN) {
                pipViewRenderer.setZOrderOnTop(true)
            }
            stoppedStream = false
        }
    }

    fun stopStreaming() {
        if (webRTCClient!!.isStreaming) {
            webRTCClient!!.stopStream()
            stoppedStream = true
        }
    }

    private fun onOffAudio() {
        if (webRTCClient!!.isAudioOn) {
            webRTCClient!!.disableAudio()
        } else {
            webRTCClient!!.enableAudio()
        }
    }

    fun onCameraSwitch() {
        webRTCClient!!.onCameraSwitch()
    }

    companion object {
        /**
         * Change this address with your Ant Media Server address
         */
        val SERVER_ADDRESS = "app.evgcdn.net"
        val SERVER_URL = "ws://" + SERVER_ADDRESS + "/vdone/websocket"
        val REST_URL = "http://" + SERVER_ADDRESS + "/vdone/rest/v2"
    }
}