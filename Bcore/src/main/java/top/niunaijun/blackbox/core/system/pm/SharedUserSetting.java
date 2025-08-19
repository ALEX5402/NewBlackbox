/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.niunaijun.blackbox.core.system.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.AtomicFile;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Settings data for a particular shared user ID we know about.
 */
public final class SharedUserSetting implements Parcelable {
    public static final String TAG = "SharedUserSetting";
    public static final Map<String, SharedUserSetting> sSharedUsers = new HashMap<>();

    String name;
    int userId;

    // The lowest targetSdkVersion of all apps in the sharedUserSetting, used to assign seinfo so
    // that all apps within the sharedUser run in the same selinux context.
    int seInfoTargetSdkVersion;

    SharedUserSetting(String _name) {
        name = _name;
    }

    SharedUserSetting() {
        // Default constructor for Parcelable
    }

    @Override
    public String toString() {
        return "SharedUserSetting{" + Integer.toHexString(System.identityHashCode(this)) + " "
                + name + "/" + userId + "}";
    }

    public static void saveSharedUsers() {
        Parcel parcel = Parcel.obtain();
        FileOutputStream fileOutputStream = null;
        AtomicFile atomicFile = new AtomicFile(BEnvironment.getSharedUserConf());
        try {
            parcel.writeMap(sSharedUsers);

            fileOutputStream = atomicFile.startWrite();
            FileUtils.writeParcelToOutput(parcel, fileOutputStream);
            atomicFile.finishWrite(fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
            atomicFile.failWrite(fileOutputStream);
        } finally {
            parcel.recycle();
        }
    }

    public static void loadSharedUsers() {
        Parcel parcel = Parcel.obtain();
        try {
            byte[] sharedUsersBytes = FileUtils.toByteArray(BEnvironment.getSharedUserConf());
            if (sharedUsersBytes == null || sharedUsersBytes.length == 0) {
                // No data to load, start fresh
                return;
            }
            
            // Check if this is old format data (before seInfoTargetSdkVersion was added)
            // If the data is too small, it's likely the old format
            if (sharedUsersBytes.length < 100) { // Arbitrary threshold for old format
                Slog.w(TAG, "Detected old format SharedUserSetting data, clearing for fresh start");
                BEnvironment.getSharedUserConf().delete();
                synchronized (sSharedUsers) {
                    sSharedUsers.clear();
                }
                return;
            }
            
            parcel.unmarshall(sharedUsersBytes, 0, sharedUsersBytes.length);
            parcel.setDataPosition(0);

            HashMap hashMap = parcel.readHashMap(SharedUserSetting.class.getClassLoader());
            synchronized (sSharedUsers) {
                sSharedUsers.clear();
                sSharedUsers.putAll(hashMap);
            }
        } catch (Exception e) {
            // If loading fails, clear the corrupted data and start fresh
            try {
                // Delete the corrupted file
                BEnvironment.getSharedUserConf().delete();
            } catch (Exception deleteException) {
                // Ignore delete errors
            }
            
            // Clear the shared users map to start fresh
            synchronized (sSharedUsers) {
                sSharedUsers.clear();
            }
        } finally {
            parcel.recycle();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeInt(this.userId);
        dest.writeInt(this.seInfoTargetSdkVersion);
    }

    public void readFromParcel(Parcel source) {
        this.name = source.readString();
        this.userId = source.readInt();
        this.seInfoTargetSdkVersion = source.readInt();
    }

    public static final Parcelable.Creator<SharedUserSetting> CREATOR = new Parcelable.Creator<SharedUserSetting>() {
        @Override
        public SharedUserSetting createFromParcel(Parcel source) {
            SharedUserSetting setting = new SharedUserSetting();
            setting.readFromParcel(source);
            return setting;
        }

        @Override
        public SharedUserSetting[] newArray(int size) {
            return new SharedUserSetting[size];
        }
    };
}
