package com.ticketrouting.ticket_routing_api.event;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class AiRoutingClient {

    private static final String AI_BASE_URL = "http://127.0.0.1:8000/pdf_search/query";

    private final RestTemplate restTemplate = new RestTemplate();

    public AiSearchResponse search(String subject) {
        System.out.println(">>> [AI-CLIENT] subject=" + subject);
        String url = UriComponentsBuilder
                .fromHttpUrl(AI_BASE_URL)
                .queryParam("query", subject)
                .toUriString();

        return restTemplate.getForObject(url, AiSearchResponse.class);
    }
}
