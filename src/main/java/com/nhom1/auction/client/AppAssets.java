package com.nhom1.auction.client;

import javafx.scene.text.Font;

public class AppAssets {

    public static Font EBGaramondRegular;
    public static Font EBGaramondItalic;

    public static void loadFonts(){

        try {
            EBGaramondRegular = Font.loadFont(
                AppAssets.class.getResourceAsStream("/assets/fonts/EBGaramond-Regular.ttf"), 14);

            EBGaramondItalic = Font.loadFont(
                AppAssets.class.getResourceAsStream("/assets/fonts/EBGaramond-Italic.ttf"), 14 );
        }
        catch (Exception e) {
        }
    }
}