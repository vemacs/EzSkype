package in.kyle.ezskypeezlife;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import in.kyle.ezskypeezlife.api.SkypeCredentials;
import in.kyle.ezskypeezlife.api.SkypeStatus;
import in.kyle.ezskypeezlife.api.obj.SkypeConversation;
import in.kyle.ezskypeezlife.api.obj.SkypeUser;
import in.kyle.ezskypeezlife.events.EventManager;
import in.kyle.ezskypeezlife.internal.caches.SkypeCacheManager;
import in.kyle.ezskypeezlife.internal.obj.SkypeLocalUserInternal;
import in.kyle.ezskypeezlife.internal.obj.SkypeSession;
import in.kyle.ezskypeezlife.internal.packet.auth.SkypeAuthEndpointFinalPacket;
import in.kyle.ezskypeezlife.internal.packet.auth.SkypeAuthFinishPacket;
import in.kyle.ezskypeezlife.internal.packet.auth.SkypeLoginInfoPacket;
import in.kyle.ezskypeezlife.internal.packet.auth.SkypeLoginJavascriptParameters;
import in.kyle.ezskypeezlife.internal.packet.auth.SkypeLoginPacket;
import in.kyle.ezskypeezlife.internal.packet.conversation.SkypeConversationAddPacket;
import in.kyle.ezskypeezlife.internal.packet.conversation.SkypeConversationJoinPacket;
import in.kyle.ezskypeezlife.internal.packet.conversation.SkypeConversationJoinUrlIdPacket;
import in.kyle.ezskypeezlife.internal.packet.pull.SkypeEndpoint;
import in.kyle.ezskypeezlife.internal.packet.pull.SkypeRegisterEndpointsPacket;
import in.kyle.ezskypeezlife.internal.packet.session.SkypeSetVisibilityPacket;
import in.kyle.ezskypeezlife.internal.packet.user.SkypeGetContactsPacket;
import in.kyle.ezskypeezlife.internal.packet.user.SkypeGetSelfInfoPacket;
import in.kyle.ezskypeezlife.internal.thread.SkypeContactsThread;
import in.kyle.ezskypeezlife.internal.thread.SkypePacketIOPool;
import in.kyle.ezskypeezlife.internal.thread.SkypePollerThread;
import in.kyle.ezskypeezlife.internal.thread.SkypeSessionThread;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Kyle on 10/5/2015.
 */
public class EzSkype {
    
    public static final Gson GSON = new Gson();
    public static final Logger LOGGER = LoggerFactory.getLogger(EzSkype.class);
    
    @Getter
    private SkypePacketIOPool packetIOPool;
    @Getter
    private SkypeSession skypeSession;
    @Getter
    private EventManager eventManager;
    @Getter
    private AtomicBoolean active;
    @Getter
    private SkypeCacheManager skypeCache;
    @Getter
    private SkypeLocalUserInternal localUser;
    
    private final SkypeCredentials skypeCredentials;
    private boolean startedThreads;
    
    /**
     * Creates a new Skype instance (Pro constructor)
     *
     * @param skypeCredentials - The Skype login credentials
     */
    public EzSkype(SkypeCredentials skypeCredentials) {
        this.skypeCredentials = skypeCredentials;
        this.active = new AtomicBoolean();
        this.skypeCache = new SkypeCacheManager(this);
        this.eventManager = new EventManager();
        this.packetIOPool = new SkypePacketIOPool(2);
    }
    
    /**
     * Creates a new Skype instance (Noob constructor)
     *
     * @param user - The Skype username
     * @param pass - The Skype password
     */
    public EzSkype(String user, String pass) {
        this(new SkypeCredentials(user, pass.toCharArray()));
    }
    
    public void logout() {
        // TODO add logout packet
        active.set(false);
    }
    
    /**
     * Logs into Skype with specific endpoints
     *
     * @param endpoints - The endpoints Skype should listen for
     * @throws Exception
     */
    public EzSkype login(SkypeEndpoint[] endpoints) throws Exception {
        LOGGER.info("Logging into Skype, user: " + skypeCredentials.getUsername());
        // Get login params
        SkypeLoginInfoPacket loginInfoPacket = new SkypeLoginInfoPacket(this);
        SkypeLoginJavascriptParameters parameters = (SkypeLoginJavascriptParameters) loginInfoPacket.executeSync();
        
        
        // Get x-token
        SkypeLoginPacket skypeLoginPacket = new SkypeLoginPacket(this, skypeCredentials, parameters);
        String xToken = (String) skypeLoginPacket.executeSync();
        
        // Get reg token and location
        SkypeAuthFinishPacket skypeAuthFinishPacket = new SkypeAuthFinishPacket(this, xToken);
        skypeSession = (SkypeSession) skypeAuthFinishPacket.executeSync();
        
        SkypeRegisterEndpointsPacket skypeRegisterEndpointsPacket = new SkypeRegisterEndpointsPacket(this, endpoints);
        skypeRegisterEndpointsPacket.executeSync();
        
        SkypeAuthEndpointFinalPacket finalPacket = new SkypeAuthEndpointFinalPacket(this);
        finalPacket.executeSync();
        
        active.set(true);
        
        EzSkype.LOGGER.info("Loading profile");
        loadLocalProfile();
        EzSkype.LOGGER.info("Loading contacts");
        loadContacts();
        EzSkype.LOGGER.info("Starting pollers");
        startThreads();
        //setOnline();
        return this;
    }
    
