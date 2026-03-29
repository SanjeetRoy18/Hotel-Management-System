import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL resource = getClass().getResource("/Main.fxml");
        if (resource == null) {
            throw new IllegalStateException("Cannot find Main.fxml in resources");
        }

        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1000, 600);
        URL stylesheet = getClass().getResource("/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Hotel Management System");
        stage.setScene(scene);

        Object controller = loader.getController();
        if (controller instanceof MainController) {
            stage.setOnCloseRequest(e -> ((MainController) controller).saveData());
        }

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}