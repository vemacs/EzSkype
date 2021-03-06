package in.kyle.ezskypeezlife.internal.packet.auth;

import in.kyle.ezskypeezlife.EzSkype;
import in.kyle.ezskypeezlife.internal.obj.SkypeSession;
import in.kyle.ezskypeezlife.internal.packet.HTTPRequest;
import in.kyle.ezskypeezlife.internal.packet.SkypePacket;
import in.kyle.ezskypeezlife.internal.packet.WebConnectionBuilder;

import java.util.UUID;

/**
 * Created by Kyle on 10/5/2015.
 */
public class SkypeAuthFinishPacket extends SkypePacket {
    
    private final String token;
    
    public SkypeAuthFinishPacket(EzSkype ezSkype, String token) {
        super("https://client-s.gateway.messenger.live.com/v1/users/ME/endpoints", HTTPRequest.POST, ezSkype, false);
        this.token = token;
    }
    
    @Override
    protected SkypeSession run(WebConnectionBuilder webConnectionBuilder) throws Exception {
        webConnectionBuilder.addHeader("Authentication", "skypetoken=" + token);
        webConnectionBuilder.setPostData("{}");
        
        webConnectionBuilder.send();
        
        String location = webConnectionBuilder.getConnection().getHeaderField("Location");
        location = location.substring(location.indexOf("//") + 2, location.indexOf("client"));
        
        String[] tokenOre = webConnectionBuilder.getConnection().getHeaderField("Set-RegistrationToken").split(";");
        String regToken = tokenOre[0];
        
        String endpoint = tokenOre[2].substring(tokenOre[2].indexOf("=") + 1);
    
        return new SkypeSession(regToken, token, location, endpoint, UUID.randomUUID());
    }
}
