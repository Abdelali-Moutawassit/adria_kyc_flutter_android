import 'package:flutter_test/flutter_test.dart';
import 'package:adria_kyc_integration/adria_kyc_integration.dart';
import 'package:adria_kyc_integration/adria_kyc_integration_platform_interface.dart';
import 'package:adria_kyc_integration/adria_kyc_integration_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockAdriaKycIntegrationPlatform
    with MockPlatformInterfaceMixin
    implements AdriaKycIntegrationPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final AdriaKycIntegrationPlatform initialPlatform = AdriaKycIntegrationPlatform.instance;

  test('$MethodChannelAdriaKycIntegration is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelAdriaKycIntegration>());
  });

  test('getPlatformVersion', () async {
    AdriaKycIntegration adriaKycIntegrationPlugin = AdriaKycIntegration();
    MockAdriaKycIntegrationPlatform fakePlatform = MockAdriaKycIntegrationPlatform();
    AdriaKycIntegrationPlatform.instance = fakePlatform;

    expect(await adriaKycIntegrationPlugin.getPlatformVersion(), '42');
  });
}
