package models;

public class Client extends User {
    private String preferences;

    public String getPreferences() { return preferences; }
    public void setPreferences(String preferences) { this.preferences = preferences; }
}