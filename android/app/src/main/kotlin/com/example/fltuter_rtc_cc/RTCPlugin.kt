package com.example.fltuter_rtc_cc

import android.app.Activity
import android.content.Context
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel


class RTCPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var applicationContext: Context? = null
    private var mActivity: Activity? = null
    private var mFlutterPluginBinding: FlutterPlugin.FlutterPluginBinding? = null

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mActivity = binding.activity

    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        mActivity = binding.activity
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        WebRTCViewPlugin.registerWith(mFlutterPluginBinding!!, mActivity!!)
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.v_belero.live_stream/webrtc")
        channel.setMethodCallHandler(this)
        applicationContext = flutterPluginBinding.applicationContext
        mFlutterPluginBinding = flutterPluginBinding


    }

    override fun onDetachedFromActivityForConfigChanges() {
        mActivity = null
    }

    override fun onDetachedFromActivity() {
        mActivity = null
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}

object WebRTCViewPlugin {
    fun registerWith(binding: FlutterPlugin.FlutterPluginBinding, activity: Activity) {
        //注册相机

        binding. flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("view1", AndroidRTCFactory(binding.binaryMessenger, activity))
//        binding.platformViewRegistry.registerViewFactory(
//            "view1",
//            AndroidRTCFactory(binding.binaryMessenger, activity)
//        )
    }
}