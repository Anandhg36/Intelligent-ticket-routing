package com.ticketrouting.ticket_routing_api.event;

import com.ticketrouting.ticket_routing_api.dto.AiTeamConfidence;

import java.util.List;

public class AiSearchResponse {

    private Boolean autoAssign;
    private List<AiTeamConfidence> teams;
    private List<AiSearchResult> results;

    public Boolean getAutoAssign() {
        return autoAssign;
    }

    public void setAutoAssign(Boolean autoAssign) {
        this.autoAssign = autoAssign;
    }

    public List<AiTeamConfidence> getTeams() {
        return teams;
    }

    public void setTeams(List<AiTeamConfidence> teams) {
        this.teams = teams;
    }

    public List<AiSearchResult> getResults() {
        return results;
    }

    public void setResults(List<AiSearchResult> results) {
        this.results = results;
    }
}
