package com.nhom1.auction.client;

import com.nhom1.auction.client.Controller.MainController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class ClientApplication extends Application {

    private static final double DESIGN_WIDTH = 1320;
    private static final double DESIGN_HEIGHT = 855;
    private static final double MIN_WIDTH = 1080;
    private static final double MIN_HEIGHT = 700;

    private static Stage stage;
    private static FXMLLoader fxmlLoader;
    private static String clientIp;
    private static String ip;
    private static int port;

    @Override
    public void start(Stage stage) throws Exception {
        ClientApplication.stage = stage;

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/views/main.fxml")
        );

        AppAssets.loadFonts();

        Scene scene = new Scene(loader.load(), DESIGN_WIDTH, DESIGN_HEIGHT);

        MainController mainController = loader.getController();

        AppNavigator.setRoot(mainController);

        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setWidth(DESIGN_WIDTH);
        stage.setHeight(DESIGN_HEIGHT);
        stage.centerOnScreen();
        stage.show();

        AppNavigator.navigateTo(AppView.SIGN_IN);
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static Stage getStage(){
        return stage;
    }

    public static String getClientIp() {
        return clientIp;
    }

    public static FXMLLoader getFxmlLoader() {
        return fxmlLoader;
    }

    public static String getIp() {
        return ip;
    }

    public static int getPort() {
        return port;
    }

    public static void setStage(Stage stage) {
        ClientApplication.stage = stage;
    }

    public static void setClientIp(String clientIp) {
        ClientApplication.clientIp = clientIp;
    }

    public static void setIp(String ip) {
        ClientApplication.ip = ip;
    }

    public static void setFxmlLoader(FXMLLoader fxmlLoader) {
        ClientApplication.fxmlLoader = fxmlLoader;
    }

    public static void setPort(int port) {
        ClientApplication.port = port;
    }

}
