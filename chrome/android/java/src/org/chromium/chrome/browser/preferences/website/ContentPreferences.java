// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.preferences.website;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import org.chromium.base.CommandLine;
import org.chromium.chrome.ChromeSwitches;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.ContentSettingsType;
import org.chromium.chrome.browser.preferences.ChromeBaseCheckBoxPreference;
import org.chromium.chrome.browser.preferences.LocationSettings;
import org.chromium.chrome.browser.preferences.ManagedPreferenceDelegate;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * The fragment displayed on Settings -> Content settings.
 */
public class ContentPreferences extends PreferenceFragment
        implements OnPreferenceChangeListener, OnPreferenceClickListener {
    // The keys for each category shown on the Content Settings page.
    static final String ALL_SITES_KEY = "website_settings";
    static final String COOKIES_KEY = "cookies";
    static final String LOCATION_KEY = "device_location";
    static final String CAMERA_AND_MIC_KEY = "use_camera_or_mic";
    static final String JAVASCRIPT_KEY = "enable_javascript";
    static final String BLOCK_POPUPS_KEY = "block_popups";
    static final String PUSH_NOTIFICATIONS_KEY = "push_notifications";
    static final String POPUPS_KEY = "popups";
    static final String PROTECTED_CONTENT_KEY = "protected_content";
    static final String TRANSLATE_KEY = "translate";
    static final String STORAGE_KEY = "use_storage";

    private ManagedPreferenceDelegate mManagedPreferenceDelegate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.content_preferences);
        getActivity().setTitle(R.string.prefs_content_settings);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            getPreferenceScreen().removePreference(findPreference(PROTECTED_CONTENT_KEY));
        }

        if (!pushNotificationsSupported()) {
            getPreferenceScreen().removePreference(findPreference(PUSH_NOTIFICATIONS_KEY));
        }

        // Set up the checkbox preferences.
        List<String> checkBoxPreferences = new ArrayList<String>();
        checkBoxPreferences.add(JAVASCRIPT_KEY);

        for (String prefName : checkBoxPreferences) {
            Preference p = findPreference(prefName);
            p.setOnPreferenceChangeListener(this);
        }

        mManagedPreferenceDelegate = createManagedPreferenceDelegate();

        updatePreferenceStates();
    }

    private int keyToContentSettingsType(String key) {
        if (COOKIES_KEY.equals(key)) {
            return ContentSettingsType.CONTENT_SETTINGS_TYPE_COOKIES;
        } else if (LOCATION_KEY.equals(key)) {
            return ContentSettingsType.CONTENT_SETTINGS_TYPE_GEOLOCATION;
        } else if (CAMERA_AND_MIC_KEY.equals(key)) {
            return ContentSettingsType.CONTENT_SETTINGS_TYPE_MEDIASTREAM;
        } else if (PUSH_NOTIFICATIONS_KEY.equals(key)) {
            return ContentSettingsType.CONTENT_SETTINGS_TYPE_NOTIFICATIONS;
        } else if (POPUPS_KEY.equals(key)) {
            return ContentSettingsType.CONTENT_SETTINGS_TYPE_POPUPS;
        } else if (PROTECTED_CONTENT_KEY.equals(key)) {
            return ContentSettingsType.CONTENT_SETTINGS_TYPE_PROTECTED_MEDIA_IDENTIFIER;
        }
        return -1;
    }

    /**
     * Returns whether Push Notifications (Push Messaging) is supported.
     */
    public static boolean pushNotificationsSupported() {
        return CommandLine.getInstance().hasSwitch(
                ChromeSwitches.EXPERIMENTAL_WEB_PLAFTORM_FEATURES);
    }

    private void updatePreferenceStates() {
        PrefServiceBridge prefServiceBridge = PrefServiceBridge.getInstance();

        // Translate preference.
        Preference translatePref = findPreference(TRANSLATE_KEY);
        if (translatePref != null) {
            setTranslateStateSummary(translatePref);
        }

        // JavaScript preference.
        ChromeBaseCheckBoxPreference javascriptPref =
                (ChromeBaseCheckBoxPreference) findPreference(JAVASCRIPT_KEY);
        javascriptPref.setChecked(prefServiceBridge.javaScriptEnabled());
        javascriptPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);

        // Preferences that navigate to Website Settings.
        List<String> websitePrefs = new ArrayList<String>();
        websitePrefs.add(LOCATION_KEY);
        if (Build.VERSION.SDK_INT >= 19) {
            websitePrefs.add(PROTECTED_CONTENT_KEY);
        }
        websitePrefs.add(COOKIES_KEY);
        websitePrefs.add(CAMERA_AND_MIC_KEY);
        if (pushNotificationsSupported()) {
            websitePrefs.add(PUSH_NOTIFICATIONS_KEY);
        }
        websitePrefs.add(POPUPS_KEY);
        // Initialize the summary and icon for all preferences that have an
        // associated content settings entry.
        for (String prefName : websitePrefs) {
            Preference p = findPreference(prefName);
            int type = keyToContentSettingsType(prefName);
            Website.PermissionDataEntry entry =
                    Website.PermissionDataEntry.getPermissionDataEntry(type);
            boolean checked = false;
            if (LOCATION_KEY.equals(p.getKey())) {
                checked = LocationSettings.areAllLocationSettingsEnabled();
            } else if (CAMERA_AND_MIC_KEY.equals(prefName)) {
                checked = PrefServiceBridge.getInstance().isCameraMicEnabled();
            } else if (PROTECTED_CONTENT_KEY.equals(prefName)) {
                checked = PrefServiceBridge.getInstance().isProtectedMediaIdentifierEnabled();
            } else if (COOKIES_KEY.equals(prefName)) {
                checked = PrefServiceBridge.getInstance().isAcceptCookiesEnabled();
            } else if (PUSH_NOTIFICATIONS_KEY.equals(prefName)) {
                checked = PrefServiceBridge.getInstance().isPushNotificationsEnabled();
            } else if (POPUPS_KEY.equals(prefName)) {
                checked = PrefServiceBridge.getInstance().popupsEnabled();
            }
            p.setTitle(entry.titleResourceId);
            if (COOKIES_KEY.equals(prefName) && checked
                    && prefServiceBridge.isBlockThirdPartyCookiesEnabled()) {
                p.setSummary(R.string.website_settings_category_allowed_except_third_party);
            } else {
                p.setSummary(checked
                        ? entry.contentSettingToResourceIdForCategory(entry.defaultEnabledValue)
                        : entry.contentSettingToResourceIdForCategory(entry.defaultDisabledValue));
            }
            p.setIcon(entry.iconResourceId);
            p.setOnPreferenceClickListener(this);
        }

        Preference p = findPreference(ALL_SITES_KEY);
        p.setOnPreferenceClickListener(this);
        // TODO(finnur): Re-move this for Storage once it can be moved to the 'Usage' menu.
        p = findPreference(STORAGE_KEY);
        p.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
    }

    // OnPreferenceClickListener:

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (!ALL_SITES_KEY.equals(preference.getKey())) {
            preference.getExtras().putString(
                    WebsitePreferences.EXTRA_CATEGORY, preference.getKey());
        }
        preference.getExtras().putString(WebsitePreferences.EXTRA_TITLE,
                preference.getTitle().toString());
        return false;
    }

    // OnPreferenceChangeListener:

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!preference.isEnabled()) return false;

        String key = preference.getKey();
        if (JAVASCRIPT_KEY.equals(key)) {
            PrefServiceBridge.getInstance().setJavaScriptEnabled((boolean) newValue);
        }
        return true;
    }

    private void setTranslateStateSummary(Preference translatePref) {
        boolean translateEnabled = PrefServiceBridge.getInstance().isTranslateEnabled();
        translatePref.setSummary(translateEnabled
                ? R.string.website_settings_category_ask
                : R.string.website_settings_category_blocked);
    }

    private ManagedPreferenceDelegate createManagedPreferenceDelegate() {
        return new ManagedPreferenceDelegate() {
            @Override
            public boolean isPreferenceControlledByPolicy(Preference preference) {
                if (JAVASCRIPT_KEY.equals(preference.getKey())) {
                    return PrefServiceBridge.getInstance().javaScriptManaged();
                }
                return false;
            }
        };
    }
}
