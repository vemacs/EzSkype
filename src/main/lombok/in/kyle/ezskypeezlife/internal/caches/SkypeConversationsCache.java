package in.kyle.ezskypeezlife.internal.caches;

import in.kyle.ezskypeezlife.EzSkype;
import in.kyle.ezskypeezlife.api.SkypeConversationType;
import in.kyle.ezskypeezlife.internal.obj.SkypeConversationInternal;
import in.kyle.ezskypeezlife.internal.obj.SkypeGroupConversationInternal;
import in.kyle.ezskypeezlife.internal.obj.SkypeUserConversationInternal;
import in.kyle.ezskypeezlife.internal.packet.conversation.SkypeGetGroupConversationPacket;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kyle on 10/7/2015.
 */
public class SkypeConversationsCache {
    
    @Getter
    private final Map<String, SkypeConversationInternal> skypeConversations;
    private final EzSkype ezSkype;
    
    public SkypeConversationsCache(EzSkype ezSkype) {
        this.ezSkype = ezSkype;
        this.skypeConversations = new HashMap<>();
    }
    
    /**
     * Get a conversation from the cache or get it from the server
     *
     * @param longId - The longId of the user
     * @return - The Skype user
     */
    public SkypeConversationInternal getSkypeConversation(String longId) {
        if (skypeConversations.containsKey(longId)) {
            return skypeConversations.get(longId);
        } else {
            SkypeConversationType conversationType = SkypeConversationType.fromLongId(longId);
            if (conversationType == SkypeConversationType.USER) {
                return new SkypeUserConversationInternal(ezSkype, longId, longId, true, false, "");
            } else {
                SkypeGetGroupConversationPacket skypeGetGroupConversationPacket = new SkypeGetGroupConversationPacket(ezSkype, longId);
    
                try {
                    SkypeConversationInternal skypeConversationInternal = (SkypeConversationInternal) skypeGetGroupConversationPacket
                            .executeSync();
                    if (!skypeConversationInternal.isFullyLoaded()) {
                        skypeConversationInternal.fullyLoad();
                    }
                    skypeConversations.put(skypeConversationInternal.getLongId(), skypeConversationInternal);
                    return skypeConversationInternal;
                } catch (Exception e) {
                    e.printStackTrace();
                    return new SkypeGroupConversationInternal(ezSkype, longId, "", false, Collections.emptyList(), false, null, "", 
                            Collections.emptyList(), Collections.emptyList());
                }
            }
        }
    }
}
