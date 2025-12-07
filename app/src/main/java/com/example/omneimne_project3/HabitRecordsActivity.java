package com.example.omneimne_project3;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HabitRecordsActivity extends AppCompatActivity {
    private FirebaseManager firebaseManager;
    private ListView recordsListView;
    private ArrayAdapter<String> adapter;
    private List<Record> records;
    private List<String> recordDisplayList;
    private String habitId;
    private String habitName;
    private int habitGoal;
    private TextView titleText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_records);

        firebaseManager = new FirebaseManager();

        habitId = getIntent().getStringExtra("habitId");
        habitName = getIntent().getStringExtra("habitName");
        habitGoal = getIntent().getIntExtra("habitGoal", 0);

        titleText = findViewById(R.id.habitTitleText);
        titleText.setText(habitName + " (Goal: " + habitGoal + ")");

        recordsListView = findViewById(R.id.recordsListView);
        progressBar = findViewById(R.id.recordsProgressBar);
        recordDisplayList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recordDisplayList);
        recordsListView.setAdapter(adapter);

        Button addRecordBtn = findViewById(R.id.addRecordBtn);
        addRecordBtn.setOnClickListener(v -> showAddRecordDialog());

        recordsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            Record record = records.get(position);
            showRecordOptionsDialog(record);
            return true;
        });

        loadRecords();
    }

    private void loadRecords() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.getRecordsForHabit(habitId, new FirebaseManager.RecordsCallback() {
            @Override
            public void onSuccess(List<Record> result) {
                progressBar.setVisibility(View.GONE);
                records = result;
                recordDisplayList.clear();
                for (Record record : records) {
                    recordDisplayList.add(record.getDate() + ": " + record.getValue() +
                            "/" + habitGoal);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(HabitRecordsActivity.this, "Error: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddRecordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_record, null);
        EditText valueEdit = dialogView.findViewById(R.id.recordValueEdit);
        Button datePickerBtn = dialogView.findViewById(R.id.datePickerBtn);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        final String[] selectedDate = {sdf.format(calendar.getTime())};

        datePickerBtn.setText(selectedDate[0]);
        datePickerBtn.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    HabitRecordsActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        selectedDate[0] = sdf.format(calendar.getTime());
                        datePickerBtn.setText(selectedDate[0]);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Add Record")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String valueStr = valueEdit.getText().toString().trim();

                    if (valueStr.isEmpty()) {
                        Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int value = Integer.parseInt(valueStr);
                        if (value < 0) {
                            Toast.makeText(this, "Value cannot be negative",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        progressBar.setVisibility(View.VISIBLE);

                        firebaseManager.addOrUpdateRecord(habitId, selectedDate[0], value,
                                new FirebaseManager.HabitCallback() {
                                    @Override
                                    public void onSuccess(String recordId) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(HabitRecordsActivity.this,
                                                "Record saved!", Toast.LENGTH_SHORT).show();
                                        loadRecords();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(HabitRecordsActivity.this,
                                                "Error: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid value", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRecordOptionsDialog(Record record) {
        new AlertDialog.Builder(this)
                .setTitle(record.getDate())
                .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditRecordDialog(record);
                    } else {
                        showDeleteRecordConfirmation(record);
                    }
                })
                .show();
    }

    private void showEditRecordDialog(Record record) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_record, null);
        EditText valueEdit = dialogView.findViewById(R.id.editRecordValueEdit);
        TextView dateText = dialogView.findViewById(R.id.editRecordDateText);

        dateText.setText("Date: " + record.getDate());
        valueEdit.setText(String.valueOf(record.getValue()));

        new AlertDialog.Builder(this)
                .setTitle("Edit Record")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String valueStr = valueEdit.getText().toString().trim();

                    if (valueStr.isEmpty()) {
                        Toast.makeText(this, "Please enter a value", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int value = Integer.parseInt(valueStr);
                        if (value < 0) {
                            Toast.makeText(this, "Value cannot be negative",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        progressBar.setVisibility(View.VISIBLE);

                        firebaseManager.addOrUpdateRecord(habitId, record.getDate(), value,
                                new FirebaseManager.HabitCallback() {
                                    @Override
                                    public void onSuccess(String recordId) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(HabitRecordsActivity.this,
                                                "Record updated!", Toast.LENGTH_SHORT).show();
                                        loadRecords();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(HabitRecordsActivity.this,
                                                "Error: " + error, Toast.LENGTH_SHORT).show();
                                    }
                                });

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid value", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteRecordConfirmation(Record record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Delete record for " + record.getDate() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);

                    firebaseManager.deleteRecord(habitId, record.getId(),
                            new FirebaseManager.HabitCallback() {
                                @Override
                                public void onSuccess(String recordId) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(HabitRecordsActivity.this,
                                            "Record deleted", Toast.LENGTH_SHORT).show();
                                    loadRecords();
                                }

                                @Override
                                public void onFailure(String error) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(HabitRecordsActivity.this,
                                            "Error: " + error, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}