

package android.content;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;


public class SyncInfo implements Parcelable {
    
    private static final Account REDACTED_ACCOUNT = new Account("*****", "*****");

    
    public final int authorityId;

    
    public final Account account;

    
    public final String authority;

    
    public final long startTime;

    
    public static SyncInfo createAccountRedacted(
        int authorityId, String authority, long startTime) {
        throw new RuntimeException("Stub!");
    }

    
    public SyncInfo(int authorityId, Account account, String authority, long startTime) {
        throw new RuntimeException("Stub!");
    }

    
    public SyncInfo(SyncInfo other) {
        throw new RuntimeException("Stub!");
    }

    
    public int describeContents() {
        return 0;
    }

    
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(authorityId);
        parcel.writeParcelable(account, flags);
        parcel.writeString(authority);
        parcel.writeLong(startTime);
    }

    SyncInfo(Parcel parcel) {
        throw new RuntimeException("Stub!");
    }

    public static final Creator<SyncInfo> CREATOR = new Creator<SyncInfo>() {
        public SyncInfo createFromParcel(Parcel in) {
            return new SyncInfo(in);
        }

        public SyncInfo[] newArray(int size) {
            return new SyncInfo[size];
        }
    };
}
