package com.example.omneimne_project3;

public class Habit {
    private String id;
    private String userId;
    private String name;
    private int goal;
    private String visibility;

    public Habit() {}

    public Habit(String id, String name, int goal) {
        this.id = id;
        this.name = name;
        this.goal = goal;
        this.visibility = "private";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGoal() {
        return goal;
    }

    public void setGoal(int goal) {
        this.goal = goal;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}