import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:adria_kyc_integration/adria_kyc_integration_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelAdriaKycIntegration platform = MethodChannelAdriaKycIntegration();
  const MethodChannel channel = MethodChannel('adria_kyc_integration');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
