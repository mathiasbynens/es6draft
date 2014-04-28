/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.SetFunctionName;
import static com.github.anba.es6draft.compiler.DefaultCodeGenerator.ToPropertyKey;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.PropName;

import java.util.Iterator;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>Runtime Semantics: BindingInitialization</h1>
 * <ul>
 * <li>12.1.2 Runtime Semantics: BindingInitialization
 * <li>12.2.4.2.2 Runtime Semantics: BindingInitialization
 * <li>13.2.2.2 Runtime Semantics: BindingInitialization
 * <li>13.2.3.5 Runtime Semantics: BindingInitialization
 * <li>13.14.3 Runtime Semantics: BindingInitialization
 * </ul>
 * 
 * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
 * <ul>
 * <li>13.2.3.6 Runtime Semantics: IteratorBindingInitialization
 * <li>14.1.20 Runtime Semantics: IteratorBindingInitialization
 * <li>14.2.16 Runtime Semantics: IteratorBindingInitialization
 * <li>
 * </ul>
 * 
 * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
 * <ul>
 * <li>13.2.3.7 Runtime Semantics: KeyedBindingInitialization
 * </ul>
 */
final class BindingInitializationGenerator {
    private static final class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_Get = MethodDesc.create(MethodType.Static,
                Types.AbstractOperations, "Get", Type.getMethodType(Types.Object,
                        Types.ExecutionContext, Types.ScriptObject, Types.Object));

