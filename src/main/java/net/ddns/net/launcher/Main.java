package net.ddns.net.launcher;

import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class Main extends Application {

    private static final File APPLICATION_FOLDER = new File(System.getProperty("user.home") + "/AppData/Local/Swooosh/CampusLive"); //change here
    private static final File VERSION_TEXT_FILE = new File(APPLICATION_FOLDER.getAbsolutePath() + "/version.txt");
    private static final File APPLICATION_FILE = new File(APPLICATION_FOLDER.getAbsolutePath() + "/CampusLiveServer.jar"); //change here
    private static final File KEY_STORE_FILE = new File(APPLICATION_FOLDER.getAbsolutePath() + "/campuslive.store");
    private volatile BooleanProperty downloadRunning = new SimpleBooleanProperty(false);

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (!APPLICATION_FOLDER.exists()) {
            APPLICATION_FOLDER.mkdirs();
        }
        int localVersion = getLocalVersion();
        int onlineVersion = getOnlineVersion();
        if (localVersion != onlineVersion || localVersion == 0 || !APPLICATION_FILE.exists() || !KEY_STORE_FILE.exists()) {
            primaryStage.setResizable(false);
            primaryStage.setTitle("Updater");
            primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("CLLogo.png")));
            Label label = new Label("Please be patient while downloading...");
            label.setStyle("-fx-font-size: 16;");
            ProgressIndicator progressIndicator = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressIndicator.setMinSize(150, 150);
            VBox vBox = new VBox(label, progressIndicator);
            vBox.setSpacing(20);
            vBox.setPadding(new Insets(10));
            vBox.setAlignment(Pos.CENTER);
            Scene scene = new Scene(vBox, 300, 300);
            primaryStage.setScene(scene);
            primaryStage.show();
            downloadRunning.set(true);
            new Downloader().start();
            downloadRunning.addListener(e -> {
                setLocalVersion(onlineVersion);
                launch();
                System.exit(0);
            });
        } else {
            launch();
            System.exit(0);
        }
    }

    public class Downloader extends Thread {
        @Override
        public void run() {
            if (downloadApplication() && downloadKeyStore()) {
                downloadRunning.set(false);
            } else {
                displayErrorMessage("Download failed", "Failed to download application!\nPlease try again1");
                System.exit(0);
            }
        }
    }

    private void displayErrorMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private int getOnlineVersion() {
        System.out.println("Getting online version...");
        try {
            URL url = new URL("http://swooosh.ddns.net:8080/job/CampusLiveServer/lastSuccessfulBuild/buildNumber"); //change here
            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String input;
            while((input = bufferedReader.readLine()) != null) {
                System.out.println("Online version: " + input);
                return Integer.parseInt(input);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("Online version: 0");
        return 0;
    }

    private int getLocalVersion() {
        System.out.println("Getting local version...");
        try {
            Scanner scanner = new Scanner(VERSION_TEXT_FILE);
            if (scanner.hasNextLine()) {
                String version = scanner.nextLine();
                System.out.println("Local version: " + version);
                return Integer.parseInt(version);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("Local version: 0");
        return 0;
    }

    private void setLocalVersion(int version) {
        System.out.println("Setting local version...");
        try {
            PrintWriter printWriter = new PrintWriter(VERSION_TEXT_FILE);
            printWriter.write(version + "");
            printWriter.flush();
            printWriter.close();
            System.out.println("Local version set to " + version);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Boolean downloadApplication() {
        System.out.println("Trying to download application");
        try {
            URL url = new URL("http://swooosh.ddns.net:8080/job/CampusLiveServer/lastSuccessfulBuild/artifact/target/CampusLiveServer.jar"); //change here
            InputStream inputStream = url.openStream();
            Files.copy(inputStream, APPLICATION_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Successfully downloaded application");
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private Boolean downloadKeyStore() {
        System.out.println("Trying to download keystore");
        try {
            if (!KEY_STORE_FILE.exists()) {
                URL url = new URL("http://swooosh.ddns.net:8080/job/CampusLiveStudent/27/artifact/target/campuslive.store");
                InputStream inputStream = url.openStream();
                Files.copy(inputStream, KEY_STORE_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Successfully downloaded keystore");
            } else {
                System.out.println("Keystore already downloaded");
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void launch() {
        System.out.println("Trying to launch application");
        try {
            Desktop.getDesktop().open(APPLICATION_FILE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(null);
    }

}
