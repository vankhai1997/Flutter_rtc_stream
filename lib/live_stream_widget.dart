import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class LiveStreamWidget extends StatefulWidget {
  final double? width;
  final double? height;

  const LiveStreamWidget({
    Key? key,
    this.width,
    this.height,
  }) : super(key: key);

  @override
  LiveStreamWidgetState createState() => LiveStreamWidgetState();
}

class LiveStreamWidgetState extends State<LiveStreamWidget> {
  final String _viewType = "liveStreamView";
  final MethodChannel _channel =
      const MethodChannel("com.example.fltuter_rtc_cc/webrtc");

  //是否后置摄像头
  bool? isBackamera = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance!.addPostFrameCallback((timeStamp) {
      init();
    });
  }

  void init() {
    Map<String, dynamic> arguments = {};
    arguments["socketURL"] = "wss://app.evgcdn.net/vdone/websocket";
    arguments["streamId"] = "123456";
    _channel.invokeMethod('init', arguments);
  }

  void switchCamera() {
    _channel.invokeMethod('switchCamera');
  }

  void onOffAudio() {
    _channel.invokeMethod('onOffAudio');
  }

  void startStreaming() {
    _channel.invokeMethod('startStreaming');
  }

  void stopStreaming() {
    _channel.invokeMethod('stopStreaming');
  }

  void onOffVideo() {
    _channel.invokeMethod('onOffVideo');
  }

  void _registerChannel(int id) {
    _channel.setMethodCallHandler(_platformCallHandler);
  }

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return SizedBox(
        width: widget.width ?? MediaQuery.of(context).size.width,
        height: widget.height ?? MediaQuery.of(context).size.height,
        child: Stack(
          children: [
            AndroidView(
              viewType: _viewType,
              creationParams: {
                "width": widget.width ?? MediaQuery.of(context).size.width,
                "height": widget.height ?? MediaQuery.of(context).size.height,
              },
              onPlatformViewCreated: _registerChannel,
              creationParamsCodec: const StandardMessageCodec(),
            ),
            Align(
              alignment: Alignment.bottomCenter,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  TextButton(
                    onPressed: () {
                      startStreaming();
                    },
                    child: const Text('Bắt đầu'),
                  ),
                  TextButton(
                    onPressed: () {
                      stopStreaming();
                    },
                    child: const Text('kết thức'),
                  ),
                  TextButton(
                    onPressed: () {
                      switchCamera();
                    },
                    child: const Text('Đổi camera'),
                  ),
                  TextButton(
                    onPressed: () {
                      onOffAudio();
                    },
                    child: const Text('Tắt/Bật mic'),
                  ),
                ],
              ),
            )
          ],
        ),
      );
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return const SizedBox();
    } else {
      return Container();
    }
  }

  //监听原生view传值
  Future<dynamic> _platformCallHandler(MethodCall call) async {
    print('=======method: ${call.method}');

    switch (call.method) {
      case "onStreamInfoList":
        Map map = call.arguments;
        // widget.cameraCallBack?.recordPhoto!(map["path"]);
        break;
      case "onBufferedAmountChange":
        Map map = call.arguments;
        // widget.cameraCallBack?.recordVideo!(map["recordStatus"],map["path"]);
        break;
      case "onStateChange":
        Map map = call.arguments;
        // widget.cameraCallBack?.recordVideo!(map["recordStatus"],map["path"]);
        break;
    }
  }
}
