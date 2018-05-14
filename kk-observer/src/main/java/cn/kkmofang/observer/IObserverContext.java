package cn.kkmofang.observer;

import cn.kkmofang.script.IScriptContext;
import cn.kkmofang.script.IScriptFunction;
import cn.kkmofang.script.IScriptObject;

/**
 * Created by zhanghailong on 2018/3/13.
 */

public interface IObserverContext extends IScriptContext{

    String[][] evaluateKeys(String evaluateCode);

    Object evaluate(String evaluateCode);

    Object execEvaluate(Object func, Object object);

}
