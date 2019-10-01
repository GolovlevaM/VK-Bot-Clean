package ru.vanishBot;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.messages.Dialog;
import com.vk.api.sdk.objects.messages.Message;
import com.vk.api.sdk.objects.photos.Photo;
import com.vk.api.sdk.objects.photos.PhotoAlbum;
import com.vk.api.sdk.objects.photos.PhotoAlbumFull;
import com.vk.api.sdk.objects.wall.WallPostFull;

import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CleanManager implements Runnable{
    private boolean interrupt = false;
    private VkApiClient vk;
    private UserActor user;
    private Boolean cleanWall;
    private Boolean cleanPhoto;
    private Boolean goOutGroups;
    private Boolean cleanMessages;
    private int number;
    private Utils utils;

    private static final Logger logger = LoggerFactory.getLogger(CleanManager.class);

    public CleanManager(VkApiClient vk, UserActor user, Utils utils, boolean cleanWall, boolean cleanPhoto,
                        boolean goOutGroups, boolean cleanMessages, int number) {
        this.vk = vk;
        this.user = user;
        this.cleanWall = cleanWall;
        this.cleanPhoto = cleanPhoto;
        this.goOutGroups = goOutGroups;
        this.cleanMessages = cleanMessages;
        this.number = number;
        this.utils = utils;
    }

    @Override
    public void run() {
        logger.info("Cleaning is starting");
        try {
            if (cleanWall) {
                logger.info("Cleaning the wall");
                    List<WallPostFull> idWallPostsForDelete = new ArrayList<>();
                     for (int i=0; i<10; i++) {
                         logger.info("Getting wall's posts from "+ (100*i) + " to " + (100*i+100));
                         List<WallPostFull> listPosts = vk.wall().get(user)
                                 .offset(100*i)
                                 .count(100)
                                 .execute()
                                 .getItems();

                         idWallPostsForDelete.addAll(listPosts);
                         Thread.sleep(350);
                     }

                    for (WallPostFull post : idWallPostsForDelete) {
                         logger.info("delete " + post.getId());
                        vk.wall().delete(user).postId(post.getId()).execute();
                        Thread.sleep(350);
                    }

                    logger.info("The wall was cleaned");

            }
            if (cleanPhoto) {
                    logger.info("Getting photos from profile");
                    List<Photo> foto = vk.photos().get(user).albumId("profile").count(1000).execute().getItems();
                    Thread.sleep(350);
                    logger.info("Getting photo from wall");
                    foto.addAll(vk.photos().get(user).albumId("wall").count(1000).execute().getItems());
                    logger.info("Getting photo from saved");
                    Thread.sleep(350);
                    foto.addAll(vk.photos().get(user).albumId("saved").count(1000).execute().getItems());
                    Thread.sleep(350);
                    List<PhotoAlbumFull> albums = vk.photos().getAlbums(user).execute().getItems();
                    logger.info("Getting albums");
                    Thread.sleep(350);

                    for (Photo photo : foto) {
                        logger.info("Delete photo" + photo.getId());
                        vk.photos().delete(user, photo.getId()).execute();
                        Thread.sleep(350);
                    }

                    for(PhotoAlbumFull album : albums){
                        logger.info("Delete album" + album.getId());
                        vk.photos().deleteAlbum(user, album.getId()).execute();
                        Thread.sleep(350);
                    }

                    logger.info("Photos was deleting");

            }
            if (goOutGroups) {

                    logger.info("Getting ID Groups");
                    List<Integer> listGroups = vk.groups().get(user).count(200).execute().getItems();
                    listGroups.addAll(vk.groups().get(user).offset(200).count(200).execute().getItems());
                    Thread.sleep(500);
                    for (Integer groupId : listGroups) {
                        logger.info("leave" + groupId);
                        vk.groups().leave(user, groupId).execute();
                        Thread.sleep(350);
                    }

                    logger.info("Groups left");

            }
            if (cleanMessages) {

            }
        }catch (InterruptedException e) {
            logger.info("Observer thread interrupted", e);
            interrupt = true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("CleaningManager end");


    }


}
