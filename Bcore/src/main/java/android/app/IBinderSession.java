package android.app;

import android.os.IInterface;
import android.os.RemoteException;

public interface IBinderSession extends IInterface {
    void binderTransactionCompleted(long val) throws RemoteException;
    long binderTransactionStarting(String str) throws RemoteException;
}
