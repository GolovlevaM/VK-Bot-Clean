package ru.vanishBot;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.users.UserXtrCounters;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Controller {

    public TextField idApp;
    public TextField serviceKey;
    private String path = System.getProperty("user.home") + "\\vanish\\property.properties";
    private ExecutorService service = Executors.newSingleThreadExecutor();
    private Future<?> currentTask;
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);


    public Button getTokenButton;
    public Button savePropertyButton;
    public TextField vkId;
    public TextField token;
    public Button startButton;
    public Button stopButton;
    public RadioButton cleanWall;
    public RadioButton cleanPhoto;
    public RadioButton goOutGroups;
    public RadioButton cleanMessages;
    public TextField countMessagesForSave;

    public int number;

    private VkApiClient vk;
    private UserActor user;

    @FXML
    private void initialize() {
        TransportClient transportClient = HttpTransportClient.getInstance();
        vk = new VkApiClient(transportClient);
        stopButton.setDisable(true);

        try {
            logger.info("try initialize start properties from file: " + path);
            File file = new File(path);
            if (file.exists()) {
                Properties properties = new Properties();
                properties.load(Files.newInputStream(file.toPath()));
                logger.info("load vkId properties from file: " + path);
                vkId.setText(properties.getProperty("vkId"));

                logger.info("load token properties from file: " + path);
                token.setText(properties.getProperty("token"));

                logger.info("load idApp properties from file: " + path);
                idApp.setText(properties.getProperty("idApp"));

                logger.info("load serviceKey properties from file: " + path);
                serviceKey.setText(properties.getProperty("serviceKey"));

                logger.info("load all properties from file: " + path);
            }
            else
                logger.info("File: " + path + " not exists");
        }catch (Exception e){
            logger.error("Fail load start properties", e);
        }
    }

    public void getTokenAction(ActionEvent actionEvent) {
        try {
            int idApp = Integer.parseInt(this.idApp.getText());
            URI url = new URI("https://oauth.vk.com/authorize?client_id=" + idApp + "&display=page&redirect_uri=https://oauth.vk.com/blank.html&scope=photos,wall,groups,messages,offline&response_type=token&v=5.68&state=123456");
            Desktop.getDesktop().browse(url);
        }catch (NumberFormatException e){
            logger.error(e.getMessage(), e);
            createAlert("Error in appId. AppId must be numbers.");
        }catch (Exception e){
            logger.error("Fail open token url", e);
            createAlert("can't open url");
        }
    }

    public void savePropertyAction(ActionEvent actionEvent) {
        Properties property = new Properties();
        property.put("vkId", vkId.getText());
        property.put("token", token.getText());
        property.put("idApp", idApp.getText());
        property.put("serviceKey", serviceKey.getText());

        logger.info("try save properties to file: " + path);
        try{
            File file = new File(path);
            if (!file.exists()) {
                logger.info("File: " + path + "not exists, try create file");
                Files.createDirectories(file.getParentFile().toPath());
                file.createNewFile();
            }

            logger.info("start save properties");
            try(FileOutputStream out = new FileOutputStream(path)) {
                property.store(out, "");
                logger.info("property saved");
            }

        }catch (Exception e){
            logger.error("fail when try save property to file: " + path, e);
        }
    }

    public void startAction(ActionEvent actionEvent) {
        logger.info("Start button click");

        int id = parseId(vkId.getText());
        if (id == -1) {
            createAlert("vk id must be \"https://vk.com/(id or nickName)\"");
            return;
        } else if (id == -2){
            createAlert("app id must be numbers");
            return;
        }

        String tokenFromInputString = getTokenFromInputString(token.getText());
        if (tokenFromInputString==null || !validateToken(id, tokenFromInputString)) {
            createAlert("No valid token");
            return;
        }

        if(!countMessagesForSave.getText().isEmpty()){
            try {
                number = Integer.parseInt(countMessagesForSave.getText());
            }catch (NumberFormatException e){
                createAlert("It's must be a number");
                return;
            }
        }

        startButton.setDisable(true);
        Utils utils = new Utils(vk, user);
        CleanManager cleanManager = new CleanManager(vk, user, utils, cleanWall.isSelected(), cleanPhoto.isSelected(),
                goOutGroups.isSelected(), cleanMessages.isSelected(), number);
        currentTask = service.submit(cleanManager);

        while (true){
            if(currentTask.isDone()){
                informAlert();
                break;
            }
        }

        stopButton.setDisable(false);
    }


    public void stopAction(ActionEvent actionEvent) {
        stopButton.setDisable(true);
        logger.info("click stop button. Try canceled task");
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        startButton.setDisable(false);

    }

    private String getTokenFromInputString(String url){
        logger.info("try get token from string: " + url);
        int start = url.indexOf("access_token=");
        int end = url.indexOf("&expires_in=");
        if (start == -1 || end == -1)
            return null;
        return url.substring(start, end).replaceAll("access_token=", "");
    }

    private boolean validateToken(int id, String token){
        logger.info("try validate token: " + token + " and userId: " + id);

        user = new UserActor(id, token);
        try {
            GetResponse execute = vk.wall().get(user).ownerId(-86529522).count(1).execute();
            logger.info("token fine");
            return true;
        } catch (ApiException | ClientException e) {
            logger.info("Error in token", e);
            return false;
        }
    }

    private int parseId(String vkLink){
        if (serviceKey.getText() == null || serviceKey.getText().isEmpty()) {
            createAlert("Service key should not be empty!");
            return -1;
        }

        logger.info("try parse userId from: " + vkLink + " with service token = " + serviceKey.getText());
        if (!vkLink.startsWith("https://vk.com/"))
            return -1;

        String name = vkLink.replaceAll("https://vk.com/","");


        try {
            ServiceActor service = new ServiceActor(Integer.parseInt(idApp.getText()), serviceKey.getText());
            List<UserXtrCounters> usersId = vk.users().get(service).userIds(name).execute();
            logger.info("get userId = " + usersId.get(0).getId());
            return usersId.get(0).getId();
        }catch (NumberFormatException e){
            logger.info("Error in idApp", e);
            return -2;
        } catch (Exception e) {
            logger.info("Error in userId", e);
            return -1;
        }
    }

    public void setOnCloseOperation(Stage currentStage) {
        logger.info("set on close operation");
        currentStage.setOnCloseRequest( (value) -> {
            try {
                if (currentTask != null)
                    currentTask.cancel(true);
                service.shutdown();
                service.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Fail exit app, invoke System.exit(1)");
                System.exit(1);
            }
        });
    }
    private void createAlert(String info){
        logger.info("add error alert with text: " +info);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Some fields error");
        alert.setContentText(info);

        alert.showAndWait();
    }

    private void informAlert(){
        logger.info("alert with sucsses clean");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("YAHOO");
        alert.setHeaderText("Clean finish");

        alert.showAndWait();
    }
}
