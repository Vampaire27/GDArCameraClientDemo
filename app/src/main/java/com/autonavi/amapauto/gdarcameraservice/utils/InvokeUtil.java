package com.autonavi.amapauto.gdarcameraservice.utils;

import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public final class InvokeUtil {
    private static final String TAG = InvokeUtil.class.getSimpleName();

    private final static int METHOD_MATCH_NONE = 0;
    private final static int METHOD_MATCH_PUBLIC = 0x01;
    private final static int METHOD_MATCH_PARAMS_TYPE = 0x02;
    private final static int METHOD_MATCH_STRICTLY = METHOD_MATCH_PUBLIC | METHOD_MATCH_PARAMS_TYPE;

    private final static int INSTANCE_DENIED = 0;
    private final static int INSTANCE_OK = 1;
    private final static int INSTANCE_CONV = 2;

    public static <T> T newInstanceOrThrow(Class<? extends T> clz, Object...params) throws IllegalAccessException,
            InvocationTargetException, InstantiationException {
        Log.d(TAG, "newInstanceOrThrow()");
        Constructor[] constructors = clz.getDeclaredConstructors();
        if ((constructors == null) || (constructors.length == 0)) {
            throw new IllegalArgumentException("Can't get even one available constructor for " + clz);
        }
        Class[] paramClasses = new Class[params.length];
        Constructor found = null;
        for (Constructor constructor : constructors) {
            Class[] arrayOfClass = constructor.getParameterTypes();
            if (arrayOfClass.length != params.length) {
                continue;
            }
            if (params.length == 0) {
                found = constructor;
                break;
            }
            boolean matched = true;
            for (int i = 0; i < params.length; i++) {
                int v = instanceOf(params[0], arrayOfClass[i]);
                if (v == INSTANCE_DENIED) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                found = constructor;
                break;
            }
        }
        if (found != null) {
            found.setAccessible(true);
            return (T) found.newInstance(params);
        }
        throw new NoSuchElementException("no Constructor match it!!");
    }

    public static Object invokeMethod(Object obj, String methodName, Object...params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Log.d(TAG, "invokeMethod()");
        Method method = matchMethod(obj.getClass(), methodName, params);
        if (method == null) {
            throw new NoSuchMethodException("class " + obj.getClass().getCanonicalName() +
                " cannot find method " + methodName);
        }
        return method.invoke(obj, params);
    }

    public static Object invokeStaticMethod(Class clz, String methodName, Object...params)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Log.d(TAG, "invokeStaticMethod()");
        Method method = matchMethod(clz, methodName, params);
        if (method == null) {
            throw new NoSuchMethodException("class " + clz.getCanonicalName() +
                " cannot find method " + methodName);
        }
        return method.invoke(null, params);
    }

    public static Class wrappedClass(Class clz) {
        Log.d(TAG, "wrappedClass()");
        try {
            return ((Class) clz.getField("TYPE").get(null));
        } catch (Exception e) {
            return null;
        }
    }

    public static Method[] methodsForName(Class clz, String name) {
        Log.d(TAG, "methodsForName()");
        Method[] methods = clz.getDeclaredMethods();
        if (methods == null || methods.length == 0) {
            return null;
        }
        List<Method> out = new ArrayList<>();
        for (Method method : methods) {
            if (method.getName().equals(name)) {
                out.add(method);
            }
        }
        if (out.size() == 0) {
            return null;
        }
        return out.toArray(new Method[0]);
    }

    public static Method matchMethod(Class clz, String name, Object...params) {
        Log.d(TAG, "matchMethod()");
        Method[] methods = methodsForName(clz, name);
        if (methods == null || methods.length == 0) {
            return null;
        }
        Method found = null;
        int maxMatch = 0;
        for (Method method : methods) {
            int v = matchMethodParameterTypes(method, params);
            if ( v > maxMatch) {
                maxMatch = v;
                found = method;
            }
        }
        if (maxMatch == METHOD_MATCH_NONE) {
            return null;
        }
        if ((maxMatch & METHOD_MATCH_PUBLIC) == 0 ) {
            found.setAccessible(true);
        }
        return found;
    }

    private static int instanceOf(Object obj, Class<?> clz) {
        Log.d(TAG, "instanceOf()");
        if ( obj == null ) {
            // 基本类型不允许null对象
            if (clz.isPrimitive()) {
                return INSTANCE_DENIED;
            }
            // 空对象可匹配任何对象类型
            return INSTANCE_OK;
        }
        if (clz.isPrimitive()) {
            if (clz == void.class) {
                return INSTANCE_DENIED;
            }
            Class wclz = wrappedClass(obj.getClass());
            // 非封装类型对象
            if (wclz == null) {
                return INSTANCE_DENIED;
            }
            // 基本类型与封装类型完全匹配
            if (wclz == clz) {
                return INSTANCE_OK;
            }
            // 基本类型与封装类型完全不匹配
            if (clz == long.class && wclz == int.class) {
                return INSTANCE_CONV;
            }
            if (clz == double.class && (wclz == float.class || wclz == long.class || wclz == int.class)) {
                return INSTANCE_CONV;
            }
            if (clz == float.class && wclz == int.class) {
                return INSTANCE_CONV;
            }
            if (clz == int.class && (wclz == byte.class || wclz == short.class || wclz == char.class)) {
                return INSTANCE_CONV;
            }
            return INSTANCE_DENIED;
        }
        return clz.isInstance(obj) ? INSTANCE_OK : INSTANCE_DENIED;
    }

    private static <T> int arrayLength(T[] array) {
        Log.d(TAG, "arrayLength()");
        return (array == null ? 0 : array.length);
    }

    private static int matchMethodParameterTypes(Method method, Object...params) {
        Log.d(TAG, "matchMethodParameterTypes()");
        Class[] types = method.getParameterTypes();
        int tlen = arrayLength(types);
        int plen = arrayLength(params);
        int value = METHOD_MATCH_NONE;
        if (tlen != plen) {
            return METHOD_MATCH_NONE;
        }
        if (plen > 0) {
            int[] pos = new int[plen];
            int size = 0;
            for (int i= 0; i< plen; i++) {
                Object p = params[i];
                int v = instanceOf(p, types[i]);
                if (v == INSTANCE_DENIED) {
                    return METHOD_MATCH_NONE;
                } else if (v == INSTANCE_OK) {
                    continue;
                } else {
                    pos[size++] = i;
                }
            }
            if (size > 0) {
                for (int index : pos) {
                    Object p = params[index];
                    if (p instanceof Number) {
                        Number n = (Number) p;
                        if (types[index] == int.class) {
                            params[index] = n.intValue();
                        }
                        else if (types[index] == long.class) {
                            params[index] = n.longValue();
                        }
                        else if (types[index] == double.class) {
                            params[index] = n.doubleValue();
                        }
                        else if (types[index] == float.class) {
                            params[index] = n.floatValue();
                        }
                        else if (types[index] == byte.class) {
                            params[index] = n.byteValue();
                        }
                        else if (types[index] == short.class) {
                            params[index] = n.shortValue();
                        }
                    } else if (p instanceof Character) {
                        char c = (Character)p;
                        if (types[index] == int.class) {
                            params[index] = (int)c;
                        }
                        else if (types[index] == long.class) {
                            params[index] = (long)c;
                        }

                        else if (types[index] == byte.class) {
                            params[index] = (byte)c;
                        }
                        else if (types[index] == short.class) {
                            params[index] = (short)c;
                        }
                    }
                }
            }
        }
        value |= METHOD_MATCH_PARAMS_TYPE;
        if (Modifier.isPublic(method.getModifiers())) {
            value |= METHOD_MATCH_PUBLIC;
        }
        return value;
    }

    public static void setValueOfField(Object obj, String fieldName, Object value) throws NoSuchFieldException,
            IllegalAccessException {
        Log.d(TAG, "setValueOfField()");
        if (TextUtils.isEmpty(fieldName)) {
            throw new IllegalArgumentException("param fieldName is empty");
        }
        Class clz = obj.getClass();
        Field field = fieldByNameRecursive(clz, fieldName);
        if (!Modifier.isPublic (field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
            field.setAccessible(true);
        }
        field.set(obj, value);
    }

    private static Field fieldByNameRecursive(Class clz, String fieldName) throws NoSuchFieldException {
        Log.d(TAG, "fieldByNameRecursive()");
        Class target = clz;
        while (!target.equals(Object.class)) {
            try {
                return target.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                target = clz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
