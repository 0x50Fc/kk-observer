package cn.kkmofang.observer;

import cn.kkmofang.script.IScriptContext;
import cn.kkmofang.script.IScriptObject;

/**
 * Created by zhanghailong on 16/7/26.
 */
public interface IObserver extends IScriptObject {

    public IObserverContext context();

    public IObserver parent();

    public void setParent(IObserver observer);

    public Object get(String[] keys);

    public void set(String[] keys,Object value);

    public IObserver change(String[] keys);

    public <T extends java.lang.Object> IObserver on(String[] keys, Listener<T> listener, T weakObject, int priority, boolean children);

    public <T extends java.lang.Object> IObserver off(String[] keys, Listener<T> listener, T weakObject);

    public <T extends java.lang.Object> IObserver on(String evaluateCode, Listener<T> listener, T weakObject, int priority);
}
