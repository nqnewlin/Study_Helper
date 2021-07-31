package com.zybooks.studyhelper;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.VolleyError;
import java.util.List;

public class ImportActivity extends AppCompatActivity {

    private LinearLayout mSubjectLayoutContainer;
    private StudyFetcher mStudyFetcher;
    private ProgressBar mLoadingProgressBar;
    private boolean mDarkTheme;
    private SharedPreferences mSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDarkTheme = mSharedPrefs.getBoolean(SettingsFragment.PREFERENCE_THEME, false);
        if (mDarkTheme) {
            setTheme(R.style.DarkTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);

        mSubjectLayoutContainer = findViewById(R.id.subjectLayout);

        // Show progress bar
        mLoadingProgressBar = findViewById(R.id.loadingProgressBar);
        mLoadingProgressBar.setVisibility(View.VISIBLE);

        mStudyFetcher = new StudyFetcher(this);
        mStudyFetcher.fetchSubjects(mFetchListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // If theme changed, recreate the activity so theme is applied
        boolean darkTheme = mSharedPrefs.getBoolean(SettingsFragment.PREFERENCE_THEME, false);
        if (darkTheme != mDarkTheme) {
            recreate();
        }

    }

    private StudyFetcher.OnStudyDataReceivedListener mFetchListener = new StudyFetcher.OnStudyDataReceivedListener() {

        @Override
        public void onSubjectsReceived(List<Subject> subjects) {

            // Hide progress bar
            mLoadingProgressBar.setVisibility(View.GONE);
            boolean darkTheme = mSharedPrefs.getBoolean(SettingsFragment.PREFERENCE_THEME, false);

            // Create a checkbox for each subject
            for (Subject subject: subjects) {
                CheckBox checkBox = new CheckBox(getApplicationContext());
                checkBox.setTextSize(24);
                if (darkTheme) {
                    checkBox.setTextColor(Color.WHITE);
                }
                checkBox.setText(subject.getText());
                checkBox.setTag(subject);
                mSubjectLayoutContainer.addView(checkBox);
            }
        }

        @Override
        public void onQuestionsReceived(List<Question> questions) {

            if (questions.size() > 0) {
                StudyDatabase studyDb = StudyDatabase.getInstance(getApplicationContext());

                // Add the questions to the database
                for (Question question : questions) {
                    studyDb.addQuestion(question);
                }

                String subject = questions.get(0).getSubject();
                Toast.makeText(getApplicationContext(), subject + " imported successfully",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            Toast.makeText(getApplicationContext(), "Error loading subjects. Try again later.",
                    Toast.LENGTH_LONG).show();
            mLoadingProgressBar.setVisibility(View.GONE);
        }
    };

    public void importButtonClick(View view) {

        StudyDatabase dbHelper = StudyDatabase.getInstance(getApplicationContext());

        // Determine which subjects were selected
        int numCheckBoxes = mSubjectLayoutContainer.getChildCount();
        for (int i = 0; i < numCheckBoxes; i++) {
            CheckBox checkBox = (CheckBox) mSubjectLayoutContainer.getChildAt(i);
            if (checkBox.isChecked()) {
                Subject subject = (Subject) checkBox.getTag();

                // Add subject to the database
                if (dbHelper.addSubject(subject)) {
                    mStudyFetcher.fetchQuestions(subject, mFetchListener);
                } else {
                    Toast.makeText(this, subject.getText() + " is already imported.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}