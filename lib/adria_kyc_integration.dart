
import 'adria_kyc_integration_platform_interface.dart';

class AdriaKycIntegration {
  Future<String?> getPlatformVersion() {
    return AdriaKycIntegrationPlatform.instance.getPlatformVersion();
  }
}
