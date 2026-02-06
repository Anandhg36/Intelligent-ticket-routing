package com.ticketrouting.ticket_routing_api.event;

public class AiSearchResult {

    private String path;
    private String team;
    private String text;
    private Double score;
    private Double boostContribution;
    private  String ai_suggested_message;
    private Double teamConfidence;


    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public Double getBoostContribution() { return boostContribution; }
    public void setBoostContribution(Double boostContribution) { this.boostContribution = boostContribution; }

    public String getAiSuggestedMessage() {
        return ai_suggested_message;
    }
    public Double getTeamConfidence() {
        return teamConfidence;
    }
    public void setTeamConfidence(Double teamConfidence) {
        this.teamConfidence = teamConfidence;
    }
}
