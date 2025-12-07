package com.example.omneimne_project3;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialActivity extends AppCompatActivity {
    private FirebaseManager firebaseManager;
    private ListView socialHabitsListView;
    private ArrayAdapter<String> adapter;
    private List<Habit> publicHabits;
    private List<String> habitDisplayList;
    private ProgressBar progressBar;
    private Map<String, String> userNamesCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social);

        firebaseManager = new FirebaseManager();
        socialHabitsListView = findViewById(R.id.socialHabitsListView);
        progressBar = findViewById(R.id.socialProgressBar);
        habitDisplayList = new ArrayList<>();
        userNamesCache = new HashMap<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, habitDisplayList);
        socialHabitsListView.setAdapter(adapter);

        loadPublicHabits();
    }

    private void loadPublicHabits() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getPublicHabits(new FirebaseManager.HabitsCallback() {
            @Override
            public void onSuccess(List<Habit> result) {
                publicHabits = result;
                habitDisplayList.clear();

                if (publicHabits.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    habitDisplayList.add("No public habits yet. Be the first to share!");
                    adapter.notifyDataSetChanged();
                    return;
                }

                // Load user names for each habit
                final int[] processedCount = {0};
                for (Habit habit : publicHabits) {
                    String userId = habit.getUserId();

                    // Check cache first
                    if (userNamesCache.containsKey(userId)) {
                        String userName = userNamesCache.get(userId);
                        habitDisplayList.add(formatHabitDisplay(habit, userName));
                        processedCount[0]++;

                        if (processedCount[0] == publicHabits.size()) {
                            progressBar.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        // Fetch from Firestore
                        firebaseManager.getUserDisplayName(userId,
                                new FirebaseManager.UserCallback() {
                                    @Override
                                    public void onSuccess(String displayName) {
                                        userNamesCache.put(userId, displayName);
                                        habitDisplayList.add(formatHabitDisplay(habit, displayName));
                                        processedCount[0]++;

                                        if (processedCount[0] == publicHabits.size()) {
                                            progressBar.setVisibility(View.GONE);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        habitDisplayList.add(formatHabitDisplay(habit, "Unknown User"));
                                        processedCount[0]++;

                                        if (processedCount[0] == publicHabits.size()) {
                                            progressBar.setVisibility(View.GONE);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                    }
                }
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SocialActivity.this, "Error: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatHabitDisplay(Habit habit, String userName) {
        String currentUserId = firebaseManager.getCurrentUserId();
        boolean isCurrentUser = habit.getUserId().equals(currentUserId);

        if (isCurrentUser) {
            return "🌐 " + habit.getName() + " (Goal: " + habit.getGoal() + ") - You";
        } else {
            return "🌐 " + habit.getName() + " (Goal: " + habit.getGoal() +
                    ") - by " + userName;
        }
    }
}