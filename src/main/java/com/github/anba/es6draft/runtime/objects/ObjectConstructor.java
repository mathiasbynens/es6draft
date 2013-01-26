/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.FromPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.PropertyDescriptor.ToPropertyDescriptor;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.2 Object Objects</h2>
 * <ul>
 * <li>15.2.1 The Object Constructor Called as a Function
 * <li>15.2.2 The Object Constructor
 * <li>15.2.3 Properties of the Object Constructor
 * </ul>
 */
public class ObjectConstructor extends OrdinaryObject implements Scriptable, Callable, Constructor,
        Initialisable {
    public ObjectConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        return "function Object() { /* native code */ }";
    }

    /**
     * 15.2.1.1 Object ( [ value ] )
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        Object value = args.length > 0 ? args[0] : UNDEFINED;
        if (Type.isUndefinedOrNull(value)) {
            return ObjectCreate(realm());
        }
        return ToObject(realm(), value);
    }

    /**
     * 15.2.2.1 new Object ( [ value ] )
     */
    @Override
    public Object construct(Object... args) {
        if (args.length > 0) {
            Object value = args[0];
            switch (Type.of(value)) {
            case Object:
                return value;
            case String:
            case Boolean:
            case Number:
                return ToObject(realm(), value);
            case Null:
            case Undefined:
            default:
                break;
            }
        }
        return ObjectCreate(realm());
    }

    /**
     * 15.2.3 Properties of the Object Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        /**
         * 15.2.3.1 Object.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ObjectPrototype;

        /**
         * 15.2.3.2 Object.getPrototypeOf ( O )
         */
        @Function(name = "getPrototypeOf", arity = 1)
        public static Object getPrototypeOf(Realm realm, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            Scriptable proto = Type.objectValue(o).getPrototype();
            if (proto != null) {
                return proto;
            }
            return NULL;
        }

        /**
         * 15.2.3.3 Object.getOwnPropertyDescriptor ( O, P )
         */
        @Function(name = "getOwnPropertyDescriptor", arity = 2)
        public static Object getOwnPropertyDescriptor(Realm realm, Object thisValue, Object o,
                Object p) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            Object key = ToPropertyKey(realm, p);
            Property desc;
            if (key instanceof String) {
                desc = Type.objectValue(o).getOwnProperty((String) key);
            } else {
                desc = Type.objectValue(o).getOwnProperty((Symbol) key);
            }
            return FromPropertyDescriptor(realm, desc);
        }

        /**
         * 15.2.3.4 Object.getOwnPropertyNames ( O )
         */
        @Function(name = "getOwnPropertyNames", arity = 1)
        public static Object getOwnPropertyNames(Realm realm, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            List<String> nameList = GetOwnPropertyNames(realm, Type.objectValue(o));
            return CreateArrayFromList(realm, nameList);
        }

        /**
         * 15.2.3.5 Object.create ( O [, Properties] )
         */
        @Function(name = "create", arity = 2)
        public static Object create(Realm realm, Object thisValue, Object o, Object properties) {
            if (!(Type.isObject(o) || Type.isNull(o))) {
                throw throwTypeError(realm, "");
            }
            Scriptable proto = Type.isObject(o) ? Type.objectValue(o) : null;
            Scriptable obj = ObjectCreate(realm, proto);
            if (!Type.isUndefined(properties)) {
                return ObjectDefineProperties(realm, obj, properties);
            }
            return obj;
        }

        /**
         * 15.2.3.6 Object.defineProperty ( O, P, Attributes )
         */
        @Function(name = "defineProperty", arity = 3)
        public static Object defineProperty(Realm realm, Object thisValue, Object o, Object p,
                Object attributes) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            Object key = ToPropertyKey(realm, p);
            PropertyDescriptor desc = ToPropertyDescriptor(realm, attributes);
            if (key instanceof String) {
                DefinePropertyOrThrow(realm, Type.objectValue(o), (String) key, desc);
            } else {
                DefinePropertyOrThrow(realm, Type.objectValue(o), (Symbol) key, desc);
            }
            return o;
        }

        /**
         * 15.2.3.7 Object.defineProperties ( O, Properties )
         */
        @Function(name = "defineProperties", arity = 2)
        public static Object defineProperties(Realm realm, Object thisValue, Object o,
                Object properties) {
            return ObjectDefineProperties(realm, o, properties);
        }

        /**
         * 15.2.3.8 Object.seal ( O )
         */
        @Function(name = "seal", arity = 1)
        public static Object seal(Realm realm, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            // FIXME: spec bug (bug 1056)
            MakeObjectSecure(realm, Type.objectValue(o), false);
            return o;
        }

        /**
         * 15.2.3.9 Object.freeze ( O )
         */
        @Function(name = "freeze", arity = 1)
        public static Object freeze(Realm realm, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            // FIXME: spec bug (bug 1056)
            MakeObjectSecure(realm, Type.objectValue(o), true);
            return o;
        }

        /**
         * 15.2.3.10 Object.preventExtensions ( O )
         */
        @Function(name = "preventExtensions", arity = 1)
        public static Object preventExtensions(Realm realm, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            Type.objectValue(o).preventExtensions();
            return o;
        }

        /**
         * 15.2.3.11 Object.isSealed ( O )
         */
        @Function(name = "isSealed", arity = 1)
        public static Object isSealed(Realm realm, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            return TestIfSecureObject(realm, Type.objectValue(o), false);
        }

        /**
         * 15.2.3.12 Object.isFrozen ( O )
         */
        @Function(name = "isFrozen", arity = 1)
        public static Object isFrozen(Realm realm, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            return TestIfSecureObject(realm, Type.objectValue(o), true);
        }

        /**
         * 15.2.3.13 Object.isExtensible ( O )
         */
        @Function(name = "isExtensible", arity = 1)
        public static Object isExtensible(Realm realm, Object thisValue, Object o) {
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            return Type.objectValue(o).isExtensible();
        }

        /**
         * 15.2.3.14 Object.keys ( O )
         */
        @Function(name = "keys", arity = 1)
        public static Object keys(Realm realm, Object thisValue, Object o) {
            // FIXME: spec bug - steps start at 8
            if (!Type.isObject(o)) {
                throw throwTypeError(realm, "");
            }
            List<String> nameList = GetOwnPropertyKeys(realm, Type.objectValue(o));
            return CreateArrayFromList(realm, nameList);
        }
    }

    /**
     * 15.2.3.7 Object.defineProperties ( O, Properties )
     * <p>
     * Runtime Semantics: ObjectDefineProperties Abstract Operation
     */
    public static Scriptable ObjectDefineProperties(Realm realm, Object o, Object properties) {
        if (!Type.isObject(o)) {
            throw throwTypeError(realm, "");
        }
        Scriptable obj = Type.objectValue(o);
        Scriptable props = ToObject(realm, properties);
        // FIXME: spec bug ('keys of each enumerable own property' -> string/symbol/private ?)
        List<String> names = GetOwnPropertyKeys(realm, props);
        List<PropertyDescriptor> descriptors = new ArrayList<>();
        for (String p : names) {
            Object descObj = Get(props, p);
            PropertyDescriptor desc = ToPropertyDescriptor(realm, descObj);
            descriptors.add(desc);
        }
        ScriptException pendingException = null;
        for (int i = 0, size = names.size(); i < size; ++i) {
            String p = names.get(i);
            PropertyDescriptor desc = descriptors.get(i);
            try {
                DefinePropertyOrThrow(realm, obj, p, desc);
            } catch (ScriptException e) {
                if (pendingException == null) {
                    pendingException = e;
                }
            }
        }
        if (pendingException != null) {
            throw pendingException;
        }
        return obj;
    }
}
