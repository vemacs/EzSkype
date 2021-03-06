package in.kyle.ezskypeezlife.internal.caches;

import in.kyle.ezskypeezlife.EzSkype;
import in.kyle.ezskypeezlife.api.obj.SkypeUser;
import in.kyle.ezskypeezlife.internal.obj.SkypeUserInternal;
import in.kyle.ezskypeezlife.internal.packet.user.SkypeGetUserInfoPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Kyle on 10/7/2015.
 */
public class SkypeUsersCache {
    
    private final Map<String, SkypeUserInternal> skypeUsers;
    private final EzSkype ezSkype;
    
    
    public SkypeUsersCache(EzSkype ezSkype) {
        this.ezSkype = ezSkype;
        this.skypeUsers = new HashMap<>();
    }
    
    /**
     * Get a user from the cache or get it from the server
     *
     * @param username - The username of the user
     * @return - The Skype user
     */
    public SkypeUserInternal getOrCreateUserLoaded(String username) {
        if (username.startsWith("8:")) {
            username = username.substring(2);
        }
    
        if (skypeUsers.containsKey(username)) {
            return skypeUsers.get(username);
        } else {
            SkypeGetUserInfoPacket getUserInfoPacket = new SkypeGetUserInfoPacket(ezSkype, username);
            try {
                List<SkypeUserInternal> skypeUserInternalList = (List<SkypeUserInternal>) getUserInfoPacket.executeSync();
                EzSkype.LOGGER.debug("Getting user {} user list {}", username, skypeUserInternalList);
                SkypeUserInternal skypeUserInternal = skypeUserInternalList.get(0);
                this.skypeUsers.put(skypeUserInternal.getUsername(), skypeUserInternal);
                return skypeUserInternal;
            } catch (Exception e) {
                e.printStackTrace();
                return new SkypeUserInternal(username, ezSkype);
            }
        }
    }
    
    /**
     * Creates a new Skype user that is unloaded
     *
     * @param username - The username to get
     * @return - The unloaded Skype user
     */
    public SkypeUserInternal getOrCreateUserUnloaded(String username) {
        if (username.startsWith("8:")) {
            username = username.substring(2);
        }
        final String finalUsername = username;
        if (skypeUsers.containsKey(username)) {
            return skypeUsers.get(username);
        } else {
            return new SkypeUserInternal(username, ezSkype);
        }
    }
    
    /**
     * Fully load a user if not already fully loaded
     *
     * @param skypeUser - The user to load
     * @return - The loaded Skype user
     */
    public SkypeUser fullyLoadUser(SkypeUserInternal skypeUser) { // TODO fix
        SkypeGetUserInfoPacket getUserInfoPacket = new SkypeGetUserInfoPacket(ezSkype, skypeUser.getUsername());
        SkypeUserInternal skypeUserNew;
        try {
            skypeUserNew = ((List<SkypeUserInternal>) getUserInfoPacket.executeSync()).get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return skypeUser;
        }
    
        skypeUser.setFirstName(skypeUserNew.getFirstName());
        skypeUser.setLastName(skypeUserNew.getLastName());
        skypeUser.setMood(skypeUserNew.getMood());
        skypeUser.setRichMood(skypeUserNew.getRichMood());
        skypeUser.setDisplayName(skypeUserNew.getDisplayName());
        skypeUser.setCountry(skypeUserNew.getCountry());
        skypeUser.setCity(skypeUserNew.getCity());
        skypeUser.setAvatarUrl(skypeUserNew.getAvatarUrl());
        skypeUser.setLoaded(true);
        return skypeUser;
    }
    
    public Map<String, SkypeUserInternal> getSkypeUsers() {
        return skypeUsers;
    }
}
