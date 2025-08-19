package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;

import java.security.PublicKey;

/**
 * updated by alex5402 on 2021/5/7.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * TFNQw5HgWUS33Ke1eNmSFTwoQySGU7XNsK (USDT TRC20)
 */
public class VerifierInfo implements Parcelable {

    public static final Parcelable.Creator<VerifierInfo> CREATOR = new Parcelable.Creator<VerifierInfo>() {
        public VerifierInfo createFromParcel(final Parcel source) {
            return new VerifierInfo(source);
        }

        public VerifierInfo[] newArray(final int size) {
            return new VerifierInfo[size];
        }
    };

    public VerifierInfo(final String packageName, final PublicKey publicKey) {
        throw new RuntimeException("Stub!");
    }

    private VerifierInfo(final Parcel source) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public int describeContents() {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        throw new RuntimeException("Stub!");
    }
}