        static final MethodDesc AbstractOperations_Get_String = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "Get", Type.getMethodType(
                        Types.Object, Types.ExecutionContext, Types.ScriptObject, Types.String));

        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_initializeBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "initializeBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: Reference
        static final MethodDesc Reference_putValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "putValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_createRestArray = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "createRestArray",
                Type.getMethodType(Types.ScriptObject, Types.Iterator, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_ensureObject = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "ensureObject",
                Type.getMethodType(Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getIterator = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "getIterator",
                Type.getMethodType(Types.Iterator, Types.ScriptObject, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_iteratorNextAndIgnore = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "iteratorNextAndIgnore",
                Type.getMethodType(Type.VOID_TYPE, Types.Iterator));

        static final MethodDesc ScriptRuntime_iteratorNextOrUndefined = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "iteratorNextOrUndefined",
                Type.getMethodType(Types.Object, Types.Iterator));

        // class: Type
        static final MethodDesc Type_isUndefined = MethodDesc.create(MethodType.Static,
                Types._Type, "isUndefined", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    private static final IdentifierResolution identifierResolution = new IdentifierResolution();
    private final CodeGenerator codegen;

    BindingInitializationGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param node
     *            the function node
     * @param iterator
     *            the arguments iterator
     * @param mv
     *            the expression visitor
     */
    void generate(FunctionNode node, Variable<Iterator<?>> iterator, ExpressionVisitor mv) {
        IteratorBindingInitialization init = new IteratorBindingInitialization(codegen, mv,
                EnvironmentType.NoEnvironment, null);
        node.getParameters().accept(init, iterator);
    }

    /**
     * stack: [] {@literal ->} []
     * 
     * @param node
     *            the function node
     * @param iterator
     *            the arguments iterator
     * @param mv
     *            the expression visitor
     */
    void generateWithEnvironment(FunctionNode node, Variable<? extends EnvironmentRecord> envRec,
            Variable<Iterator<?>> iterator, ExpressionVisitor mv) {
        IteratorBindingInitialization init = new IteratorBindingInitialization(codegen, mv,
                EnvironmentType.EnvironmentFromLocal, envRec);
        node.getParameters().accept(init, iterator);
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    void generate(Binding node, ExpressionVisitor mv) {
        if (node instanceof BindingIdentifier) {
            generate((BindingIdentifier) node, mv);
        } else {
            BindingInitialization init = new BindingInitialization(codegen, mv,
                    EnvironmentType.NoEnvironment, null);
            node.accept(init, null);
        }
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    private void generate(BindingIdentifier node, ExpressionVisitor mv) {
        // stack: [value] -> []
        ResolveBinding(node, mv);
        mv.swap();
        PutValue(mv);
    }

    /**
     * stack: [envRec, value] {@literal ->} []
     * 
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    void generateWithEnvironment(Binding node, ExpressionVisitor mv) {
        if (node instanceof BindingIdentifier) {
            generateWithEnvironment((BindingIdentifier) node, mv);
        } else {
            // stack: [env, value] -> []
            BindingInitialization init = new BindingInitialization(codegen, mv,
                    EnvironmentType.EnvironmentFromStack, null);
            node.accept(init, null);
        }
    }

    /**
     * stack: [envRec, value] {@literal ->} []
     * 
     * @param node
     *            the binding node
     * @param mv
     *            the expression visitor
     */
    private void generateWithEnvironment(BindingIdentifier node, ExpressionVisitor mv) {
        // stack: [envRec, value] -> [envRec, id, value]
        mv.aconst(((BindingIdentifier) node).getName());
        mv.swap();
        // stack: [envRec, id, value] -> []
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
    }

    /**
     * stack: [] {@literal ->} [Reference]
     * 
     * @param node
     *            the binding identifier node
     * @param mv
     *            the expression visitor
     */
    private static void ResolveBinding(BindingIdentifier node, ExpressionVisitor mv) {
        identifierResolution.resolve(node, mv);
    }

    /**
     * stack: [Reference, Object] {@literal ->} []
     * 
     * @param mv
     *            the expression visitor
     */
    private static void PutValue(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_putValue);
    }

    private enum EnvironmentType {
        NoEnvironment, EnvironmentFromStack, EnvironmentFromLocal
    }

    private abstract static class RuntimeSemantics<R, V> extends DefaultNodeVisitor<R, V> {
        protected final CodeGenerator codegen;
        protected final ExpressionVisitor mv;
        protected final EnvironmentType environment;
        protected final Variable<? extends EnvironmentRecord> envRec;

        RuntimeSemantics(CodeGenerator codegen, ExpressionVisitor mv, EnvironmentType environment,
                Variable<? extends EnvironmentRecord> envRec) {
            this.codegen = codegen;
            this.mv = mv;
            this.environment = environment;
            this.envRec = envRec;
            assert (environment == EnvironmentType.EnvironmentFromLocal) == (envRec != null);
        }

        protected final void BindingInitialization(Binding node) {
            node.accept(new BindingInitialization(codegen, mv, environment, envRec), null);
        }

        protected final void IteratorBindingInitialization(ArrayBindingPattern node,
                Variable<Iterator<?>> iterator) {
            node.accept(new IteratorBindingInitialization(codegen, mv, environment, envRec),
                    iterator);
        }

        protected final void KeyedBindingInitialization(BindingProperty node,
                Variable<ScriptObject> object, String key) {
            node.accept(new LiteralKeyedBindingInitialization(codegen, mv, environment, envRec,
                    object), key);
        }

        protected final void KeyedBindingInitialization(BindingProperty node,
                Variable<ScriptObject> object, ComputedPropertyName key) {
            node.accept(new ComputedKeyedBindingInitialization(codegen, mv, environment, envRec,
                    object), key);
        }

        @Override
        protected final R visit(Node node, V value) {
            throw new IllegalStateException();
        }

        protected final ValType expressionValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionValue(node, mv);
        }

        protected final ValType expressionBoxedValue(Expression node, ExpressionVisitor mv) {
            return codegen.expressionBoxedValue(node, mv);
        }

        protected final void dupEnv() {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                mv.dup();
            }
        }

        protected final void popEnv() {
            if (environment == EnvironmentType.EnvironmentFromStack) {
                mv.pop();
            }
        }

        protected final void prepareBindingIdentifier(BindingIdentifier identifier) {
            // Load environment record and binding identifier on stack to avoid swap instructions
            if (environment == EnvironmentType.EnvironmentFromLocal) {
                // stack: [] -> [envRec, id]
                mv.load(envRec);
                mv.aconst(identifier.getName());
            } else if (environment == EnvironmentType.EnvironmentFromStack) {
                // stack: [envRec] -> [envRec, id]
                mv.aconst(identifier.getName());
            } else {
                assert environment == EnvironmentType.NoEnvironment;
            }
        }
    }

    /**
     * <h1>Runtime Semantics: BindingInitialization</h1>
     * <ul>
     * <li>12.1.2 Runtime Semantics: BindingInitialization
     * <li>12.2.4.2.2 Runtime Semantics: BindingInitialization
     * <li>13.2.2.2 Runtime Semantics: BindingInitialization
     * <li>13.2.3.5 Runtime Semantics: BindingInitialization
     * <li>13.14.3 Runtime Semantics: BindingInitialization
     * </ul>
     */
    private static final class BindingInitialization extends RuntimeSemantics<Void, Void> {
        BindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, environment, envRec);
        }

        @Override
        public Void visit(ArrayBindingPattern node, Void value) {
            // step 1: Assert: Type(value) is Object

            // step 2-3:
            // stack: [(env), value] -> [(env)]
            Variable<Iterator<?>> iterator = mv.newScratchVariable(Iterator.class).uncheckedCast();
            mv.lineInfo(node);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_getIterator);
            mv.store(iterator);

            // step 4:
            IteratorBindingInitialization(node, iterator);

            mv.freeVariable(iterator);

            // stack: [(env)] -> []
            popEnv();

            return null;
        }

        @Override
        public Void visit(ObjectBindingPattern node, Void value) {
            // step 1: Assert: Type(value) is Object

            // stack: [(env), value] -> [(env)]
            Variable<ScriptObject> object = mv.newScratchVariable(ScriptObject.class);
            mv.store(object);

            // step 2: [...]
            for (BindingProperty property : node.getProperties()) {
                if (property.getPropertyName() == null) {
                    // BindingProperty : SingleNameBinding
                    String name = BoundNames(property.getBinding()).get(0);
                    KeyedBindingInitialization(property, object, name);
                } else {
                    // BindingProperty : PropertyName : BindingElement
                    String name = PropName(property.getPropertyName());
                    if (name != null) {
                        KeyedBindingInitialization(property, object, name);
                    } else {
                        PropertyName propertyName = property.getPropertyName();
                        assert propertyName instanceof ComputedPropertyName;
                        KeyedBindingInitialization(property, object,
                                (ComputedPropertyName) propertyName);
                    }
                }
            }

            mv.freeVariable(object);

            // stack: [(env)] -> []
            popEnv();

            return null;
        }

        @Override
        public Void visit(BindingIdentifier node, Void value) {
            if (environment == EnvironmentType.EnvironmentFromLocal) {
                // stack: [envRec, id, value] -> []
                mv.invoke(Methods.EnvironmentRecord_initializeBinding);
            } else if (environment == EnvironmentType.EnvironmentFromStack) {
                // stack: [envRec, id, value] -> []
                mv.invoke(Methods.EnvironmentRecord_initializeBinding);
            } else {
                assert environment == EnvironmentType.NoEnvironment;
                // stack: [value] -> []
                ResolveBinding(node, mv);
                mv.swap();
                PutValue(mv);
            }

            return null;
        }
    }

    /**
     * <h2>Runtime Semantics: IteratorBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.6 Runtime Semantics: IteratorBindingInitialization
     * <li>14.1.20 Runtime Semantics: IteratorBindingInitialization
     * <li>14.2.16 Runtime Semantics: IteratorBindingInitialization
     * <li>
     * </ul>
     */
    private static final class IteratorBindingInitialization extends
            RuntimeSemantics<Void, Variable<Iterator<?>>> {
        IteratorBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec) {
            super(codegen, mv, environment, envRec);
        }

        @Override
        public Void visit(FormalParameterList node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> [(env)]
            for (FormalParameter formal : node) {
                formal.accept(this, iterator);
            }

            return null;
        }

        @Override
        public Void visit(ArrayBindingPattern node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> [(env)]
            for (BindingElementItem element : node.getElements()) {
                element.accept(this, iterator);
            }

            return null;
        }

        @Override
        public Void visit(BindingElision node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> [(env)]
            mv.load(iterator);
            mv.invoke(Methods.ScriptRuntime_iteratorNextAndIgnore);

            return null;
        }

        @Override
        public Void visit(BindingElement node, Variable<Iterator<?>> iterator) {
            Binding binding = node.getBinding();
            Expression initializer = node.getInitializer();

            if (binding instanceof BindingIdentifier) {
                // BindingElement : SingleNameBinding
                // SingleNameBinding : BindingIdentifier Initializer{opt}

                // stack: [(env)] -> [(env), (env)]
                dupEnv();

                // stack: [(env)] -> [(env), (env, id)]
                prepareBindingIdentifier((BindingIdentifier) binding);

                // steps 1-4
                mv.load(iterator);
                mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);

                // step 5
                // stack: [(env), (env, id), v] -> [(env), (env, id), v']
                if (initializer != null) {
                    Label undef = new Label();
                    mv.dup();
                    mv.invoke(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        expressionBoxedValue(initializer, mv);
                        if (IsAnonymousFunctionDefinition(initializer)) {
                            SetFunctionName(initializer, ((BindingIdentifier) binding).getName(),
                                    mv);
                        }
                    }
                    mv.mark(undef);
                }

                // step 6
                // stack: [(env), (env, id), v'] -> [(env)]
                BindingInitialization(binding);
            } else {
                // BindingElement : BindingPattern Initializer{opt}
                assert binding instanceof BindingPattern;

                // stack: [(env)] -> [(env), (env)]
                dupEnv();

                // steps 1-4
                mv.load(iterator);
                mv.invoke(Methods.ScriptRuntime_iteratorNextOrUndefined);

                // step 5
                // stack: [(env), (env), v] -> [(env), (env), v']
                if (initializer != null) {
                    Label undef = new Label();
                    mv.dup();
                    mv.invoke(Methods.Type_isUndefined);
                    mv.ifeq(undef);
                    {
                        mv.pop();
                        expressionBoxedValue(initializer, mv);
                    }
                    mv.mark(undef);
                }

                // step 7
                // stack: [(env), (env), v'] -> [(env), (env), v']
                mv.lineInfo(binding);
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureObject);

                // step 8
                // stack: [(env), (env), v'] -> [(env)]
                BindingInitialization(binding);
            }

            return null;
        }

        @Override
        public Void visit(BindingRestElement node, Variable<Iterator<?>> iterator) {
            // stack: [(env)] -> [(env), (env)]
            dupEnv();

            // stack: [(env)] -> [(env), (env, id)]
            prepareBindingIdentifier(node.getBindingIdentifier());

            mv.load(iterator);
            mv.loadExecutionContext();
            // stack: [(env), (env, id), iterator, cx] -> [(env), (env, id), rest]
            mv.invoke(Methods.ScriptRuntime_createRestArray);

            // stack: [(env), (env, id), rest] -> [(env)]
            BindingInitialization(node.getBindingIdentifier());

            return null;
        }
    }

    /**
     * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static abstract class KeyedBindingInitialization<PROPERTYNAME> extends
            RuntimeSemantics<Void, PROPERTYNAME> {
        private final Variable<ScriptObject> object;

        KeyedBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec,
                Variable<ScriptObject> object) {
            super(codegen, mv, environment, envRec);
            this.object = object;
        }

        abstract ValType evaluatePropertyName(PROPERTYNAME propertyName);

        @Override
        public Void visit(BindingProperty node, PROPERTYNAME value) {
            Binding binding = node.getBinding();
            Expression initializer = node.getInitializer();

            // stack: [(env)] -> [(env), (env)]
            dupEnv();

            if (binding instanceof BindingIdentifier) {
                // stack: [(env)] -> [(env), (env, id)]
                prepareBindingIdentifier((BindingIdentifier) binding);
            }

            // stack: [(env), (env)] -> [(env), (env), cx, obj, propertyName]
            mv.loadExecutionContext();
            mv.load(object);
            ValType type = evaluatePropertyName(value);

            // steps 1-2
            // stack: [(env), (env), cx, obj, propertyName] -> [(env), (env), v]
            if (type == ValType.String) {
                mv.invoke(Methods.AbstractOperations_Get_String);
            } else {
                mv.invoke(Methods.AbstractOperations_Get);
            }

            // step 3
            // stack: [(env), (env), v] -> [(env), (env), v']
            if (initializer != null) {
                Label undef = new Label();
                mv.dup();
                mv.invoke(Methods.Type_isUndefined);
                mv.ifeq(undef);
                {
                    mv.pop();
                    expressionBoxedValue(initializer, mv);
                    if (binding instanceof BindingIdentifier
                            && IsAnonymousFunctionDefinition(initializer)) {
                        SetFunctionName(initializer, ((BindingIdentifier) binding).getName(), mv);
                    }
                }
                mv.mark(undef);
            }

            if (binding instanceof BindingPattern) {
                // step 4
                // stack: [(env), (env), v'] -> [(env), (env), v']
                mv.lineInfo(binding);
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureObject);
            }

            // step 5
            // stack: [(env), (env), v'] -> [(env)]
            BindingInitialization(binding);

            return null;
        }
    }

    /**
     * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static final class LiteralKeyedBindingInitialization extends
            KeyedBindingInitialization<String> {
        LiteralKeyedBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec,
                Variable<ScriptObject> object) {
            super(codegen, mv, environment, envRec, object);
        }

        @Override
        ValType evaluatePropertyName(String propertyName) {
            mv.aconst(propertyName);
            return ValType.String;
        }
    }

    /**
     * <h2>Runtime Semantics: KeyedBindingInitialization</h2>
     * <ul>
     * <li>13.2.3.7 Runtime Semantics: KeyedBindingInitialization
     * </ul>
     */
    private static final class ComputedKeyedBindingInitialization extends
            KeyedBindingInitialization<ComputedPropertyName> {
        ComputedKeyedBindingInitialization(CodeGenerator codegen, ExpressionVisitor mv,
                EnvironmentType environment, Variable<? extends EnvironmentRecord> envRec,
                Variable<ScriptObject> object) {
            super(codegen, mv, environment, envRec, object);
        }

        @Override
        ValType evaluatePropertyName(ComputedPropertyName propertyName) {
            // Runtime Semantics: Evaluation
            // ComputedPropertyName : [ AssignmentExpression ]
            ValType propType = expressionValue(propertyName.getExpression(), mv);
            return ToPropertyKey(propType, mv);
        }
    }
}