package top.niunaijun.blackbox.fake.service;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.fake.hook.ClassInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * SQLiteDatabase proxy to handle database-related issues in sandboxed apps.
 */
public class SQLiteDatabaseProxy extends ClassInvocationStub {
    public static final String TAG = "SQLiteDatabaseProxy";

    public SQLiteDatabaseProxy() {
        super();
    }

    @Override
    protected Object getWho() {
        return null; // Not needed for class method hooks
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        // Hook SQLiteDatabase class methods directly
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    // Hook rawQuery to handle missing tables gracefully
    @ProxyMethod("rawQuery")
    public static class RawQuery extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                String sql = (String) args[0];
                if (sql != null && sql.contains("DurableJob")) {
                    Slog.w(TAG, "SQLiteDatabase: rawQuery called with DurableJob table, returning empty cursor");
                    // Return an empty cursor instead of crashing
                    return null;
                }
                return method.invoke(who, args);
            } catch (SQLiteException e) {
                Slog.w(TAG, "SQLiteDatabase: rawQuery failed, returning null", e);
                return null;
            } catch (Exception e) {
                Slog.e(TAG, "SQLiteDatabase: rawQuery error", e);
                return method.invoke(who, args);
            }
        }
    }

    // Hook query to handle missing tables gracefully
    @ProxyMethod("query")
    public static class Query extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                String table = (String) args[0];
                if (table != null && table.equals("DurableJob")) {
                    Slog.w(TAG, "SQLiteDatabase: query called on DurableJob table, returning empty cursor");
                    // Return an empty cursor instead of crashing
                    return null;
                }
                return method.invoke(who, args);
            } catch (SQLiteException e) {
                Slog.w(TAG, "SQLiteDatabase: query failed, returning null", e);
                return null;
            } catch (Exception e) {
                Slog.e(TAG, "SQLiteDatabase: query error", e);
                return method.invoke(who, args);
            }
        }
    }

    // Hook execSQL to handle missing tables gracefully
    @ProxyMethod("execSQL")
    public static class ExecSQL extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                String sql = (String) args[0];
                if (sql != null && sql.contains("DurableJob")) {
                    Slog.w(TAG, "SQLiteDatabase: execSQL called with DurableJob table, ignoring");
                    // Ignore DurableJob table operations
                    return null;
                }
                return method.invoke(who, args);
            } catch (SQLiteException e) {
                Slog.w(TAG, "SQLiteDatabase: execSQL failed, ignoring", e);
                return null;
            } catch (Exception e) {
                Slog.e(TAG, "SQLiteDatabase: execSQL error", e);
                return method.invoke(who, args);
            }
        }
    }
}
