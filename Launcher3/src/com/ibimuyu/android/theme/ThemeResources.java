package com.ibimuyu.android.theme;

import java.lang.ref.WeakReference;
import java.util.Locale;
import com.ibimuyu.android.util.LongSparseArray;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;


public class ThemeResources extends Resources {
	private Resources mSourceResources = null;
	final TypedValue mTmpValueE = new TypedValue();
	final Configuration mTmpConfigE = new Configuration();
    private final LongSparseArray<WeakReference<Drawable.ConstantState> > mDrawableCacheE
    = new LongSparseArray<WeakReference<Drawable.ConstantState> >();
    boolean mInitEnd = false;
	public ThemeResources(Resources resources) {
		super(resources.getAssets(),resources.getDisplayMetrics(),resources.getConfiguration());
		mSourceResources = resources;
		mInitEnd = true;
	}
	public Drawable loadDrawable(TypedValue value, int id)
            throws NotFoundException {
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
        	//ColorDrawable���ⲻ�ӹ�//
        	return ResourcesProxy.loadDrawable(mSourceResources, value, id);
        }
        if (value.string == null) {
        	throw new NotFoundException("Resource is not a Drawable (color or path): " + value);
        }
        String file = value.string.toString();
        if (file.endsWith(".xml")) {
        	//XML���ⲻ�ӹ�//
        	return ResourcesProxy.loadDrawable(mSourceResources, value, id);
        }
        
        final long key = (((long) value.assetCookie) << 32) | value.data;
        Drawable dr = getCachedDrawable(mDrawableCacheE, key);
        if (dr != null) {
        	//ȡ������,��ֱ�ӷ���//
            return dr;
        }
        
        dr = loadThemeDrawable(value,id);
        
        if (dr != null) {
            dr.setChangingConfigurations(value.changingConfigurations);
            Drawable.ConstantState cs = dr.getConstantState();
            if (cs != null) {
                synchronized (mTmpValueE) {
                	mDrawableCacheE.put(key, new WeakReference<Drawable.ConstantState>(cs));
                }
            }
        } else {
        	//����û�нӹ�//
        	return ResourcesProxy.loadDrawable(mSourceResources, value, id);
        }

        return dr;
	}
	
    private Drawable getCachedDrawable(
            LongSparseArray<WeakReference<Drawable.ConstantState>> drawableCache,
            long key) {
        synchronized (mTmpValueE) {
            WeakReference<Drawable.ConstantState> wr = drawableCache.get(key);
            if (wr != null) {
                Drawable.ConstantState entry = wr.get();
                if (entry != null) {
                    return entry.newDrawable(this);
                }
                else {
                    drawableCache.delete(key);
                }
            }
        }
        return null;
    }
    
	/**
	 * ���ﲻ�ò����˸�����,���ص��������
	 * updateConfiguration(Configuration config,
     *      DisplayMetrics metrics, CompatibilityInfo compat),
     * ���ǵ����������.
	 */
    @Override
	public void updateConfiguration(Configuration config,
            DisplayMetrics metrics) {
		super.updateConfiguration(config,metrics);
		//�˷������ڻ���Ĺ��캯���е���,��ֹ�鷳,����δ���,û�б�Ҫ����//
		if(!mInitEnd) {
			return;
		}
		mSourceResources.updateConfiguration(config, metrics);
        synchronized (mTmpValueE) {
            int configChanges = 0xfffffff;
            if (config != null) {
                mTmpConfigE.setTo(config);
                if (mTmpConfigE.locale == null) {
                    mTmpConfigE.locale = Locale.getDefault();
                }
                configChanges = getConfiguration().updateFrom(mTmpConfigE);
            }
            clearDrawableCache(mDrawableCacheE, configChanges);
        }
	}
    
    private void clearDrawableCache(
            LongSparseArray<WeakReference<Drawable.ConstantState>> cache,
            int configChanges) {
        int N = cache.size();
        for (int i=0; i<N; i++) {
            WeakReference<Drawable.ConstantState> ref = cache.valueAt(i);
            if (ref != null) {
                Drawable.ConstantState cs = ref.get();
                if (cs != null) {
                    if (Configuration.needNewResources(
                            configChanges, cs.getChangingConfigurations())) {
                        cache.setValueAt(i, null);
                    }
                }
            }
        }
    }

    
    private Drawable loadThemeDrawable(TypedValue value, int id) {
    	return null;
    }
}
