package com.example.omneimne_project3;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DailyProgressActivity extends AppCompatActivity {
    private FirebaseManager firebaseManager;
    private LinearLayout progressContainer;
    private TextView dateText;
    private ProgressBar progressBar;
    private SimpleDateFormat sdf;
    private Calendar calendar;
    private String currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_progress);

        firebaseManager = new FirebaseManager();
        progressContainer = findViewById(R.id.progressContainer);
        dateText = findViewById(R.id.dateText);
        progressBar = findViewById(R.id.dailyProgressBar);
        Button changeDateBtn = findViewById(R.id.changeDateBtn);

        sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        calendar = Calendar.getInstance();
        currentDate = sdf.format(calendar.getTime());

        dateText.setText("Progress for: " + currentDate);

        changeDateBtn.setOnClickListener(v -> showDatePicker());

        loadDailyProgress();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    currentDate = sdf.format(calendar.getTime());
                    dateText.setText("Progress for: " + currentDate);
                    loadDailyProgress();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadDailyProgress() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getDailyProgress(currentDate,
                new FirebaseManager.ProgressCallback() {
                    @Override
                    public void onSuccess(List<DailyProgress> progressList) {
                        progressBar.setVisibility(View.GONE);
                        displayProgress(progressList);
                    }

                    @Override
                    public void onFailure(String error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(DailyProgressActivity.this, "Error: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayProgress(List<DailyProgress> progressList) {
        progressContainer.removeAllViews();

        if (progressList.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No habits to track");
            emptyText.setTextSize(16);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(dpToPixel(16), dpToPixel(16), dpToPixel(16), dpToPixel(16));
            progressContainer.addView(emptyText);
            return;
        }

        for (DailyProgress progress : progressList) {
            addProgressBar(progress);
        }
    }

    private void addProgressBar(DailyProgress progress) {
        // Container for each habit's progress
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(dpToPixel(16), dpToPixel(8), dpToPixel(16), dpToPixel(8));

        // Habit name and values
        TextView nameText = new TextView(this);
        nameText.setText(progress.getHabitName() + ": " + progress.getCurrent() +
                "/" + progress.getGoal());
        nameText.setTextSize(18);
        nameText.setTextColor(Color.BLACK);
        itemLayout.addView(nameText);

        // Progress bar container
        LinearLayout barContainer = new LinearLayout(this);
        barContainer.setOrientation(LinearLayout.HORIZONTAL);
        barContainer.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPixel(30)
        );
        containerParams.setMargins(0, dpToPixel(8), 0, dpToPixel(8));
        barContainer.setLayoutParams(containerParams);

        // Progress bar
        TextView progressBarView = new TextView(this);
        float percentage = progress.getProgressPercentage();
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        int availableWidth = screenWidthDp - 32; // Subtract padding
        int progressWidth = (int) (availableWidth * percentage / 100);

        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                dpToPixel(progressWidth),
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        progressBarView.setLayoutParams(barParams);

        // Color based on progress
        if (percentage >= 100) {
            progressBarView.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
        } else if (percentage >= 50) {
            progressBarView.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
        } else {
            progressBarView.setBackgroundColor(Color.parseColor("#F44336")); // Red
        }

        progressBarView.setText(String.format(Locale.US, "%.0f%%", percentage));
        progressBarView.setTextColor(Color.WHITE);
        progressBarView.setGravity(Gravity.CENTER);
        progressBarView.setTextSize(14);

        barContainer.addView(progressBarView);
        itemLayout.addView(barContainer);

        progressContainer.addView(itemLayout);
    }

    private int dpToPixel(int dps) {
        return (int) (dps * getResources().getDisplayMetrics().density);
    }
}