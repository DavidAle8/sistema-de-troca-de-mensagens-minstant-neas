package br.ufs.dcomp.ChatRabbitMQ;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class Rest {

    public  static String doGet(String path) throws Exception {
        
        String BASE_URL = "https://shark.rmq.cloudamqp.com";
        String USERNAME = "jwpauneq";
        String PASSWORD = "FY1tTGNmLVBoQfwlMiBl2z_Pc5AIaNS6";

        String auth = USERNAME + ":" + PASSWORD;
        String authHeader = "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes());

        Client client = ClientBuilder.newClient();

        Response response = client
            .target(BASE_URL)
            .path(path)
            .request(MediaType.APPLICATION_JSON)
            .header("Authorization", authHeader)
            .get();

        if (response.getStatus() == 200) {
            return response.readEntity(String.class);
        } else {
            throw new RuntimeException("HTTP error: " + response.getStatus());
        }
    }
}
