/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.2 DataView Objects</h2>
 * <ul>
 * <li>24.2.1 Abstract Operations For DataView Objects
 * <li>24.2.2 The DataView Constructor
 * <li>24.2.3 Properties of the DataView Constructor
 * </ul>
 */
public class DataViewConstructor extends BuiltinConstructor implements Initialisable {
    public DataViewConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    private static class DataViewObjectAllocator implements ObjectAllocator<DataViewObject> {
        static final ObjectAllocator<DataViewObject> INSTANCE = new DataViewObjectAllocator();

        @Override
        public DataViewObject newInstance(Realm realm) {
            return new DataViewObject(realm);
        }
    }

    /**
     * 24.2.1 Abstract Operations For DataView Objects <br>
     * 24.2.1.1 GetViewValue(view, requestIndex, isLittleEndian, type)
     */
    public static double GetViewValue(ExecutionContext cx, Object view, Object requestIndex,
            Object isLittleEndian, ElementType type) {
        /* steps 1-2 */
        if (!(view instanceof DataViewObject)) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        DataViewObject dataView = (DataViewObject) view;
        /* step 3 */
        ArrayBufferObject buffer = dataView.getBuffer();
        /* step 4 */
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* step 5 */
        double numberIndex = ToNumber(cx, requestIndex);
        /* steps 6-7 */
        double getIndex = ToInteger(numberIndex);
        /* step 8 */
        if (numberIndex != getIndex || getIndex < 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* steps 9-10 */
        boolean littleEndian = ToBoolean(isLittleEndian);
        /* step 11 */
        long viewOffset = dataView.getByteOffset();
        /* step 12 */
        long viewSize = dataView.getByteLength();
        /* step 13 */
        int elementSize = type.size();
        /* step 14 */
        if (getIndex + elementSize > viewSize) {
            throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 15 */
        long bufferIndex = (long) getIndex + viewOffset;
        /* step 16 */
        return GetValueFromBuffer(cx, buffer, bufferIndex, type, littleEndian);
    }

    /**
     * 24.2.1 Abstract Operations For DataView Objects <br>
     * 24.2.1.2 SetViewValue(view, requestIndex, isLittleEndian, type, value)
     */
    public static void SetViewValue(ExecutionContext cx, Object view, Object requestIndex,
            Object isLittleEndian, ElementType type, Object value) {
        /* steps 1-2 */
        if (!(view instanceof DataViewObject)) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        DataViewObject dataView = (DataViewObject) view;
        /* step 3 */
        ArrayBufferObject buffer = dataView.getBuffer();
        /* step 4 */
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* step 5 */
        double numberIndex = ToNumber(cx, requestIndex);
        /* steps 6-7 */
        double getIndex = ToInteger(numberIndex);
        /* step 8 */
        if (numberIndex != getIndex || getIndex < 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* steps 9-10 */
        boolean littleEndian = ToBoolean(isLittleEndian);
        /* step 11 */
        long viewOffset = dataView.getByteOffset();
        /* step 12 */
        long viewSize = dataView.getByteLength();
        /* step 13 */
        int elementSize = type.size();
        /* step 14 */
        if (getIndex + elementSize > viewSize) {
            throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 15 */
        long bufferIndex = (long) getIndex + viewOffset;
        /* step 16 */
        SetValueInBuffer(cx, buffer, bufferIndex, type, ToNumber(cx, value), littleEndian);
    }

    /**
     * 24.2.2.1 DataView (buffer, byteOffset=0, byteLength=undefined)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object buffer = args.length > 0 ? args[0] : UNDEFINED;
        Object byteOffset = args.length > 1 ? args[1] : 0;
        Object byteLength = args.length > 2 ? args[2] : UNDEFINED;
        /* step 1 (implicit) */
        /* step 2 */
        if (!(thisValue instanceof DataViewObject)) {
            throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        DataViewObject dataView = (DataViewObject) thisValue;
        /* step 3 (not applicable) */
        /* step 4 */
        if (dataView.getBuffer() != null) {
            throwTypeError(calleeContext, Messages.Key.InitialisedObject);
        }
        /* steps 5-6 */
        if (!(buffer instanceof ArrayBufferObject)) {
            throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        ArrayBufferObject bufferObj = (ArrayBufferObject) buffer;
        // FIXME: spec bug - missing check for initialisation status, cf. TypedArray Constructor
        if (bufferObj.getData() == null) {
            throwTypeError(calleeContext, Messages.Key.UninitialisedObject);
        }
        /* step 7 */
        double numberOffset = ToNumber(calleeContext, byteOffset);
        /* steps 8-9 */
        double offset = ToInteger(numberOffset);
        /* step 10 */
        if (numberOffset != offset || offset < 0) {
            throwRangeError(calleeContext, Messages.Key.InvalidByteOffset);
        }
        /* step 11 */
        long bufferByteLength = bufferObj.getByteLength();
        /* step 12 */
        if (offset > bufferByteLength) {
            throwRangeError(calleeContext, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* steps 13-14 */
        long viewByteLength, viewByteOffset = (long) offset;
        if (Type.isUndefined(byteLength)) {
            viewByteLength = bufferByteLength - viewByteOffset;
        } else {
            double numberLength = ToNumber(calleeContext, byteLength);
            double viewLength = ToInteger(numberLength);
            if (numberLength != viewLength || viewLength < 0) {
                throwRangeError(calleeContext, Messages.Key.InvalidByteOffset);
            }
            viewByteLength = (long) viewLength;
            if (offset + viewByteLength > bufferByteLength) {
                throwRangeError(calleeContext, Messages.Key.ArrayOffsetOutOfRange);
            }
        }
        /* step 15 */
        if (dataView.getBuffer() != null) {
            throwTypeError(calleeContext, Messages.Key.InitialisedObject);
        }
        /* steps 16-18 */
        dataView.setBuffer(bufferObj);
        dataView.setByteLength(viewByteLength);
        dataView.setByteOffset(viewByteOffset);
        /* steps 19 */
        return dataView;
    }

    /**
     * 24.2.2.2 new DataView( ... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 24.2.3 Properties of the DataView Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "DataView";

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        /**
         * 24.2.3.1 DataView.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.DataViewPrototype;

        /**
         * 24.2.3.2 DataView [ @@create ] ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.DataViewPrototype,
                    DataViewObjectAllocator.INSTANCE);
        }
    }
}
