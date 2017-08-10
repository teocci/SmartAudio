package com.github.teocci.newsmartaudio.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

import com.github.teocci.newsmartaudio.R;
import com.github.teocci.newsmartaudio.utils.LogHelper;

import java.io.IOException;
import java.io.InputStream;

import static com.github.teocci.newsmartaudio.utils.Config.KEY_STATION_NAME;
import static net.kseek.streaming.utils.Config.KEY_OPEN_SOURCE_LICENSE;


public class PreferencesActivity extends AppCompatPreferenceActivity
{
    static final public String TAG = LogHelper.makeLogTag(PreferencesActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

//        ActionBar actionBar = getActionBar();
//        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#272D39"));
//        actionBar.setBackgroundDrawable(colorDrawable);

        addPreferencesFromResource(R.xml.preferences);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        final Preference stationName = findPreference(KEY_STATION_NAME);
        stationName.setSummary(settings.getString(KEY_STATION_NAME, null));
        stationName.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                preference.getEditor().putString(KEY_STATION_NAME, newValue.toString()).apply();
                preference.setSummary(newValue.toString());
                return true;
            }
        });

        final Preference openSourceLicense = findPreference(KEY_OPEN_SOURCE_LICENSE);
        openSourceLicense.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                String content = readFile("open_source_license.txt");

                AlertDialog.Builder alertDlg = new AlertDialog.Builder(PreferencesActivity.this);
                alertDlg.setTitle(getString(R.string.open_source_license_title));
                alertDlg.setMessage(content);
                alertDlg.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        dialog.dismiss();
                    }
                });
                alertDlg.show();
                return false;
            }
        });
    }

    private String readFile(String fileName)
    {
        try {
            StringBuffer stringBuffer = new StringBuffer();
            InputStream in = getAssets().open(fileName);
            byte[] buf = new byte[65535];
            int read;
            while (true) {
                read = in.read(buf);
                if (read == -1) break;

                stringBuffer.append(new String(buf, 0, read, "UTF-8"));
            }
            in.close();

            return stringBuffer.toString();
        } catch (IOException e) {
            LogHelper.e("PreferencesActivity", e.toString());
        }
        return null;
    }
}
