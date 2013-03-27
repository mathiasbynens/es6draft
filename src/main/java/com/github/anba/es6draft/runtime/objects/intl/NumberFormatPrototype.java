/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.text.NumberFormat;
import java.util.Locale;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>11 NumberFormat Objects</h1>
 * <ul>
 * <li>11.3 Properties of the Intl.NumberFormat Prototype Object
 * </ul>
 */
public class NumberFormatPrototype extends NumberFormatObject implements Initialisable {
    public NumberFormatPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 11.3 Properties of the Intl.NumberFormat Prototype Object
     */
    public enum Properties {
        ;

        private static NumberFormatObject numberFormat(Realm realm, Object object) {
            if (object instanceof NumberFormatObject) {
                // TODO: test for initialised state
                return (NumberFormatObject) object;
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 11.3.1 Intl.NumberFormat.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Intl_NumberFormat;

        /**
         * 11.3.2 Intl.NumberFormat.prototype.format
         */
        @Accessor(name = "format", type = Accessor.Type.Getter)
        public static Object format(Realm realm, Object thisValue) {
            NumberFormatObject numberFormat = numberFormat(realm, thisValue);
            if (numberFormat.getBoundFormat() == null) {
                FormatFunction f = new FormatFunction(realm);
                Callable bf = (Callable) FunctionPrototype.Properties.bind(realm, f, thisValue);
                numberFormat.setBoundFormat(bf);
            }
            return numberFormat.getBoundFormat();
        }

        /**
         * 11.3.3 Intl.NumberFormat.prototype.resolvedOptions ()
         */
        @Function(name = "resolvedOptions", arity = 0)
        public static Object resolvedOptions(Realm realm, Object thisValue) {
            numberFormat(realm, thisValue);
            return UNDEFINED;
        }
    }

    private static boolean isFinite(double x) {
        return !(Double.isInfinite(x) || Double.isNaN(x));
    }

    private static Locale getLocale(NumberFormatObject numberFormat) {
        return Locale.forLanguageTag(numberFormat.getLocale());
    }

    /**
     * Abstract Operation: FormatNumber
     */
    public static String FormatNumber(Realm realm, NumberFormatObject numberFormat, double x) {
        Locale locale = getLocale(numberFormat);
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        String n;
        boolean negative = false;
        if (!isFinite(x)) {
            if (Double.isNaN(x)) {
                n = nf.format(x);
            } else {
                n = nf.format(Double.POSITIVE_INFINITY);
                negative = x < 0;
            }
        } else {
            if (x < 0) {
                negative = true;
                x = +x;
            }
        }
        return "";
    }

    private static class FormatFunction extends BuiltinFunction {
        public FormatFunction(Realm realm) {
            super(realm);
            setPrototype(realm, realm.getIntrinsic(Intrinsics.FunctionPrototype));
            defineOwnProperty(realm, "name", new PropertyDescriptor("format", false, false, false));
            defineOwnProperty(realm, "length", new PropertyDescriptor(1, false, false, false));
            AddRestrictedFunctionProperties(realm, this);
        }

        /**
         * [[Call]]
         */
        @Override
        public Object call(Object thisValue, Object... args) {
            assert thisValue instanceof NumberFormatObject;
            Object value = args.length > 0 ? args[0] : UNDEFINED;
            double x = ToNumber(realm(), value);
            return FormatNumber(realm(), (NumberFormatObject) thisValue, x);
        }
    }
}
