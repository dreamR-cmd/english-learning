package com.english.dto;

public class ExamCountdown {
    private String label;
    private String dateText;
    private String countdownText;
    private boolean urgent;

    public ExamCountdown() {}

    public ExamCountdown(String label, String dateText, String countdownText, boolean urgent) {
        this.label = label;
        this.dateText = dateText;
        this.countdownText = countdownText;
        this.urgent = urgent;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDateText() { return dateText; }
    public void setDateText(String dateText) { this.dateText = dateText; }
    public String getCountdownText() { return countdownText; }
    public void setCountdownText(String countdownText) { this.countdownText = countdownText; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
}
