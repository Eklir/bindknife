package com.github.eklir.android.bindknife;

import android.app.Activity;

import com.github.eklir.androidproccessor.bindknife.constants.BindKnifeConstants;

public class BindKnife {

    public static void inject(Activity activity) {
        String canonicalName = activity.getClass().getCanonicalName();
        String bindClassName = canonicalName + BindKnifeConstants.BIND_SUFFIX;
        try {
            Class<?> bindClazz = Class.forName(bindClassName);
            IKnife knife = (IKnife) bindClazz.newInstance();
            knife.inject(activity);
        } catch (ClassNotFoundException e) {
            throw  new RuntimeException(String.format("AutoBind helper class [%s] not found",bindClassName));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

    }
}
