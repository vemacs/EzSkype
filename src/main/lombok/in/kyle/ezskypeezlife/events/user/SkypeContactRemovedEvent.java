package in.kyle.ezskypeezlife.events.user;

import in.kyle.ezskypeezlife.api.obj.SkypeUser;
import in.kyle.ezskypeezlife.events.SkypeEvent;
import in.kyle.ezskypeezlife.events.SkypeUserEvent;
import lombok.Data;

/**
 * Created by Kyle on 10/9/2015.
 */
@Data
public class SkypeContactRemovedEvent implements SkypeEvent, SkypeUserEvent {
    
    private final SkypeUser user;
    
}
