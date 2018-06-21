package cn.kkmofang.observer;

import java.lang.*;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import cn.kkmofang.script.IScriptFunction;
import cn.kkmofang.script.ScriptContext;

/**
 * Created by zhanghailong on 16/7/13.
 */
public class Observer implements IObserver {

    public final static int PRIORITY_ASC = -1;
    public final static int PRIORITY_LOW = Integer.MIN_VALUE;
    public final static int PRIORITY_NORMAL = 0;
    public final static int PRIORITY_HIGH = Integer.MAX_VALUE;
    public final static int PRIORITY_DESC = 1;

    private KeyObserver _keyObserver = new KeyObserver();
    private final WeakReference<IObserverContext> _context;

    protected IObserver _parent;
    protected java.lang.Object _object;


    public Observer(IObserverContext context) {
        _context = new WeakReference<>(context);
    }

    public java.lang.Object valueOf() {
        if(_object == null) {
            _object = new TreeMap<String,java.lang.Object>();
        }
        return _object;
    }

    public void setValue(java.lang.Object object) {
        _object = object;
        change(new String[]{});
    }

    @Override
    public IObserverContext context() {
        return _context.get();
    }

    @Override
    public IObserver parent() {
        return _parent;
    }

    protected void finalize() throws Throwable {
        if(_parent != null) {
            _parent.off(new String[]{},null,this);
        }
        super.finalize();
    }

    @Override
    public void setParent(IObserver parent) {

        if(_parent != parent) {

            if(_parent != null) {
                _parent.off(new String[]{},null,this);
            }

            _parent = parent;

            if(_parent != null) {

                _parent.on(new String[]{},new Listener<Observer>(){

                    @Override
                    public void onChanged(IObserver observer, String[] changedKeys,Object value, Observer weakObject) {
                        if(weakObject != null) {
                            weakObject.set(changedKeys,value);
                        }
                    }

                },this,PRIORITY_HIGH,true);

                change(new String[]{});

            }
        }


    }

    protected Object get(Object object,String[] keys,int index) {

        if(object == null) {
            return null;
        }

        if(index < keys.length) {

            Object v = ScriptContext.get(object,keys[index]);

            return get(v,keys,index + 1);

        } else {
            return object;
        }
    }

    @Override
    public Object get(String[] keys) {

        if(keys == null ) {
            return valueOf();
        }

        return get(valueOf(),keys,0);
    }

    protected void set(Object object,String[] keys,int index,Object value) {

        if(index + 1 < keys.length) {

            String key = keys[index];

            Object v = ScriptContext.get(object, key);

            if (v == null) {
                v = new TreeMap<String, Object>();
                ScriptContext.set(object, key, v);
            }

            set(v, keys, index + 1, value);

        } else if(index < keys.length) {
            ScriptContext.set(object,keys[index],value);
        }
    }

    @Override
    public void set(String[] keys, Object value) {
        if(keys == null || keys.length == 0) {
            _object = value;
            change(new String[]{});
        } else {
            set(valueOf(),keys,0,value);
            change(keys);
        }
    }

    @Override
    public IObserver change(String[] keys) {

        Set<KeyListener<?>> cbs = new HashSet<>();

        if(keys == null) {
            keys = new String[]{};
        }

        _keyObserver.changedKeys(keys,0,cbs);

        List<KeyListener<?>> vs = new ArrayList<>(cbs);

        Collections.sort(vs, new Comparator<KeyListener<?>>() {
            @Override
            public int compare(KeyListener<?> o1, KeyListener<?> o2) {
                return Integer.valueOf(o1.priority).compareTo(Integer.valueOf(o2.priority));
            }
        });

        IObserverContext ctx = _context.get();

        for(KeyListener<?> cb : vs) {
            KeyListener<Object> v = (KeyListener<Object>) cb;
            if(cb.func != null && ctx != null) {
                Object vv = ctx.execEvaluate(cb.func, this);
                v.listener.onChanged(this, keys, vv, v.weakObject());
            } else if(cb.keys != null) {
                v.listener.onChanged(this, keys,get(cb.keys), v.weakObject());
            } else {
                v.listener.onChanged(this, keys,get(keys), v.weakObject());
            }
        }

        return this;
    }

    @Override
    public <T extends java.lang.Object> IObserver on(String[] keys,Listener<T> listener,T weakObject, int priority, boolean children) {

        KeyListener<T> keyListener = new KeyListener<>(listener,weakObject,priority,children);

        if(keys == null) {
            keys = new String[]{};
        }

        if(!children) {
            keyListener.keys = keys;
        }

        _keyObserver.add(keys,0,keyListener);

        return this;
    }

