

package top.niunaijun.blackbox.core.system.pm;

import java.util.AbstractSet;
import java.util.Iterator;


public final class FastImmutableArraySet<T> extends AbstractSet<T> {
    FastIterator<T> mIterator;
    T[] mContents;

    public FastImmutableArraySet(T[] contents) {
        mContents = contents;
    }

    @Override
    public Iterator<T> iterator() {
        FastIterator<T> it = mIterator;
        if (it == null) {
            it = new FastIterator<T>(mContents);
            mIterator = it;
        } else {
            it.mIndex = 0;
        }
        return it;
    }

    @Override
    public int size() {
        return mContents.length;
    }

    private static final class FastIterator<T> implements Iterator<T> {
        private final T[] mContents;
        int mIndex;

        public FastIterator(T[] contents) {
            mContents = contents;
        }

        @Override
        public boolean hasNext() {
            return mIndex != mContents.length;
        }

        @Override
        public T next() {
            return mContents[mIndex++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
