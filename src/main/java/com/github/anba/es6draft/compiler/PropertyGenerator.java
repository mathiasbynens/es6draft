/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.PropertyDefinitionsMethod;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.assembler.MethodDesc;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;

/**
 * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation<br>
 * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation<br>
 * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
 */
final class PropertyGenerator extends
        DefaultCodeGenerator<DefaultCodeGenerator.ValType, ExpressionVisitor> {
    private static final class Methods {
        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinition = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluatePropertyDefinition", Type
                        .getMethodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                                Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinition_String = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinition", Type.getMethodType(Type.VOID_TYPE,
                                Types.OrdinaryObject, Types.String, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionAsync = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionAsync",
                Type.getMethodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionAsync_String = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinitionAsync", Type.getMethodType(Type.VOID_TYPE,
                                Types.OrdinaryObject, Types.String, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGenerator = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinitionGenerator", Type.getMethodType(Type.VOID_TYPE,
                                Types.OrdinaryObject, Types.Object, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGenerator_String = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinitionGenerator", Type.getMethodType(Type.VOID_TYPE,
                                Types.OrdinaryObject, Types.String, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGetter = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionGetter",
                Type.getMethodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionGetter_String = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinitionGetter", Type.getMethodType(Type.VOID_TYPE,
                                Types.OrdinaryObject, Types.String, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionSetter = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "EvaluatePropertyDefinitionSetter",
                Type.getMethodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                        Types.RuntimeInfo$Function, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_EvaluatePropertyDefinitionSetter_String = MethodDesc
                .create(MethodDesc.Invoke.Static, Types.ScriptRuntime,
                        "EvaluatePropertyDefinitionSetter", Type.getMethodType(Type.VOID_TYPE,
                                Types.OrdinaryObject, Types.String, Types.RuntimeInfo$Function,
                                Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineMethod = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "defineMethod", Type.getMethodType(
                        Type.VOID_TYPE, Types.OrdinaryObject, Types.Object, Types.FunctionObject,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineMethod_String = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "defineMethod", Type.getMethodType(
                        Type.VOID_TYPE, Types.OrdinaryObject, Types.String, Types.FunctionObject,
                        Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProperty = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "defineProperty", Type
                        .getMethodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                                Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProperty_String = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "defineProperty", Type
                        .getMethodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.String,
                                Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_defineProtoProperty = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "defineProtoProperty", Type
                        .getMethodType(Type.VOID_TYPE, Types.OrdinaryObject, Types.Object,
                                Types.ExecutionContext));
    }

    public PropertyGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected ValType visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 12.2.5.7 Runtime Semantics: Evaluation
     * <p>
     * ComputedPropertyName : [ AssignmentExpression ]
     */
    @Override
    public ValType visit(ComputedPropertyName node, ExpressionVisitor mv) {
        /* steps 1-3 */
        ValType type = expressionValue(node.getExpression(), mv);
        /* step 4 */
        return ToPropertyKey(type, mv);
    }

    @Override
    public ValType visit(PropertyDefinitionsMethod node, ExpressionVisitor mv) {
        codegen.compile(node, mv);

        // stack: [<object>] -> [cx, <object>]
        mv.loadExecutionContext();
        mv.swap();

        // stack: [<object>] -> []
        mv.invoke(codegen.methodDesc(node));

        return null;
    }

    /**
     * 14.3.9 Runtime Semantics: PropertyDefinitionEvaluation<br>
     * 14.4.13 Runtime Semantics: PropertyDefinitionEvaluation
     */
    @Override
    public ValType visit(MethodDefinition node, ExpressionVisitor mv) {
        codegen.compile(node);

        // stack: [<object>]

        String propName = PropName(node);
        if (propName == null) {
            assert node.getPropertyName() instanceof ComputedPropertyName;
            node.getPropertyName().accept(this, mv);

            mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
            mv.loadExecutionContext();

            switch (node.getType()) {
            case AsyncFunction:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionAsync);
                break;
            case Function:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinition);
                break;
            case Generator:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionGenerator);
                break;
            case Getter:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionGetter);
                break;
            case Setter:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionSetter);
                break;
            default:
                throw new AssertionError("invalid method type");
            }
        } else {
            mv.aconst(propName);
            mv.invoke(codegen.methodDesc(node, FunctionName.RTI));
            mv.loadExecutionContext();

            switch (node.getType()) {
            case AsyncFunction:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionAsync_String);
                break;
            case Function:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinition_String);
                break;
            case Generator:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionGenerator_String);
                break;
            case Getter:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionGetter_String);
                break;
            case Setter:
                mv.invoke(Methods.ScriptRuntime_EvaluatePropertyDefinitionSetter_String);
                break;
            default:
                throw new AssertionError("invalid method type");
            }
        }

        return null;
    }

    /**
     * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <p>
     * PropertyDefinition : IdentifierReference
     */
    @Override
    public ValType visit(PropertyNameDefinition node, ExpressionVisitor mv) {
        IdentifierReference propertyName = node.getPropertyName();
        String propName = PropName(propertyName);
        assert propName != null;

        mv.aconst(propName);
        expressionBoxedValue(propertyName, mv);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_defineProperty_String);

        return null;
    }

    /**
     * 12.2.5.8 Runtime Semantics: PropertyDefinitionEvaluation
     * <p>
     * PropertyDefinition : PropertyName : AssignmentExpression
     */
    @Override
    public ValType visit(PropertyValueDefinition node, ExpressionVisitor mv) {
        // stack: [<object>]

        PropertyName propertyName = node.getPropertyName();
        Expression propertyValue = node.getPropertyValue();

        boolean defineMethod, isAnonymousFunctionDefinition;
        if (IsFunctionDefinition(propertyValue)) {
            if (propertyValue instanceof ClassExpression) {
                // [[HomeObject]] is never undefined if [[NeedsSuper]] is true in class constructor.
                defineMethod = false;
            } else {
                assert propertyValue instanceof FunctionNode : propertyValue.getClass();
                FunctionNode function = (FunctionNode) propertyValue;
                if (function.getThisMode() == FunctionNode.ThisMode.Lexical) {
                    defineMethod = false;
                } else {
                    assert function instanceof FunctionExpression
                            || function instanceof GeneratorExpression
                            || function instanceof AsyncFunctionExpression;
                    defineMethod = function.getScope().hasSuperReference();
                }
            }
            isAnonymousFunctionDefinition = IsAnonymousFunctionDefinition(propertyValue);
        } else {
            defineMethod = false;
            isAnonymousFunctionDefinition = false;
        }

        String propName = PropName(propertyName);
        if (propName == null) {
            assert propertyName instanceof ComputedPropertyName;
            ValType type = propertyName.accept(this, mv);

            // stack: [<object>, pk]
            expressionBoxedValue(propertyValue, mv);
            // stack: [<object>, pk, value]
            if (isAnonymousFunctionDefinition) {
                SetFunctionName(propertyValue, type, mv);
            }
            mv.loadExecutionContext();
            if (defineMethod) {
                mv.invoke(Methods.ScriptRuntime_defineMethod);
            } else {
                mv.invoke(Methods.ScriptRuntime_defineProperty);
            }
        } else if ("__proto__".equals(propName)
                && codegen.isEnabled(CompatibilityOption.ProtoInitializer)) {
            expressionBoxedValue(propertyValue, mv);
            // TODO: SetFunctionName() ?
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_defineProtoProperty);
        } else {
            mv.aconst(propName);
            expressionBoxedValue(propertyValue, mv);
            if (isAnonymousFunctionDefinition) {
                SetFunctionName(propertyValue, propName, mv);
            }
            mv.loadExecutionContext();
            if (defineMethod) {
                mv.invoke(Methods.ScriptRuntime_defineMethod_String);
            } else {
                mv.invoke(Methods.ScriptRuntime_defineProperty_String);
            }
        }

        return null;
    }
}
