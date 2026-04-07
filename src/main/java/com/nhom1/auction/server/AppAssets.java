package com.nhom1.auction.server;

import javafx.scene.text.Font;

public class AppAssets {

    public static Font EBGaramondRegular;
    public static Font EBGaramondItalic;
    public static Font InterRegular;
    public static Font InterItalic;

    public static void loadFonts(){

        try {
            EBGaramondRegular = Font.loadFont(
                AppAssets.class.getResourceAsStream("/assets/fonts/EBGaramond-Regular.ttf"), 14);

            EBGaramondItalic = Font.loadFont(
                AppAssets.class.getResourceAsStream("/assets/fonts/EBGaramond-Italic.ttf"), 14 );

            InterRegular = Font.loadFont(
                AppAssets.class.getResourceAsStream("/assets/fonts/Inter-Regular.ttf"), 14);

            InterItalic = Font.loadFont(
                AppAssets.class.getResourceAsStream("/assets/fonts/Inter-Italic.ttf"), 14);

        }
        catch (Exception e) {
        }
    }
}