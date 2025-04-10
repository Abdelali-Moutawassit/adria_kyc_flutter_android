import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:adria_kyc_integration/adria_kyc_integration.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _scanResult = '';
  final _adriaKycIntegrationPlugin = AdriaKycIntegration();

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  Future<void> initPlatformState() async {
    String platformVersion;
    try {
      platformVersion =
          await _adriaKycIntegrationPlugin.getPlatformVersion() ?? 'Unknown platform version';
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  Future<void> _scanOldCin() async {
    final result = await _adriaKycIntegrationPlugin.scanOldCin();
    setState(() {
      _scanResult = result != null ? jsonEncode(result) : 'No data';
    });
  }

  Future<void> _scanNewCin() async {
    final result = await _adriaKycIntegrationPlugin.scanNewCin();
    setState(() {
      _scanResult = result != null ? jsonEncode(result) : 'No data';
    });
  }

  Future<void> _faceRecognition() async {
    final result = await _adriaKycIntegrationPlugin.faceRecognition();
    setState(() {
      _scanResult = result ?? 'No data';
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('KYC Plugin Example'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            children: [
              Text('Platform Version: $_platformVersion'),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: _scanOldCin,
                child: const Text('Scan Old CIN'),
              ),
              const SizedBox(height: 10),
              ElevatedButton(
                onPressed: _scanNewCin,
                child: const Text('Scan New CIN'),
              ),
              const SizedBox(height: 10),
              ElevatedButton(
                onPressed: _faceRecognition,
                child: const Text('Face Recognition'),
              ),
              const SizedBox(height: 30),
              const Text('Scan Result:'),
              const SizedBox(height: 10),
              Expanded(
                child: SingleChildScrollView(
                  child: Text(_scanResult),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
