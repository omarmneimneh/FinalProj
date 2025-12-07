package com.example.omneimne_project3;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseManager {
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public FirebaseManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public interface HabitsCallback {
        void onSuccess(List<Habit> habits);
        void onFailure(String error);
    }

    public interface HabitCallback {
        void onSuccess(String habitId);
        void onFailure(String error);
    }

    public interface RecordsCallback {
        void onSuccess(List<Record> records);
        void onFailure(String error);
    }

    public interface ProgressCallback {
        void onSuccess(List<DailyProgress> progressList);
        void onFailure(String error);
    }

    public interface UserCallback {
        void onSuccess(String displayName);
        void onFailure(String error);
    }

    // Habit CRUD operations
    public void addHabit(String name, int goal, String visibility, HabitCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        Map<String, Object> habit = new HashMap<>();
        habit.put("userId", userId);
        habit.put("name", name);
        habit.put("goal", goal);
        habit.put("visibility", visibility);
        habit.put("createdAt", System.currentTimeMillis());

        db.collection("habits")
                .add(habit)
                .addOnSuccessListener(documentReference ->
                        callback.onSuccess(documentReference.getId()))
                .addOnFailureListener(e ->
                        callback.onFailure(e.getMessage()));
    }

    public void getUserHabits(HabitsCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        db.collection("habits")
                .whereEqualTo("userId", userId)
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Habit> habits = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Habit habit = new Habit();
                        habit.setId(doc.getId());
                        habit.setName(doc.getString("name"));
                        habit.setGoal(doc.getLong("goal").intValue());
                        habit.setVisibility(doc.getString("visibility"));
                        habits.add(habit);
                    }
                    callback.onSuccess(habits);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getPublicHabits(HabitsCallback callback) {
        db.collection("habits")
                .whereEqualTo("visibility", "public")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Habit> habits = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Habit habit = new Habit();
                        habit.setId(doc.getId());
                        habit.setName(doc.getString("name"));
                        habit.setGoal(doc.getLong("goal").intValue());
                        habit.setUserId(doc.getString("userId"));
                        habit.setVisibility(doc.getString("visibility"));
                        habits.add(habit);
                    }
                    callback.onSuccess(habits);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateHabit(String habitId, String name, int goal, String visibility,
                            HabitCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("goal", goal);
        updates.put("visibility", visibility);

        db.collection("habits").document(habitId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(habitId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteHabit(String habitId, HabitCallback callback) {
        db.collection("habits").document(habitId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(habitId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Record operations
    public void addOrUpdateRecord(String habitId, String date, int value,
                                  HabitCallback callback) {
        String recordId = habitId + "_" + date;

        Map<String, Object> record = new HashMap<>();
        record.put("date", date);
        record.put("value", value);
        record.put("timestamp", System.currentTimeMillis());

        db.collection("habits").document(habitId)
                .collection("records").document(recordId)
                .set(record)
                .addOnSuccessListener(aVoid -> callback.onSuccess(recordId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getRecordsForHabit(String habitId, RecordsCallback callback) {
        db.collection("habits").document(habitId)
                .collection("records")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Record> records = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Record record = new Record();
                        record.setId(doc.getId());
                        record.setDate(doc.getString("date"));
                        record.setValue(doc.getLong("value").intValue());
                        records.add(record);
                    }
                    callback.onSuccess(records);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteRecord(String habitId, String recordId, HabitCallback callback) {
        db.collection("habits").document(habitId)
                .collection("records").document(recordId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(recordId))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // Daily progress
    public void getDailyProgress(String date, ProgressCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        db.collection("habits")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(habitSnapshots -> {
                    List<DailyProgress> progressList = new ArrayList<>();
                    int totalHabits = habitSnapshots.size();

                    if (totalHabits == 0) {
                        callback.onSuccess(progressList);
                        return;
                    }

                    final int[] processedHabits = {0};

                    for (QueryDocumentSnapshot habitDoc : habitSnapshots) {
                        String habitId = habitDoc.getId();
                        String habitName = habitDoc.getString("name");
                        int goal = habitDoc.getLong("goal").intValue();

                        String recordId = habitId + "_" + date;

                        db.collection("habits").document(habitId)
                                .collection("records").document(recordId)
                                .get()
                                .addOnCompleteListener(recordTask -> {
                                    DailyProgress progress = new DailyProgress();
                                    progress.setHabitId(habitId);
                                    progress.setHabitName(habitName);
                                    progress.setGoal(goal);

                                    if (recordTask.isSuccessful() &&
                                            recordTask.getResult().exists()) {
                                        DocumentSnapshot recordDoc = recordTask.getResult();
                                        progress.setCurrent(
                                                recordDoc.getLong("value").intValue());
                                    } else {
                                        progress.setCurrent(0);
                                    }

                                    progressList.add(progress);
                                    processedHabits[0]++;

                                    if (processedHabits[0] == totalHabits) {
                                        callback.onSuccess(progressList);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // User operations
    public void getUserDisplayName(String userId, UserCallback callback) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String displayName = documentSnapshot.getString("displayName");
                        callback.onSuccess(displayName != null ? displayName : "Unknown User");
                    } else {
                        callback.onSuccess("Unknown User");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