    @Override
    public <T extends java.lang.Object> IObserver off(String[] keys,Listener<T> listener,T weakObject) {

        if(keys == null) {
            keys = new String[]{};
        }

        _keyObserver.remove(keys,0,listener,weakObject);

        return this;
    }

    @Override
    public <T> IObserver on(String evaluateCode, Listener<T> listener, T weakObject, int priority) {

        IObserverContext ctx = _context.get();

        if(ctx != null) {

            Object func =  ctx.evaluate(evaluateCode);

            if(func != null) {

                KeyListener<T> keyListener = new KeyListener<>(listener,weakObject,priority,false);

                keyListener.func = func;

                for(String[] keys : ctx.evaluateKeys(evaluateCode)) {
                    _keyObserver.add(keys,0,keyListener);
                }
            }

        }

        return this;
    }

    @Override
    public String[] keys() {

        String[] keys = ScriptContext.keys(valueOf());

        if(_parent != null) {

            Set<String> keySet = new TreeSet<>();

            for(String key : _parent.keys()) {
                keySet.add(key);
            }

            for(String key : keys) {
                keySet.add(key);
            }

            return keySet.toArray(new String[keySet.size()]);
        }

        return keys;
    }

    @Override
    public Object get(String key) {
        Object v = ScriptContext.get(valueOf(),key);
        if(v == null && _parent != null) {
            v = _parent.get(key);
        }
        return v;
    }

    @Override
    public void set(String key, Object value) {
        ScriptContext.set(valueOf(),key,value);
    }

    private static class KeyListener<T extends java.lang.Object> {

        public final Listener<T> listener;
        public final int priority;
        public final boolean children;
        public Object func;
        public String[] keys;

        private final WeakReference<T> _weakObject;

        public KeyListener(Listener<T> listener,T weakObject,int priority,boolean children) {
            this.listener = listener;
            _weakObject = new WeakReference<T>(weakObject);
            this.priority = priority;
            this.children = children;
        }

        public T weakObject() {
            return _weakObject.get();
        }
    }

    private static class KeyObserver {

        private final List<KeyListener<?>> _listeners = new LinkedList<>();
        private final Map<String,KeyObserver> _observers = new TreeMap<String,KeyObserver>();

        public void on(KeyListener listener) {
            _listeners.add(listener);
        }

        public <T extends java.lang.Object> void add(String[] keys, int index, KeyListener<T> cb) {

            if(index < keys.length) {
                String key = keys[index];
                KeyObserver v;
                if(!_observers.containsKey(key)) {
                    v = new KeyObserver();
                    _observers.put(key,v);
                } else{
                    v = _observers.get(key);
                }
                v.add(keys,index + 1,cb);
            } else {
                _listeners.add(cb);
            }
        }

        public <T extends java.lang.Object> void remove(String[] keys, int index,Listener<T> listener, T weakObject) {

            if(index < keys.length) {
                String key = keys[index];
                if(listener == null && weakObject == null) {
                    _observers.clear();
                } else if(_observers.containsKey(key)) {
                    KeyObserver v = _observers.get(key);
                    v.remove(keys,index + 1,listener,weakObject);
                }
            } else {
                int i = 0;
                while(i < _listeners.size()) {
                    KeyListener<?> cb = _listeners.get(i);

                    if((listener == null || cb.listener == listener) && ( weakObject == null || weakObject == cb.weakObject())) {
                        _listeners.remove(i);
                    } else {
                        i ++;
                    }
                }
                for(String key : _observers.keySet()) {
                    KeyObserver v = _observers.get(key);
                    v.remove(keys,index,listener,weakObject);
                }
            }

        }

        public void changedKeys(String[] keys, int index,Set<KeyListener<?>> cbs) {

            if(index < keys.length) {
                String key = keys[index];

                if(_observers.containsKey(key)) {
                    KeyObserver v = _observers.get(key);
                    v.changedKeys(keys,index + 1,cbs);
                }

                for(KeyListener<?> cb : _listeners) {
                    if(cb.children) {
                        cbs.add(cb);
                    }
                }
            } else {
                cbs.addAll(_listeners);
                for(String key : _observers.keySet()) {
                    KeyObserver v = _observers.get(key);
                    v.changedKeys(keys,index,cbs);
                }
            }

        }


    }

}

