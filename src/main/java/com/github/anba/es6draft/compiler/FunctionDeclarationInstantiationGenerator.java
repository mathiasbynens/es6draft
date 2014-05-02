/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.FunctionDeclarationCollector.functionDeclarations;
import static com.github.anba.es6draft.semantics.StaticSemantics.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.compiler.Code.MethodCode;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.2 ECMAScript Function Objects</h2>
 * <ul>
 * <li>9.2.13 Function Declaration Instantiation
 * </ul>
 */
final class FunctionDeclarationInstantiationGenerator extends
        DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: Arrays
        static final MethodDesc Arrays_asList = MethodDesc.create(MethodType.Static, Types.Arrays,
                "asList", Type.getMethodType(Types.List, Types.Object_));

        // class: ExecutionContext
        static final MethodDesc ExecutionContext_getVariableEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "getVariableEnvironment",
                Type.getMethodType(Types.LexicalEnvironment));

        static final MethodDesc ExecutionContext_setEnvironment = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "setEnvironment",
                Type.getMethodType(Type.VOID_TYPE, Types.LexicalEnvironment));

        // class: ExoticArguments
        static final MethodDesc ExoticArguments_CreateMappedArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ExoticArguments, "CreateMappedArgumentsObject", Type
                        .getMethodType(Types.ExoticArguments, Types.ExecutionContext,
                                Types.FunctionObject, Types.String_, Types.Object_,
                                Types.LexicalEnvironment));

        static final MethodDesc ExoticArguments_CreateUnmappedArgumentsObject = MethodDesc.create(
                MethodType.Static, Types.ExoticArguments, "CreateUnmappedArgumentsObject",
                Type.getMethodType(Types.ExoticArguments, Types.ExecutionContext, Types.Object_));

        static final MethodDesc ExoticLegacyArguments_CreateLegacyArgumentsObject = MethodDesc
                .create(MethodType.Static, Types.ExoticLegacyArguments,
                        "CreateLegacyArgumentsObject", Type.getMethodType(
                                Types.ExoticLegacyArguments, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.String_,
                                Types.LexicalEnvironment));

        static final MethodDesc ExoticLegacyArguments_CreateLegacyArgumentsObjectFrom = MethodDesc
                .create(MethodType.Static, Types.ExoticLegacyArguments,
                        "CreateLegacyArgumentsObject", Type.getMethodType(
                                Types.ExoticLegacyArguments, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_, Types.ExoticArguments));

        static final MethodDesc ExoticLegacyArguments_CreateLegacyArgumentsObjectUnmapped = MethodDesc
                .create(MethodType.Static, Types.ExoticLegacyArguments,
                        "CreateLegacyArgumentsObject", Type.getMethodType(
                                Types.ExoticLegacyArguments, Types.ExecutionContext,
                                Types.FunctionObject, Types.Object_));

        // FunctionObject
        static final MethodDesc FunctionObject_setLegacyArguments = MethodDesc.create(
                MethodType.Virtual, Types.FunctionObject, "setLegacyArguments",
                Type.getMethodType(Type.VOID_TYPE, Types.ExoticLegacyArguments));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_newDeclarativeEnvironment = MethodDesc.create(
                MethodType.Static, Types.LexicalEnvironment, "newDeclarativeEnvironment",
                Type.getMethodType(Types.LexicalEnvironment, Types.LexicalEnvironment));

        // class: List
        static final MethodDesc List_iterator = MethodDesc.create(MethodType.Interface, Types.List,
                "iterator", Type.getMethodType(Types.Iterator));

        // class: Reference
        static final MethodDesc Reference_putValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "putValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));
    }

    private static final int EXECUTION_CONTEXT = 0;
    private static final int FUNCTION = 1;
    private static final int ARGUMENTS = 2;
    private static final IdentifierResolution identifierResolution = new IdentifierResolution();

    private static final class FunctionDeclInitMethodGenerator extends ExpressionVisitor {
        FunctionDeclInitMethodGenerator(MethodCode method, FunctionNode node) {
            super(method, IsStrict(node), false, false);
        }

        @Override
        public void begin() {
            super.begin();
            setParameterName("cx", EXECUTION_CONTEXT, Types.ExecutionContext);
            setParameterName("function", FUNCTION, Types.FunctionObject);
            setParameterName("arguments", ARGUMENTS, Types.Object_);
        }
    }

    FunctionDeclarationInstantiationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    void generate(FunctionNode function) {
        MethodCode method = codegen.newMethod(function, FunctionName.Init);
        ExpressionVisitor mv = new FunctionDeclInitMethodGenerator(method, function);

        mv.lineInfo(function);
        mv.begin();
        mv.enterScope(function);
        generate(function, mv);
        mv.exitScope();
        mv.end();
    }

    private void generate(FunctionNode function, ExpressionVisitor mv) {
        Variable<ExecutionContext> context = mv.getParameter(EXECUTION_CONTEXT,
                ExecutionContext.class);

        Variable<LexicalEnvironment<?>> env = mv.newVariable("env", LexicalEnvironment.class)
                .uncheckedCast();
        mv.loadExecutionContext();
        mv.invoke(Methods.ExecutionContext_getVariableEnvironment);
        mv.store(env);

        Variable<EnvironmentRecord> envRec = mv.newVariable("envRec", EnvironmentRecord.class);
        getEnvironmentRecord(env, mv);
        mv.store(envRec);

        Variable<Undefined> undef = mv.newVariable("undef", Undefined.class);
        mv.loadUndefined();
        mv.store(undef);

        boolean hasParameters = !function.getParameters().getFormals().isEmpty();
        Variable<Iterator<?>> iterator = null;
        if (hasParameters) {
            iterator = mv.newVariable("iterator", Iterator.class).uncheckedCast();
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.invoke(Methods.Arrays_asList);
            mv.invoke(Methods.List_iterator);
            mv.store(iterator);
        }

        /* step 1 */
        // RuntimeInfo.Function code = func.getCode();
        /* step 2 */
        boolean strict = IsStrict(function);
        boolean legacy = !strict && codegen.isEnabled(CompatibilityOption.FunctionPrototype);
        boolean block = !strict && codegen.isEnabled(CompatibilityOption.BlockFunctionDeclaration)
                && (function instanceof FunctionDefinition); // Arrow or generator functions?
        /* step 3 */
        FormalParameterList formals = function.getParameters();
        /* step 4 */
        List<String> parameterNames = BoundNames(formals);
        HashSet<String> parameterNamesSet = new HashSet<>(parameterNames);
        /* step 5 */
        boolean hasDuplicates = parameterNames.size() != parameterNamesSet.size();
        /* step 6 */
        boolean needsParameterEnvironment = ContainsExpression(formals);
        /* step 7 */
        boolean simpleParameterList = IsSimpleParameterList(formals);
        // invariant: hasDuplicates => simpleParameterList
        assert !hasDuplicates || simpleParameterList;
        // invariant: needsParameterEnvironment => !simpleParameterList
        assert !needsParameterEnvironment || !simpleParameterList;
        /* step 8 */
        Set<String> varNames = VarDeclaredNames(function); // unordered set!
        /* step 9 */
        List<StatementListItem> varDeclarations = VarScopedDeclarations(function);
        /* step 10 */
        Set<String> lexicalNames = LexicallyDeclaredNames(function); // unordered set!
        /* step 11 */
        HashSet<String> functionNames = new HashSet<>();
        /* step 12 */
        List<Declaration> functionsToInitialize = new ArrayList<>();
        /* step 13 */
        for (StatementListItem item : reverse(varDeclarations)) {
            if (!(item instanceof VariableStatement)) {
                assert isFunctionDeclaration(item);
                Declaration d = (Declaration) item;
                String fn = BoundName(d);
                if (!functionNames.contains(fn)) {
                    functionNames.add(fn);
                    functionsToInitialize.add(d);
                }
            }
        }
        /* step 14 */
        boolean needsSpecialArgumentsBinding = true;
        /* step 15 */
        boolean argumentsObjectNeeded = true;
        /* step 16 */
        if (function instanceof ArrowFunction || function instanceof GeneratorComprehension) {
            // => [[ThisMode]] of func is lexical
            needsSpecialArgumentsBinding = false;
            argumentsObjectNeeded = false;
        }
        /* step 17 */
        else if (parameterNamesSet.contains("arguments")) {
            // FIXME: spec bug - `(function(arguments){})()` broken (Bug 2642)
            needsSpecialArgumentsBinding = false;
            argumentsObjectNeeded = false;
        }
        /* step 18 */
        else {
            if (functionNames.contains("arguments")) {
                argumentsObjectNeeded = false;
            } else if (lexicalNames.contains("arguments")) {
                argumentsObjectNeeded = false;
            }
        }
        /* steps 19-20 */
        Variable<?> argumentsValue;
        if (!argumentsObjectNeeded) {
            if (legacy) {
                if (!simpleParameterList) {
                    CreateLegacyArguments(mv);
                } else {
                    CreateLegacyArguments(env, formals, mv);
                }
            }
            argumentsValue = undef;
        } else {
            Variable<ExoticArguments> argumentsObj = mv.newVariable("argumentsObj",
                    ExoticArguments.class);
            if (strict || !simpleParameterList) {
                CreateUnmappedArgumentsObject(mv);
            } else {
                CreateMappedArgumentsObject(env, formals, mv);
            }
            mv.store(argumentsObj);
            if (legacy) {
                CreateLegacyArguments(argumentsObj, mv);
            }
            argumentsValue = argumentsObj;
        }
        /* step 21 */
        HashSet<String> bindings = new HashSet<>();
        for (String paramName : parameterNames) {
            boolean alreadyDeclared = bindings.contains(paramName);
            if (!alreadyDeclared) {
                bindings.add(paramName);
                createMutableBinding(envRec, paramName, false, mv);
                if (hasDuplicates) {
                    initializeBinding(envRec, paramName, undef, mv);
                }
            }
        }
        /* step 22 */
        HashSet<String> instantiatedVarNames = new HashSet<>(parameterNames);
        /* step 23 */
        if (needsSpecialArgumentsBinding) {
            if (strict) {
                createImmutableBinding(envRec, "arguments", mv);
            } else {
                createMutableBinding(envRec, "arguments", false, mv);
            }
            if (argumentsObjectNeeded) {
                initializeBinding(envRec, "arguments", argumentsValue, mv);
                instantiatedVarNames.add("arguments");
            }
            // FIXME: spec bug - "arguments" as function not handled
            else if (!needsParameterEnvironment) {
                assert argumentsValue == undef;
                initializeBinding(envRec, "arguments", argumentsValue, mv);
                instantiatedVarNames.add("arguments");
            } else {
                assert argumentsValue == undef;
                initializeBinding(envRec, "arguments", argumentsValue, mv);
            }
        }
        /* steps 24-26 */
        if (hasParameters) {
            if (hasDuplicates) {
                /* step 24 */
                BindingInitialization(function, iterator, mv);
            } else {
                /* step 25 */
                BindingInitializationWithEnv(envRec, function, iterator, mv);
            }
        }
        /* step 27 */
        Variable<LexicalEnvironment<?>> localEnv;
        Variable<EnvironmentRecord> localEnvRec;
        if (needsParameterEnvironment) {
            localEnv = mv.newVariable("localEnv", LexicalEnvironment.class).uncheckedCast();
            newDeclarativeEnvironment(env, mv);
            mv.store(localEnv);

            localEnvRec = mv.newVariable("localEnvRec", EnvironmentRecord.class);
            getEnvironmentRecord(localEnv, mv);
            mv.store(localEnvRec);

            mv.loadExecutionContext();
            mv.load(localEnv);
            mv.invoke(Methods.ExecutionContext_setEnvironment);
        } else {
            localEnv = env;
            localEnvRec = envRec;
        }
        /* step 28 */
        for (String varName : varNames) {
            // FIXME: spec bug - "arguments" not handled (Bug 2644)
            // FIXME: spec bug - duplicate varNames (Bug 2645)
            if (!instantiatedVarNames.contains(varName)) {
                instantiatedVarNames.add(varName);
                createMutableBinding(localEnvRec, varName, false, mv);
                initializeBinding(localEnvRec, varName, undef, mv);
            }
        }

        /* B.3.2  Web Legacy Compatibility for Block-Level Function Declarations */
        BLOCK: if (block) {
            assert function instanceof FunctionDefinition;
            // Find all function declarations
            List<FunctionDeclaration> functions = functionDeclarations((FunctionDefinition) function);
            if (functions.isEmpty()) {
                break BLOCK;
            }

            // parameters, var-names and implicit "arguments"
            HashSet<String> envBindings = new HashSet<>();
            if (needsParameterEnvironment) {
                // only bindings from step 28
                envBindings.addAll(varNames);
                if (needsSpecialArgumentsBinding) {
                    envBindings.remove("arguments");
                }
                envBindings.removeAll(parameterNames);
            } else {
                // bindings from step 21, 23, 28
                envBindings.addAll(parameterNames);
                envBindings.addAll(varNames);
                if (needsSpecialArgumentsBinding) {
                    envBindings.add("arguments");
                }
                assert instantiatedVarNames.size() == envBindings.size();
            }

            for (FunctionDeclaration f : functions) {
                String fname = f.getIdentifier().getName();
                if (lexicalNames.contains(fname)) {
                    continue;
                }
                if (!simpleParameterList && parameterNamesSet.contains(fname)) {
                    continue;
                }
                // TODO: spec clear enough to omit function declarations?
                // TODO: may change when function binding is properly specified!
                // TODO: add test case!
                if (functionNames.contains(fname)) {
                    continue;
                }
                boolean alreadyDeclared = envBindings.contains(fname);
                if (!alreadyDeclared) {
                    envBindings.add(fname);
                    createMutableBinding(localEnvRec, fname, false, mv);
                    f.setLegacyBlockScoped(true);
                }
            }
        }

        /* step 29 */
        List<Declaration> lexDeclarations = LexicalDeclarations(function);
        /* step 30 */
        for (Declaration d : lexDeclarations) {
            assert !isFunctionDeclaration(d);
            for (String dn : BoundNames(d)) {
                if (d.isConstDeclaration()) {
                    createImmutableBinding(localEnvRec, dn, mv);
                } else {
                    createMutableBinding(localEnvRec, dn, false, mv);
                }
            }
        }
        // FIXME: spec bug - missing function bindings (Bug 2643)
        /* step 31 */
        for (Declaration f : functionsToInitialize) {
            // String fn = BoundName(f);
            BindingIdentifier fn = getFunctionName(f);

            // stack: [] -> [ref]
            ResolveBinding(fn, mv);

            // stack: [ref] -> [ref, fo]
            InstantiateFunctionObject(context, localEnv, f, mv);

            // stack: [ref, fo] -> []
            PutValue(mv);
        }
        /* step 32 */
        mv.areturn();
    }

    private void newDeclarativeEnvironment(Variable<LexicalEnvironment<?>> env, ExpressionVisitor mv) {
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_newDeclarativeEnvironment);
    }

    private void BindingInitialization(FunctionNode node, Variable<Iterator<?>> iterator,
            ExpressionVisitor mv) {
        // stack: [] -> []
        new BindingInitializationGenerator(codegen).generate(node, iterator, mv);
    }

    private void BindingInitializationWithEnv(Variable<? extends EnvironmentRecord> envRec,
            FunctionNode node, Variable<Iterator<?>> iterator, ExpressionVisitor mv) {
        // stack: [] -> []
        new BindingInitializationGenerator(codegen).generateWithEnvironment(node, envRec, iterator,
                mv);
    }

    private void CreateMappedArgumentsObject(Variable<LexicalEnvironment<?>> env,
            FormalParameterList formals, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.loadParameter(FUNCTION, FunctionObject.class);
        newStringArray(mv, mappedNames(formals));
        mv.loadParameter(ARGUMENTS, Object[].class);
        mv.load(env);
        mv.invoke(Methods.ExoticArguments_CreateMappedArgumentsObject);
    }

    private void CreateUnmappedArgumentsObject(ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.loadParameter(ARGUMENTS, Object[].class);
        mv.invoke(Methods.ExoticArguments_CreateUnmappedArgumentsObject);
    }

    private void CreateLegacyArguments(ExpressionVisitor mv) {
        // function.setLegacyArguments(<legacy-arguments>)
        mv.loadParameter(FUNCTION, FunctionObject.class);
        {
            // CreateLegacyArgumentsObject(cx, function, arguments)
            mv.loadExecutionContext();
            mv.loadParameter(FUNCTION, FunctionObject.class);
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.invoke(Methods.ExoticLegacyArguments_CreateLegacyArgumentsObjectUnmapped);
        }
        mv.invoke(Methods.FunctionObject_setLegacyArguments);
    }

    private void CreateLegacyArguments(Variable<LexicalEnvironment<?>> env,
            FormalParameterList formals, ExpressionVisitor mv) {
        // function.setLegacyArguments(<legacy-arguments>)
        mv.loadParameter(FUNCTION, FunctionObject.class);
        {
            // CreateLegacyArgumentsObject(cx, function, arguments, formals, scope)
            mv.loadExecutionContext();
            mv.loadParameter(FUNCTION, FunctionObject.class);
            mv.loadParameter(ARGUMENTS, Object[].class);
            newStringArray(mv, mappedNames(formals));
            mv.load(env);
            mv.invoke(Methods.ExoticLegacyArguments_CreateLegacyArgumentsObject);
        }
        mv.invoke(Methods.FunctionObject_setLegacyArguments);
    }

    private void CreateLegacyArguments(Variable<ExoticArguments> argumentsObj, ExpressionVisitor mv) {
        // function.setLegacyArguments(<legacy-arguments>)
        mv.loadParameter(FUNCTION, FunctionObject.class);
        {
            // CreateLegacyArgumentsObject(cx, function, arguments, argumentsObj)
            mv.loadExecutionContext();
            mv.loadParameter(FUNCTION, FunctionObject.class);
            mv.loadParameter(ARGUMENTS, Object[].class);
            mv.load(argumentsObj);
            mv.invoke(Methods.ExoticLegacyArguments_CreateLegacyArgumentsObjectFrom);
        }
        mv.invoke(Methods.FunctionObject_setLegacyArguments);
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

    private String[] mappedNames(FormalParameterList formals) {
        assert IsSimpleParameterList(formals);
        List<FormalParameter> list = formals.getFormals();
        int numberOfParameters = list.size();
        Set<String> mappedNames = new HashSet<>();
        String[] names = new String[numberOfParameters];
        for (int index = numberOfParameters - 1; index >= 0; --index) {
            assert list.get(index) instanceof BindingElement;
            BindingElement formal = (BindingElement) list.get(index);
            assert formal.getBinding() instanceof BindingIdentifier;
            String name = ((BindingIdentifier) formal.getBinding()).getName();
            if (!mappedNames.contains(name)) {
                mappedNames.add(name);
                names[index] = name;
            }
        }
        return names;
    }

    private void newStringArray(InstructionVisitor mv, String[] strings) {
        mv.newarray(strings.length, Types.String);
        int index = 0;
        for (String string : strings) {
            mv.astore(index++, string);
        }
    }
}