    /**
     * Logs into Skype with all endpoints
     *
     * @throws Exception
     */
    public EzSkype login() throws Exception {
        return login(SkypeEndpoint.values());
    }
    
    /**
     * // TODO this needs to be looked at
     * @param skypeStatus - The online status
     * @throws Exception
     */
    public void setStatus(SkypeStatus skypeStatus) {
        EzSkype.LOGGER.info("Setting online");
        SkypeSetVisibilityPacket setVisibilityPacket = new SkypeSetVisibilityPacket(this, skypeStatus);
        setVisibilityPacket.executeAsync();
    }
    
    /**
     * Loads all contacts from the server
     *
     * @throws Exception
     */
    private void loadContacts() throws Exception {
        if (getSkypeCache().getUsersCache().getSkypeUsers().size() == 0) {
            SkypeGetContactsPacket skypeGetContactsPacket = new SkypeGetContactsPacket(this);
            SkypeGetContactsPacket.UserContacts contacts = (SkypeGetContactsPacket.UserContacts) skypeGetContactsPacket.executeSync();
            getSkypeCache().getUsersCache().getSkypeUsers().addAll(contacts.getContacts());
            localUser.getContacts().addAll(contacts.getContacts());
            localUser.getPendingContacts().addAll(contacts.getPending());
        }
    }
    
    /**
     * Loads the users account from the server
     *
     * @throws Exception
     */
    private void loadLocalProfile() throws Exception {
        if (localUser == null) {
            SkypeGetSelfInfoPacket getSelfInfoPacket = new SkypeGetSelfInfoPacket(this);
            localUser = (SkypeLocalUserInternal) getSelfInfoPacket.executeSync();
        }
    }
    
    /**
     * Starts all the async threads
     */
    private void startThreads() {
        if (!startedThreads) {
            SkypePollerThread skypePoller = new SkypePollerThread(this);
            skypePoller.start();
            SkypeSessionThread sessionThread = new SkypeSessionThread(this);
            sessionThread.start();
            SkypeContactsThread contactsThread = new SkypeContactsThread(this);
            contactsThread.start();
            startedThreads = true;
        }
    }
    
    /**
     * Joins a Skype conversation from the URL provided by Skype
     * This should look like https://join.skype.com/zzZzzZZzZZzZ
     * @param skypeConversationUrl - The Skype conversation to join
     * @return - The Skype conversation joined
     * @throws Exception - If the conversation was unable to be joined
     */
    public SkypeConversation joinSkypeConversation(String skypeConversationUrl) throws Exception {
        String encodedId = (String) new SkypeConversationJoinUrlIdPacket(this, skypeConversationUrl).executeSync();
        JsonObject threadData = (JsonObject) new SkypeConversationJoinPacket(this, encodedId).executeSync();
        String longId = threadData.get("ThreadId").getAsString();
        SkypeConversationAddPacket addPacket = new SkypeConversationAddPacket(this, longId, localUser.getUsername());
        addPacket.executeSync();
        return getSkypeConversation(longId);
    }
    
    /**
     * Gets a Skype user from their username
     * Will remove the 8: from the beginning if it is present
     *
     * @param username - The username of the user
     * @return - A populated SkypeUser class
     * - If the api did not return anything, a user with all empty fields except the username will be returned
     */
    public SkypeUser getSkypeUser(String username) {
        return skypeCache.getUsersCache().getOrCreateUserLoaded(username);
    }
    
    /**
     * Gets a Skype conversation from the LONG id
     * The long id should look like this
     * "19:3000ebdcfcca4b42b9f6964f4066e1ad@thread.skype"
     * "8:username"
     *
     * @param longId - The long id of the conversation
     * @return - A populated SkypeConversation class
     * - If the api did not return anything, a conversation with all empty fields except the username will be returned
     */
    public SkypeConversation getSkypeConversation(String longId) {
        return skypeCache.getConversationsCache().getSkypeConversation(longId);
    }
    
    /**
     * Gets all the users Skype conversations
     * @return - The users Skype conversations
     */
    public List<SkypeConversation> getConversations() {
        return (List<SkypeConversation>) (Object) skypeCache.getConversationsCache().getSkypeConversations();
    }
}
