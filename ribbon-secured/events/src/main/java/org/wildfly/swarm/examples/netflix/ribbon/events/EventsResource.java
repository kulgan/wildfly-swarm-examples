package org.wildfly.swarm.examples.netflix.ribbon.events;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * @author Bob McWhirter
 */
@Path("/")
public class EventsResource {

    private final TimeService time;
    private static final ArrayList<Event> EVENTS = new ArrayList<>();

    public EventsResource() {
        this.time = TimeService.INSTANCE;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public void get(@Suspended final AsyncResponse asyncResponse) {
        System.err.println( "I was asked for event" );
        Event event = new Event();
        event.setName("GET");
        recordEvent(event, asyncResponse);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(Event event, @Suspended final AsyncResponse asyncResponse) {
        recordEvent(event, asyncResponse);
    }

    private void recordEvent(Event event, @Suspended AsyncResponse asyncResponse) {
        System.err.println( "asking for time" );
        Observable<ByteBuf> obs = this.time.currentTime().observe();
        System.err.println( "asked for time" );
        obs.subscribe(
                (result) -> {
                    System.err.println( "got time result: " + result );
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        ObjectReader reader = mapper.reader();
                        JsonFactory factory = new JsonFactory();
                        JsonParser parser = factory.createParser(new ByteBufInputStream(result));
                        Map map = reader.readValue(parser, Map.class);
                        event.setTimestamp(map);
                        event.setId(EVENTS.size());
                        EVENTS.add(event);
                        asyncResponse.resume(EVENTS);
                    } catch (IOException e) {
                        System.err.println("ERROR: " + e.getLocalizedMessage());
                        asyncResponse.resume(e);
                    }
                },
                (err) -> {
                    System.err.println("ERROR: " + err.getLocalizedMessage());
                    asyncResponse.resume(err);
                });
    }
}
