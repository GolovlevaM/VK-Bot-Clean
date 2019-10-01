package ru.vanishBot;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.objects.wall.WallPostFull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Controller.class);

    private VkApiClient vk;
    private UserActor actor;

    public Utils(VkApiClient vk, UserActor actor) {
        this.vk = vk;
        this.actor = actor;
    }

    public void cleaningWall(){


    }

    public void cleaningPhotos(){

    }

    public void goingOutGroups(){

    }

    public void cleaningMessages(int number){

    }
}
