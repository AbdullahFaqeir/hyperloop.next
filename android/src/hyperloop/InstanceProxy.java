/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package hyperloop;

import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.content.Context;
import android.view.View;

/**
 * This is a proxy that wraps a Java object.
 *
 * @author cwilliams
 */
@Kroll.proxy(parentModule = HyperloopModule.class)
public class InstanceProxy extends BaseProxy {

    /**
     * The underlying Java object we're wrapping.
     */
    private Object nativeObject;

    /**
     * The JS object containing overriding method implementations.
     */
    // TODO Support overrides on standard InstanceProxy instances (not dynamic
    // subclasses or instances of interfaces)?
    private Map<String, Object> overrides;

    /**
     * @param clazz The actual class of the wrapped object. This can differ from
     *            the type we declare ourselves as to JS. This may be a pointer
     *            to a dynamically generated class that subclasses the type we
     *            expose ourselves as. Or it could be some subclass of a parent
     *            type we're exposing ourselves as (when we get a more specific
     *            subclass as an arg to a method call but the method signature
     *            only declares the parent. (i.e. View.onDraw says it takes a
     *            Canvas but we may get a subclass such as DisplayListCanvas.
     *            This will point to the DisplayListCanvas.class, which the
     *            className and apiName we use will be Canvas.class)
     * @param className The fully qualified name of the class we externally
     *            expose ourselves as in JS.
     * @param nativeObject the underlying native object we're wrapping.
     */
    protected InstanceProxy(Class<?> clazz, String className, Object nativeObject) {
        super(clazz, className);
        this.nativeObject = nativeObject;
    }

    @Override
    public Object getWrappedObject() {
        return this.nativeObject;
    }

    @Override
    public Object getReceiver() {
        // call methods/fields on the object we're wrapping.
        return this.nativeObject;
    }

    @Override
    public TiUIView createView(Activity activity) {
        if (this.nativeObject != null && this.nativeObject instanceof View) {
            return new HyperloopView((View) this.nativeObject, this);
        }
        return null;
    }

    /**
     * Attempts to grab the activity for this proxy by looking at the native
     * object we hold. if it's an activity, return it. If it's a subclass of
     * view, get the Context object and return if it can be cast to an Activity.
     */
    @Override
    public Activity getActivity() {
        if (this.nativeObject instanceof Activity) {
            return (Activity) this.nativeObject;
        }
        // If the native object has an accessor to the activity, we should
        // return that...
        if (this.nativeObject instanceof View) {
            View v = (View) this.nativeObject;
            Context c = v.getContext();
            if (c instanceof Activity) {
                return (Activity) c;
            }
        }
        return super.getActivity();
    }

    @Kroll.getProperty
    public boolean getIsInstanceProxy() {
        return true;
    }

    // FIXME I'd love to make this just Map<String, Object>, but native kroll
    // layer can't
    // handle that
    @Kroll.method
    public void setOverrides(KrollDict overrides) {
        this.overrides = overrides;
    }

    Map<String, Object> getOverrides() {
        return this.overrides;
    }

    /**
     * Casts this instance to another type. This will change the type it reports
     * itself as to JS if the cast is safe.
     *
     * @param javaClass
     * @return
     */
    public InstanceProxy cast(Class<?> javaClass) {
        // TODO Should we modify this existing instance or return a new one
        // entirely?
        try {
            this.nativeObject = javaClass.cast(nativeObject);
            this.clazz = javaClass; // Should we actually modify the class
                                    // object too?
            this.className = javaClass.getName();
            return this;
        } catch (ClassCastException e) {
            Log.e(TAG, "Cannot cast to: " + javaClass.getName());
        }
        return this;
    }

    @Override
    public void release() {
        ((HyperloopModule) getCreatedInModule()).getProxyFactory().release(this);
        super.release();
    }
}
