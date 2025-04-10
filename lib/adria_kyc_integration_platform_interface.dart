import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'adria_kyc_integration_method_channel.dart';

abstract class AdriaKycIntegrationPlatform extends PlatformInterface {
  /// Constructs a AdriaKycIntegrationPlatform.
  AdriaKycIntegrationPlatform() : super(token: _token);

  static final Object _token = Object();

  static AdriaKycIntegrationPlatform _instance = MethodChannelAdriaKycIntegration();

  /// The default instance of [AdriaKycIntegrationPlatform] to use.
  ///
  /// Defaults to [MethodChannelAdriaKycIntegration].
  static AdriaKycIntegrationPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [AdriaKycIntegrationPlatform] when
  /// they register themselves.
  static set instance(AdriaKycIntegrationPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
