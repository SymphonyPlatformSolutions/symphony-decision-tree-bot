package com.symphony.platformsolutions.decisiontree.web;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class WebService {
    @GET
    @Path("healthz")
    @Produces(MediaType.APPLICATION_JSON)
    public String a() {
        return "{ \"status\": \"UP\" }";
    }
}
