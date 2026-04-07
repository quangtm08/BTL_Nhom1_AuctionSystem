package com.nhom1.auction.server;

import com.nhom1.auction.common.classes.BaseMainController;
import com.nhom1.auction.server.Controller.MainController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerApplication extends Application{

    private static final double DESIGN_WIDTH = 1320;
    private static final double DESIGN_HEIGHT = 855;
    private static final double MIN_WIDTH = 1080;
    private static final double MIN_HEIGHT = 700;

    private static Stage stage;
    private static FXMLLoader fxmlLoader;

    @Override
    public void start(Stage stage) throws Exception {
        ServerApplication.stage = stage;

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/views/main.fxml")
        );

        AppAssets.loadFonts();

        Scene scene = new Scene(loader.load(), DESIGN_WIDTH, DESIGN_HEIGHT);

        BaseMainController controller = loader.getController();
        AppNavigator.setRoot(controller);


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
    

}
