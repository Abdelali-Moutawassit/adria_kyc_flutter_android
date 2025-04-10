import 'dart:convert'; // <== Obligatoire pour json.decode
import 'package:flutter/services.dart';

class AdriaKycIntegration {
  static const MethodChannel _channel = MethodChannel('com.adria.adriascansin/kyc_sdk');

  // Récupérer la version de la plateforme
  Future<String?> getPlatformVersion() async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  // Scanner ancienne CIN
  Future<Map<String, dynamic>?> scanOldCin() async {
    try {
      final String result = await _channel.invokeMethod('scanCIN');
      return result.isNotEmpty ? Map<String, dynamic>.from(json.decode(result)) : null;
    } on PlatformException catch (e) {
      print("Erreur scanCIN: ${e.message}");
      return null;
    }
  }

  // Scanner nouvelle CIN
  Future<Map<String, dynamic>?> scanNewCin() async {
    try {
      final String result = await _channel.invokeMethod('scanNewCIN');
      return result.isNotEmpty ? Map<String, dynamic>.from(json.decode(result)) : null;
    } on PlatformException catch (e) {
      print("Erreur scanNewCIN: ${e.message}");
      return null;
    }
  }

  // Reconnaissance faciale
  Future<String?> faceRecognition() async {
    try {
      final String result = await _channel.invokeMethod('faceRecognition');
      return result;
    } on PlatformException catch (e) {
      print("Erreur faceRecognition: ${e.message}");
      return null;
    }
  }
}
