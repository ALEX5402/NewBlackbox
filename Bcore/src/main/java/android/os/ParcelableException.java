package android.os;

import android.annotation.TargetApi;

import java.io.IOException;


@TargetApi(Build.VERSION_CODES.O)
public final class ParcelableException extends RuntimeException implements Parcelable {
    public ParcelableException(Throwable t) {
        super(t);
    }

    public <T extends Throwable> void maybeRethrow(Class<T> clazz) throws T {
        throw new RuntimeException("Stub!");
    }

    public static Throwable readFromParcel(Parcel in) {
        throw new RuntimeException("Stub!");
    }

    public static void writeToParcel(Parcel out, Throwable t) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        throw new RuntimeException("Stub!");
    }

    public static final Creator<ParcelableException> CREATOR = new Creator<ParcelableException>() {
        @Override
        public ParcelableException createFromParcel(Parcel source) {
            return new ParcelableException(readFromParcel(source));
        }

        @Override
        public ParcelableException[] newArray(int size) {
            return new ParcelableException[size];
        }
    };
}
