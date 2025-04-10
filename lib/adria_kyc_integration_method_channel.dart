import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'adria_kyc_integration_platform_interface.dart';

/// An implementation of [AdriaKycIntegrationPlatform] that uses method channels.
class MethodChannelAdriaKycIntegration extends AdriaKycIntegrationPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('adria_kyc_integration');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
