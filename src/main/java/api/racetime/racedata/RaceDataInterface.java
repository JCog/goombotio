package api.racetime.racedata;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

public interface RaceDataInterface {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{raceDataUrl}")
    RaceData getRaceData(@PathParam("raceDataUrl") String raceDataUrl);
}
