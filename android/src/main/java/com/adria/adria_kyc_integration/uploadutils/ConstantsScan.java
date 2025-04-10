package com.adria.adria_kyc_integration.uploadutils;

import android.graphics.Bitmap;

public class ConstantsScan {

    public static int PACK = 0;
    public static boolean launch = true;
    public static String nom ="";
    public static String prenom = "";
    public static String cin="";
    public static String ville="";
    public static String sexe="";
    public static String adresse ="";
    public static String dateNaissance = "";
    public static String DateExpiration = "";
    public static String Telephone="";
    public static String Email="";
    public static String IdSouscription="";
    public static Bitmap signature =null;
    public static int typePiece =0;
    public static String imagePathRecto;
    public static String imagePathVerso;
    public static String zipHolo;


    public static void resetData() {
        ConstantsScan.nom ="";
        ConstantsScan.prenom = "";
        ConstantsScan.cin="";
        ConstantsScan.ville="";
        ConstantsScan.sexe="";
        ConstantsScan.adresse ="";
        ConstantsScan.dateNaissance = "";
        ConstantsScan.DateExpiration = "";
        //ConstantsScan.Telephone = "";
        ConstantsScan.Email = "";
        ConstantsScan.imagePathRecto = "";
        ConstantsScan.imagePathVerso = "";
        ConstantsScan.signature =null;
        ConstantsScan.zipHolo ="";
    }
}
