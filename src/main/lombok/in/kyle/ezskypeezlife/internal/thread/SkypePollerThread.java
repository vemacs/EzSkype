package in.kyle.ezskypeezlife.internal.thread;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import in.kyle.ezskypeezlife.EzSkype;
import in.kyle.ezskypeezlife.internal.packet.pull.SkypePullPacket;
import in.kyle.ezskypeezlife.internal.thread.poll.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Kyle on 10/5/2015.
 * <p>
 * Pulls new Skype events from the server
 */
public class SkypePollerThread extends Thread {
    
    private EzSkype ezSkype;
    private AtomicBoolean active;
    private List<SkypePollMessageType> messageTypes;
    
    public SkypePollerThread(EzSkype ezSkype) {
        super("Skype-Poller-Thread-" + ezSkype.getLocalUser().getUsername());
        this.ezSkype = ezSkype;
        this.active = ezSkype.getActive();
        
        // @formatter:off
        this.messageTypes = Arrays.asList(
                new SkypeControlClearTypingType(),
                new SkypeControlTypingType(),
                new SkypeEventCallType(),
                new SkypeTextType(),
                new SkypeThreadActivityAddMemberType(),
                new SkypeThreadActivityDeleteMemberType(),
                new SkypeThreadActivityPictureUpdateType(),
                new SkypeThreadActivityRoleUpdateType(),
                new SkypeThreadActivityTopicUpdate()
        );
        // @formatter:on
    }
    
    @Override
    public void run() {
        try {
            while (active.get()) {
                SkypePullPacket skypePullPacket = new SkypePullPacket(ezSkype);
                JsonObject responseData = (JsonObject) skypePullPacket.executeSync();
                EzSkype.LOGGER.debug(responseData.toString());
                if (responseData.entrySet().size() != 0) {
                    if (responseData.has("eventMessages")) {
                        JsonArray messages = responseData.getAsJsonArray("eventMessages");
                        
                        for (JsonElement jsonObject : messages) {
                            try {
                                extractInfo(jsonObject.getAsJsonObject());
                            } catch (Exception e) {
                                EzSkype.LOGGER.error("Error extracting info from:\n" + jsonObject, e);
                            }
                        }
                    } else {
                        EzSkype.LOGGER.error("Bad poll response: " + responseData);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void extractInfo(JsonObject jsonObject) throws Exception {
        JsonObject resource = jsonObject.getAsJsonObject().getAsJsonObject("resource");
    
        if (resource.has("messagetype")) {
            String messageType = resource.get("messagetype").getAsString();
            
            for (SkypePollMessageType skypePollMessageType : messageTypes) {
                if (skypePollMessageType.accept(messageType)) {
                    skypePollMessageType.extract(ezSkype, jsonObject, resource);
                    return;
                }
            }
        }
        
        
        return;
        
        /*
        // TODO remove
        String resourceType = resource.get("resourceType").getAsString();
        if (resourceType.equals("ConversationUpdate") || resourceType.equals("UserPresence") || resourceType.equals("EndpointPresence")) {
            return;
        }
        
        System.err.println("Invalid message: " + jsonObject);
         */
    }
}
