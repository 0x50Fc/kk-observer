package cn.kkmofang.observer;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;

/**
 * Created by zhanghailong on 2016/11/4.
 */

public class ArrayIterator<T extends Object> implements Iterator<T> {

    private final Object _object;
    private int _i;
    private int _size;

    public ArrayIterator(Object object) {
        _object = object;
        if(object != null) {
            if(object instanceof List) {
                _size = ((List) object).size();
            }
            else if(object.getClass().isArray()) {
                _size = Array.getLength(object);
            }
        }
    }

    @Override
    public boolean hasNext() {
        return _i < _size;
    }

    @Override
    public T next() {
        if(_object instanceof List) {
            return ((List<T>) _object).get(_i ++);
        }
        else if(_object.getClass().isArray()) {
            return (T) Array.get(_object,_i ++);
        }
        return null;
    }

    @Override
    public void remove() {
        if(_object instanceof List) {
            ((List<T>) _object).remove(_i);
            _size --;
        }
    }
}
