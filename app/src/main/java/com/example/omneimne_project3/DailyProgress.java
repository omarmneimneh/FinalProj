package com.example.omneimne_project3;

public class DailyProgress {
    private String habitId;
    private String habitName;
    private int goal;
    private int current;

    public DailyProgress() {}

    public String getHabitId() {
        return habitId;
    }

    public void setHabitId(String habitId) {
        this.habitId = habitId;
    }

    public String getHabitName() {
        return habitName;
    }

    public void setHabitName(String habitName) {
        this.habitName = habitName;
    }

    public int getGoal() {
        return goal;
    }

    public void setGoal(int goal) {
        this.goal = goal;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public float getProgressPercentage() {
        if (goal == 0) return 0;
        return Math.min(100f, (current * 100f) / goal);
    }
}