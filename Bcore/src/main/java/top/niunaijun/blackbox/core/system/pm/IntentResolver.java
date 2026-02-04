

package top.niunaijun.blackbox.core.system.pm;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LogPrinter;
import android.util.MutableInt;
import android.util.PrintWriterPrinter;
import android.util.Printer;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import top.niunaijun.blackbox.utils.Slog;


public abstract class IntentResolver<F extends BPackage.IntentInfo, R extends Object> {
    final private static String TAG = "IntentResolver";
    final private static boolean DEBUG = false;
    final private static boolean localLOGV = DEBUG || false;
    final private static boolean localVerificationLOGV = DEBUG || false;

    public void addFilter(F f) {
        if (localLOGV) {
            Slog.v(TAG, "Adding filter: " + f);
            f.intentFilter.dump(new LogPrinter(Log.VERBOSE, TAG), "      ");
            Slog.v(TAG, "    Building Lookup Maps:");
        }

        mFilters.add(f);
        int numS = register_intent_filter(f, f.intentFilter.schemesIterator(),
                mSchemeToFilter, "      Scheme: ");
        int numT = register_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            register_intent_filter(f, f.intentFilter.actionsIterator(),
                    mActionToFilter, "      Action: ");
        }
        if (numT != 0) {
            register_intent_filter(f, f.intentFilter.actionsIterator(),
                    mTypedActionToFilter, "      TypedAction: ");
        }
    }

    public static boolean filterEquals(IntentFilter f1, IntentFilter f2) {
        int s1 = f1.countActions();
        int s2 = f2.countActions();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasAction(f1.getAction(i))) {
                return false;
            }
        }
        s1 = f1.countCategories();
        s2 = f2.countCategories();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasCategory(f1.getCategory(i))) {
                return false;
            }
        }
        s1 = f1.countDataSchemes();
        s2 = f2.countDataSchemes();
        if (s1 != s2) {
            return false;
        }
        for (int i=0; i<s1; i++) {
            if (!f2.hasDataScheme(f1.getDataScheme(i))) {
                return false;
            }
        }
        s1 = f1.countDataSchemeSpecificParts();
        s2 = f2.countDataSchemeSpecificParts();
        if (s1 != s2) {
            return false;
        }
        return true;
    }

    private ArrayList<F> collectFilters(F[] array, IntentFilter matching) {
        ArrayList<F> res = null;
        if (array != null) {
            for (int i=0; i<array.length; i++) {
                F cur = array[i];
                if (cur == null) {
                    break;
                }
                if (filterEquals(cur.intentFilter, matching)) {
                    if (res == null) {
                        res = new ArrayList<>();
                    }
                    res.add(cur);
                }
            }
        }
        return res;
    }

    public ArrayList<F> findFilters(IntentFilter matching) {
        if (matching.countDataSchemes() == 1) {
            
            return collectFilters(mSchemeToFilter.get(matching.getDataScheme(0)), matching);
        } else if (matching.countDataTypes() != 0 && matching.countActions() == 1) {
            
            return collectFilters(mTypedActionToFilter.get(matching.getAction(0)), matching);
        } else if (matching.countDataTypes() == 0 && matching.countDataSchemes() == 0
                && matching.countActions() == 1) {
            
            return collectFilters(mActionToFilter.get(matching.getAction(0)), matching);
        } else {
            ArrayList<F> res = null;
            for (F cur : mFilters) {
                if (filterEquals(cur.intentFilter, matching)) {
                    if (res == null) {
                        res = new ArrayList<>();
                    }
                    res.add(cur);
                }
            }
            return res;
        }
    }

    public void removeFilter(F f) {
        removeFilterInternal(f);
        mFilters.remove(f);
    }

    void removeFilterInternal(F f) {
        if (localLOGV) {
            Slog.v(TAG, "Removing filter: " + f);
            f.intentFilter.dump(new LogPrinter(Log.VERBOSE, TAG), "      ");
            Slog.v(TAG, "    Cleaning Lookup Maps:");
        }

        int numS = unregister_intent_filter(f, f.intentFilter.schemesIterator(),
                mSchemeToFilter, "      Scheme: ");
        int numT = unregister_mime_types(f, "      Type: ");
        if (numS == 0 && numT == 0) {
            unregister_intent_filter(f, f.intentFilter.actionsIterator(),
                    mActionToFilter, "      Action: ");
        }
        if (numT != 0) {
            unregister_intent_filter(f, f.intentFilter.actionsIterator(),
                    mTypedActionToFilter, "      TypedAction: ");
        }
    }

    boolean dumpMap(PrintWriter out, String titlePrefix, String title,
            String prefix, ArrayMap<String, F[]> map, String packageName,
            boolean printFilter, boolean collapseDuplicates) {
        final String eprefix = prefix + "  ";
        final String fprefix = prefix + "    ";
        final ArrayMap<Object, MutableInt> found = new ArrayMap<>();
        boolean printedSomething = false;
        Printer printer = null;
        for (int mapi=0; mapi<map.size(); mapi++) {
            F[] a = map.valueAt(mapi);
            final int N = a.length;
            boolean printedHeader = false;
            F filter;
            if (collapseDuplicates && !printFilter) {
                found.clear();
                for (int i=0; i<N && (filter=a[i]) != null; i++) {
                    if (packageName != null && !isPackageForFilter(packageName, filter)) {
                        continue;
                    }
                    Object label = filterToLabel(filter);
                    int index = found.indexOfKey(label);
                    if (index < 0) {
                        found.put(label, new MutableInt(1));
                    } else {
                        found.valueAt(index).value++;
                    }
                }
                for (int i=0; i<found.size(); i++) {
                    if (title != null) {
                        out.print(titlePrefix); out.println(title);
                        title = null;
                    }
                    if (!printedHeader) {
                        out.print(eprefix); out.print(map.keyAt(mapi)); out.println(":");
                        printedHeader = true;
                    }
                    printedSomething = true;
                    dumpFilterLabel(out, fprefix, found.keyAt(i), found.valueAt(i).value);
                }
            } else {
                for (int i=0; i<N && (filter=a[i]) != null; i++) {
                    if (packageName != null && !isPackageForFilter(packageName, filter)) {
                        continue;
                    }
                    if (title != null) {
                        out.print(titlePrefix); out.println(title);
                        title = null;
                    }
                    if (!printedHeader) {
                        out.print(eprefix); out.print(map.keyAt(mapi)); out.println(":");
                        printedHeader = true;
                    }
                    printedSomething = true;
                    dumpFilter(out, fprefix, filter);
                    if (printFilter) {
                        if (printer == null) {
                            printer = new PrintWriterPrinter(out);
                        }
                        filter.intentFilter.dump(printer, fprefix + "  ");
                    }
                }
            }
        }
        return printedSomething;
    }

    private class IteratorWrapper implements Iterator<F> {
        private final Iterator<F> mI;
        private F mCur;

        IteratorWrapper(Iterator<F> it) {
            mI = it;
        }

        public boolean hasNext() {
            return mI.hasNext();
        }

        public F next() {
            return (mCur = mI.next());
        }

        public void remove() {
            if (mCur != null) {
                removeFilterInternal(mCur);
            }
            mI.remove();
        }

    }

    
    public Iterator<F> filterIterator() {
        return new IteratorWrapper(mFilters.iterator());
    }

    
    public Set<F> filterSet() {
        return Collections.unmodifiableSet(mFilters);
    }

    public List<R> queryIntentFromList(Intent intent, String resolvedType, boolean defaultOnly,
            ArrayList<F[]> listCut, int userId) {
        ArrayList<R> resultList = new ArrayList<R>();

        final boolean debug = localLOGV ||
                ((intent.getFlags() & Intent.FLAG_DEBUG_LOG_RESOLUTION) != 0);

        FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
        final String scheme = intent.getScheme();
        int N = listCut.size();
        for (int i = 0; i < N; ++i) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType, scheme,
                    listCut.get(i), resultList, userId);
        }
        filterResults(resultList);

        return resultList;
    }

    public List<R> queryIntent(Intent intent, String resolvedType, boolean defaultOnly,
            int userId) {
        String scheme = intent.getScheme();

        ArrayList<R> finalList = new ArrayList<R>();

        final boolean debug = localLOGV ||
                ((intent.getFlags() & Intent.FLAG_DEBUG_LOG_RESOLUTION) != 0);

        if (debug) Slog.v(
            TAG, "Resolving type=" + resolvedType + " scheme=" + scheme
            + " defaultOnly=" + defaultOnly + " userId=" + userId + " of " + intent);

        F[] firstTypeCut = null;
        F[] secondTypeCut = null;
        F[] thirdTypeCut = null;
        F[] schemeCut = null;

        
        
        if (resolvedType != null) {
            int slashpos = resolvedType.indexOf('/');
            if (slashpos > 0) {
                final String baseType = resolvedType.substring(0, slashpos);
                if (!baseType.equals("*")) {
                    if (resolvedType.length() != slashpos+2
                            || resolvedType.charAt(slashpos+1) != '*') {
                        
                        
                        firstTypeCut = mTypeToFilter.get(resolvedType);
                        if (debug) Slog.v(TAG, "First type cut: " + Arrays.toString(firstTypeCut));
                        secondTypeCut = mWildTypeToFilter.get(baseType);
                        if (debug) Slog.v(TAG, "Second type cut: "
                                + Arrays.toString(secondTypeCut));
                    } else {
                        
                        firstTypeCut = mBaseTypeToFilter.get(baseType);
                        if (debug) Slog.v(TAG, "First type cut: " + Arrays.toString(firstTypeCut));
                        secondTypeCut = mWildTypeToFilter.get(baseType);
                        if (debug) Slog.v(TAG, "Second type cut: "
                                + Arrays.toString(secondTypeCut));
                    }
                    
                    
                    thirdTypeCut = mWildTypeToFilter.get("*");
                    if (debug) Slog.v(TAG, "Third type cut: " + Arrays.toString(thirdTypeCut));
                } else if (intent.getAction() != null) {
                    
                    
                    
                    firstTypeCut = mTypedActionToFilter.get(intent.getAction());
                    if (debug) Slog.v(TAG, "Typed Action list: " + Arrays.toString(firstTypeCut));
                }
            }
        }

        
        
        
        if (scheme != null) {
            schemeCut = mSchemeToFilter.get(scheme);
            if (debug) Slog.v(TAG, "Scheme list: " + Arrays.toString(schemeCut));
        }

        
        
        
        if (resolvedType == null && scheme == null && intent.getAction() != null) {
            firstTypeCut = mActionToFilter.get(intent.getAction());
            if (debug) Slog.v(TAG, "Action list: " + Arrays.toString(firstTypeCut));
        }

        FastImmutableArraySet<String> categories = getFastIntentCategories(intent);
        if (firstTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType,
                    scheme, firstTypeCut, finalList, userId);
        }
        if (secondTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType,
                    scheme, secondTypeCut, finalList, userId);
        }
        if (thirdTypeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType,
                    scheme, thirdTypeCut, finalList, userId);
        }
        if (schemeCut != null) {
            buildResolveList(intent, categories, debug, defaultOnly, resolvedType,
                    scheme, schemeCut, finalList, userId);
        }
        filterResults(finalList);


        if (debug) {
            Slog.v(TAG, "Final result list:");
            for (int i=0; i<finalList.size(); i++) {
                Slog.v(TAG, "  " + finalList.get(i));
            }
        }
        return finalList;
    }

    
    protected boolean allowFilterResult(F filter, List<R> dest) {
        return true;
    }

    
    protected boolean isFilterStopped(F filter, int userId) {
        return false;
    }

    
    protected abstract boolean isPackageForFilter(String packageName, F filter);

    protected abstract F[] newArray(int size);

    @SuppressWarnings("unchecked")
    protected R newResult(F filter, int match, int userId) {
        return (R)filter;
    }

    @SuppressWarnings("unchecked")
    protected void sortResults(List<R> results) {
        Collections.sort(results, mResolvePrioritySorter);
    }

    
    protected void filterResults(List<R> results) {
    }

    protected void dumpFilter(PrintWriter out, String prefix, F filter) {
        out.print(prefix); out.println(filter);
    }

    protected Object filterToLabel(F filter) {
        return "IntentFilter";
    }

    protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {
        out.print(prefix); out.print(label); out.print(": "); out.println(count);
    }

    private final void addFilter(ArrayMap<String, F[]> map, String name, F filter) {
        F[] array = map.get(name);
        if (array == null) {
            array = newArray(2);
            map.put(name,  array);
            array[0] = filter;
        } else {
            final int N = array.length;
            int i = N;
            while (i > 0 && array[i-1] == null) {
                i--;
            }
            if (i < N) {
                array[i] = filter;
            } else {
                F[] newa = newArray((N*3)/2);
                System.arraycopy(array, 0, newa, 0, N);
                newa[N] = filter;
                map.put(name, newa);
            }
        }
    }

    private final int register_mime_types(F filter, String prefix) {
        final Iterator<String> i = filter.intentFilter.typesIterator();
        if (i == null) {
            return 0;
        }

        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            if (localLOGV) Slog.v(TAG, prefix + name);
            String baseName = name;
            final int slashpos = name.indexOf('/');
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                name = name + "/*";
            }

            addFilter(mTypeToFilter, name, filter);

            if (slashpos > 0) {
                addFilter(mBaseTypeToFilter, baseName, filter);
            } else {
                addFilter(mWildTypeToFilter, baseName, filter);
            }
        }

        return num;
    }

    private final int unregister_mime_types(F filter, String prefix) {
        final Iterator<String> i = filter.intentFilter.typesIterator();
        if (i == null) {
            return 0;
        }

        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            if (localLOGV) Slog.v(TAG, prefix + name);
            String baseName = name;
            final int slashpos = name.indexOf('/');
            if (slashpos > 0) {
                baseName = name.substring(0, slashpos).intern();
            } else {
                name = name + "/*";
            }

            remove_all_objects(mTypeToFilter, name, filter);

            if (slashpos > 0) {
                remove_all_objects(mBaseTypeToFilter, baseName, filter);
            } else {
                remove_all_objects(mWildTypeToFilter, baseName, filter);
            }
        }
        return num;
    }

    private final int register_intent_filter(F filter, Iterator<String> i,
            ArrayMap<String, F[]> dest, String prefix) {
        if (i == null) {
            return 0;
        }

        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            if (localLOGV) Slog.v(TAG, prefix + name);
            addFilter(dest, name, filter);
        }
        return num;
    }

    private final int unregister_intent_filter(F filter, Iterator<String> i,
            ArrayMap<String, F[]> dest, String prefix) {
        if (i == null) {
            return 0;
        }

        int num = 0;
        while (i.hasNext()) {
            String name = i.next();
            num++;
            if (localLOGV) Slog.v(TAG, prefix + name);
            remove_all_objects(dest, name, filter);
        }
        return num;
    }

    private final void remove_all_objects(ArrayMap<String, F[]> map, String name,
            Object object) {
        F[] array = map.get(name);
        if (array != null) {
            int LAST = array.length-1;
            while (LAST >= 0 && array[LAST] == null) {
                LAST--;
            }
            for (int idx=LAST; idx>=0; idx--) {
                if (array[idx] == object) {
                    final int remain = LAST - idx;
                    if (remain > 0) {
                        System.arraycopy(array, idx+1, array, idx, remain);
                    }
                    array[LAST] = null;
                    LAST--;
                }
            }
            if (LAST < 0) {
                map.remove(name);
            } else if (LAST < (array.length/2)) {
                F[] newa = newArray(LAST+2);
                System.arraycopy(array, 0, newa, 0, LAST+1);
                map.put(name, newa);
            }
        }
    }

    private static FastImmutableArraySet<String> getFastIntentCategories(Intent intent) {
        final Set<String> categories = intent.getCategories();
        if (categories == null) {
            return null;
        }
        return new FastImmutableArraySet<String>(categories.toArray(new String[categories.size()]));
    }

    private void buildResolveList(Intent intent, FastImmutableArraySet<String> categories,
            boolean debug, boolean defaultOnly, String resolvedType, String scheme,
            F[] src, List<R> dest, int userId) {
        final String action = intent.getAction();
        final Uri data = intent.getData();
        final String packageName = intent.getPackage();



        final int N = src != null ? src.length : 0;
        boolean hasNonDefaults = false;
        int i;
        F filter;
        for (i=0; i<N && (filter=src[i]) != null; i++) {
            int match;
            if (debug) Slog.v(TAG, "Matching against filter " + filter);








            
            if (packageName != null && !isPackageForFilter(packageName, filter)) {
                if (debug) {
                    Slog.v(TAG, "  Filter is not from package " + packageName + "; skipping");
                }
                continue;
            }

            
            if (!allowFilterResult(filter, dest)) {
                if (debug) {
                    Slog.v(TAG, "  Filter's target already added");
                }
                continue;
            }

            match = filter.intentFilter.match(action, resolvedType, scheme, data, categories, TAG);
            if (match >= 0) {
                if (debug) Slog.v(TAG, "  Filter matched!  match=0x" +
                        Integer.toHexString(match) + " hasDefault="
                        + filter.intentFilter.hasCategory(Intent.CATEGORY_DEFAULT));
                if (!defaultOnly || filter.intentFilter.hasCategory(Intent.CATEGORY_DEFAULT)) {
                    final R oneResult = newResult(filter, match, userId);
                    if (debug) Slog.v(TAG, "    Created result: " + oneResult);
                    if (oneResult != null) {
                        dest.add(oneResult);
                    }
                } else {
                    hasNonDefaults = true;
                }
            } else {
                if (debug) {
                    String reason;
                    switch (match) {
                        case IntentFilter.NO_MATCH_ACTION: reason = "action"; break;
                        case IntentFilter.NO_MATCH_CATEGORY: reason = "category"; break;
                        case IntentFilter.NO_MATCH_DATA: reason = "data"; break;
                        case IntentFilter.NO_MATCH_TYPE: reason = "type"; break;
                        default: reason = "unknown reason"; break;
                    }
                    Slog.v(TAG, "  Filter did not match: " + reason);
                }
            }
        }

        if (debug && hasNonDefaults) {
            if (dest.size() == 0) {
                Slog.v(TAG, "resolveIntent failed: found match, but none with CATEGORY_DEFAULT");
            } else if (dest.size() > 1) {
                Slog.v(TAG, "resolveIntent: multiple matches, only some with CATEGORY_DEFAULT");
            }
        }
    }

    
    @SuppressWarnings("rawtypes")
    private static final Comparator mResolvePrioritySorter = new Comparator() {
        public int compare(Object o1, Object o2) {
            final int q1 = ((IntentFilter) o1).getPriority();
            final int q2 = ((IntentFilter) o2).getPriority();
            return (q1 > q2) ? -1 : ((q1 < q2) ? 1 : 0);
        }
    };

    
    private final HashSet<F> mFilters = new HashSet<>();

    
    private final ArrayMap<String, F[]> mTypeToFilter = new ArrayMap<String, F[]>();

    
    private final ArrayMap<String, F[]> mBaseTypeToFilter = new ArrayMap<String, F[]>();

    
    private final ArrayMap<String, F[]> mWildTypeToFilter = new ArrayMap<String, F[]>();

    
    private final ArrayMap<String, F[]> mSchemeToFilter = new ArrayMap<String, F[]>();

    
    private final ArrayMap<String, F[]> mActionToFilter = new ArrayMap<String, F[]>();

    
    private final ArrayMap<String, F[]> mTypedActionToFilter = new ArrayMap<String, F[]>();
}
