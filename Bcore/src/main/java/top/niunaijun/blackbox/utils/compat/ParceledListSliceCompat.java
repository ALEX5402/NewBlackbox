package top.niunaijun.blackbox.utils.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import black.android.content.pm.BRParceledListSlice;

public class ParceledListSliceCompat {

	public static boolean isReturnParceledListSlice(Method method) {
		return method != null && method.getReturnType() == BRParceledListSlice.getRealClass();
	}

	public static boolean isParceledListSlice(Object obj) {
		return obj != null && obj.getClass() == BRParceledListSlice.getRealClass();
	}

	public static Object create(List<?> list) {
		Object slice = BRParceledListSlice.get()._new(list);
		if (slice != null) {
			return slice;
		} else {
			slice = BRParceledListSlice.get()._new();
		}
		for (Object item : list) {
			BRParceledListSlice.get(slice).append(item);
		}
		BRParceledListSlice.get(slice).setLastSlice(true);
		return slice;
	}

	public static Object create(List<?> list, Method method) {
		if (method != null && method.getReturnType() != null) {
			if ("android.content.pm.PackageInfoList".equals(method.getReturnType().getName())) {
				try {
					Class<?> pkgInfoListCls = Class.forName("android.content.pm.PackageInfoList");
					Constructor<?> constructor = pkgInfoListCls.getConstructor(List.class);
					return constructor.newInstance(list);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
		return create(list);
	}
}
