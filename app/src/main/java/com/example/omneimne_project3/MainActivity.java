package com.example.omneimne_project3;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private FirebaseManager firebaseManager;
    private ListView habitListView;
    private ArrayAdapter<String> adapter;
    private List<Habit> habits;
    private List<String> habitDisplayList;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseManager = new FirebaseManager();
        habitListView = findViewById(R.id.habitListView);
        progressBar = findViewById(R.id.mainProgressBar);
        habitDisplayList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, habitDisplayList);
        habitListView.setAdapter(adapter);

        Button addHabitBtn = findViewById(R.id.addHabitBtn);
        Button viewDailyProgressBtn = findViewById(R.id.viewDailyProgressBtn);
        Button viewSocialBtn = findViewById(R.id.viewSocialBtn);

        addHabitBtn.setOnClickListener(v -> showAddHabitDialog());

        viewDailyProgressBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DailyProgressActivity.class);
            startActivity(intent);
        });

        viewSocialBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SocialActivity.class);
            startActivity(intent);
        });

        habitListView.setOnItemClickListener((parent, view, position, id) -> {
            Habit habit = habits.get(position);
            Intent intent = new Intent(MainActivity.this, HabitRecordsActivity.class);
            intent.putExtra("habitId", habit.getId());
            intent.putExtra("habitName", habit.getName());
            intent.putExtra("habitGoal", habit.getGoal());
            startActivity(intent);
        });

        habitListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Habit habit = habits.get(position);
            showHabitOptionsDialog(habit);
            return true;
        });

        loadHabits();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHabits();
    }

    private void loadHabits() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getUserHabits(new FirebaseManager.HabitsCallback() {
            @Override
            public void onSuccess(List<Habit> result) {
                progressBar.setVisibility(View.GONE);
                habits = result;
                habitDisplayList.clear();
                for (Habit habit : habits) {
                    String visibility = habit.getVisibility() != null ?
                            habit.getVisibility() : "private";
                    String visIcon = visibility.equals("public") ? "🌐 " : "🔒 ";
                    habitDisplayList.add(visIcon + habit.getName() +
                            " (Goal: " + habit.getGoal() + ")");
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddHabitDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_habit_firebase, null);
        EditText nameEdit = dialogView.findViewById(R.id.habitNameEdit);
        EditText goalEdit = dialogView.findViewById(R.id.habitGoalEdit);
        RadioGroup visibilityGroup = dialogView.findViewById(R.id.visibilityRadioGroup);

        new AlertDialog.Builder(this)
                .setTitle("Add New Habit")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = nameEdit.getText().toString().trim();
                    String goalStr = goalEdit.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter a habit name",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (goalStr.isEmpty()) {
                        Toast.makeText(this, "Please enter a goal",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int goal = Integer.parseInt(goalStr);
                        if (goal <= 0) {
                            Toast.makeText(this, "Goal must be positive",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int selectedId = visibilityGroup.getCheckedRadioButtonId();
                        String visibility = selectedId == R.id.publicRadio ?
                                "public" : "private";

                        progressBar.setVisibility(View.VISIBLE);

                        firebaseManager.addHabit(name, goal, visibility,
                                new FirebaseManager.HabitCallback() {
                                    @Override
                                    public void onSuccess(String habitId) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this, "Habit added!",
                                                Toast.LENGTH_SHORT).show();
                                        loadHabits();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this, "Error: " + error,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid goal value",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHabitOptionsDialog(Habit habit) {
        new AlertDialog.Builder(this)
                .setTitle(habit.getName())
                .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditHabitDialog(habit);
                    } else {
                        showDeleteHabitConfirmation(habit);
                    }
                })
                .show();
    }

    private void showEditHabitDialog(Habit habit) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_habit_firebase, null);
        EditText nameEdit = dialogView.findViewById(R.id.habitNameEdit);
        EditText goalEdit = dialogView.findViewById(R.id.habitGoalEdit);
        RadioGroup visibilityGroup = dialogView.findViewById(R.id.visibilityRadioGroup);

        nameEdit.setText(habit.getName());
        goalEdit.setText(String.valueOf(habit.getGoal()));

        if ("public".equals(habit.getVisibility())) {
            ((RadioButton) dialogView.findViewById(R.id.publicRadio)).setChecked(true);
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit Habit")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String name = nameEdit.getText().toString().trim();
                    String goalStr = goalEdit.getText().toString().trim();

                    if (name.isEmpty() || goalStr.isEmpty()) {
                        Toast.makeText(this, "All fields required",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int goal = Integer.parseInt(goalStr);
                        if (goal <= 0) {
                            Toast.makeText(this, "Goal must be positive",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int selectedId = visibilityGroup.getCheckedRadioButtonId();
                        String visibility = selectedId == R.id.publicRadio ?
                                "public" : "private";

                        progressBar.setVisibility(View.VISIBLE);

                        firebaseManager.updateHabit(habit.getId(), name, goal, visibility,
                                new FirebaseManager.HabitCallback() {
                                    @Override
                                    public void onSuccess(String habitId) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this, "Habit updated!",
                                                Toast.LENGTH_SHORT).show();
                                        loadHabits();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(MainActivity.this, "Error: " + error,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid goal value",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteHabitConfirmation(Habit habit) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Habit")
                .setMessage("Are you sure you want to delete \"" + habit.getName() +
                        "\"? This will also delete all associated records.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);

                    firebaseManager.deleteHabit(habit.getId(),
                            new FirebaseManager.HabitCallback() {
                                @Override
                                public void onSuccess(String habitId) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this, "Habit deleted",
                                            Toast.LENGTH_SHORT).show();
                                    loadHabits();
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this, "Error: " + error,
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}