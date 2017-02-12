package org.n3r.eql.util;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import ognl.NoSuchPropertyException;
import ognl.Ognl;
import ognl.OgnlException;

import java.util.Map;

@Slf4j
public class Og {
//    public static Object eval(String expr, Map<String, Object> mergeProperties) {
//        return eval(expr, mergeProperties, Maps.<Object, Map<String, Object>>newHashMap());
//    }

    public static Object eval(String expr, Map<String, Object> mergeProperties,
                              Map<Object, Map<String, Object>> cachedProperties) {
        Exception ex = null;
        try {
            val nestedExpr = new NestedExpr(expr);
            if (nestedExpr.isNested()) {
                Object map = evalNested(nestedExpr, mergeProperties, cachedProperties);
                if (map != null) return map;
            }

            return Ognl.getValue(expr, new OgRoot(mergeProperties));
        } catch (NoSuchPropertyException e) { // ignore
        } catch (OgnlException e) {
            if (!e.getMessage().contains("source is null for getProperty"))
                ex = e;
        } catch (Exception e) {
            ex = e;
        }

        if (ex != null) log.warn("error while eval " + expr, ex);
        return null;
    }

    private static Object evalNested(NestedExpr nestedExpr,
                                     Map<String, Object> mergeProperties,
                                     Map<Object, Map<String, Object>> cachedProperties) {
        Object parent = eval(nestedExpr.getParentExpr(), mergeProperties, cachedProperties);
        if (!isNormalBean(parent)) return null;

        val map = parseBeanProperties(cachedProperties, parent);
        if (map.isEmpty()) return null;

        return eval(nestedExpr.getSubExpr(), map, cachedProperties);
    }

    public static boolean isNormalBean(Object parent) {
        return parent != null && !(parent instanceof Map);
    }

    private static Map<String, Object> parseBeanProperties(
            Map<Object, Map<String, Object>> cachedProperties, Object parent) {
        Map<String, Object> map = cachedProperties.get(parent);
        if (map != null) return map;

        map = Maps.newHashMap();
        P.mergeBeanProperties(parent, map);
        cachedProperties.put(parent, map);

        return map;
    }
}
